export type ServiceKind = "JELLYFIN" | "EMBY";

/** Public, durable description of what to play. Never put a route or credential here. */
export interface CastMediaItem {
  itemId: string;
  title: string;
  subtitle?: string;
  artworkPath?: string;
  audioStreamIndex?: number;
  subtitleStreamIndex?: number;
  sourceId?: string;
  resumePositionMs: number;
}

export interface CastLoadSpec {
  version: 1;
  service: ServiceKind;
  items: CastMediaItem[];
  startIndex: number;
  profileId: string;
}

/** This object is intentionally only parsed from Cast load credentials and is never persisted. */
export interface CastCredentialEnvelope {
  version: 1;
  service: ServiceKind;
  baseUrl: string;
  alternateUrl?: string;
  userId: string;
  accessToken: string;
  deviceId: string;
}

export class CastProtocolError extends Error {}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);

const text = (value: unknown, name: string, optional = false): string | undefined => {
  if (optional && (value === undefined || value === null || value === "")) return undefined;
  if (typeof value !== "string" || value.length === 0 || value.length > 512 || /[\u0000-\u001f]/.test(value)) {
    throw new CastProtocolError(`Ugyldig ${name}`);
  }
  return value;
};

const index = (value: unknown, name: string): number => {
  if (!Number.isInteger(value) || (value as number) < 0 || (value as number) > 2_147_483_647) {
    throw new CastProtocolError(`Ugyldig ${name}`);
  }
  return value as number;
};

const service = (value: unknown): ServiceKind => {
  if (value !== "JELLYFIN" && value !== "EMBY") throw new CastProtocolError("Ukjend teneste");
  return value;
};

export function parseLoadSpec(value: unknown): CastLoadSpec {
  if (!isRecord(value) || value.version !== 1) throw new CastProtocolError("Ugyldig Cast-forespurnad");
  if (!Array.isArray(value.items) || value.items.length === 0 || value.items.length > 100) {
    throw new CastProtocolError("Mottakaren treng minst éin video");
  }
  const items = value.items.map((entry) => {
    if (!isRecord(entry)) throw new CastProtocolError("Ugyldig video");
    const resumePositionMs = index(entry.resumePositionMs ?? 0, "startposisjon");
    const audioStreamIndex = entry.audioStreamIndex === undefined ? undefined : index(entry.audioStreamIndex, "lydspor");
    const subtitleStreamIndex = entry.subtitleStreamIndex === undefined ? undefined : index(entry.subtitleStreamIndex, "tekstspor");
    return {
      itemId: text(entry.itemId, "video-id")!,
      title: text(entry.title, "tittel")!,
      subtitle: text(entry.subtitle, "undertittel", true),
      artworkPath: text(entry.artworkPath, "kunststi", true),
      audioStreamIndex,
      subtitleStreamIndex,
      sourceId: text(entry.sourceId, "kjelde", true),
      resumePositionMs,
    };
  });
  const startIndex = index(value.startIndex, "køindeks");
  if (startIndex >= items.length) throw new CastProtocolError("Ugyldig køindeks");
  return { version: 1, service: service(value.service), items, startIndex, profileId: text(value.profileId, "profil")! };
}

export function parseCredentialEnvelope(value: unknown, expectedService: ServiceKind): CastCredentialEnvelope {
  if (!isRecord(value) || value.version !== 1) throw new CastProtocolError("Manglar Cast-innlogging");
  const parsedService = service(value.service);
  if (parsedService !== expectedService) throw new CastProtocolError("Tenesta stemmer ikkje");
  const baseUrl = text(value.baseUrl, "tenaradresse")!;
  const alternateUrl = text(value.alternateUrl, "alternativ adresse", true);
  for (const route of [baseUrl, alternateUrl].filter(Boolean)) {
    const url = new URL(route!);
    if (url.protocol !== "https:" && url.protocol !== "http:") throw new CastProtocolError("Utrygg tenaradresse");
    if (url.username || url.password || url.hash || url.search) throw new CastProtocolError("Utrygg tenaradresse");
  }
  return {
    version: 1,
    service: parsedService,
    baseUrl,
    alternateUrl,
    userId: text(value.userId, "brukar")!,
    accessToken: text(value.accessToken, "tilgang")!,
    deviceId: text(value.deviceId, "eining")!,
  };
}

/** Only permits an asset or stream from the selected primary/alternative routes. */
export function routeUrl(value: string, credentials: CastCredentialEnvelope): URL {
  const candidates = [credentials.baseUrl, credentials.alternateUrl].filter((route): route is string => Boolean(route));
  const resolved = new URL(value, candidates[0]);
  const matched = candidates.some((candidate) => {
    const base = new URL(candidate);
    return base.protocol === resolved.protocol && base.host === resolved.host &&
      resolved.pathname.startsWith(base.pathname.replace(/\/$/, "") + "/");
  });
  if (!matched || resolved.username || resolved.password || resolved.hash || /%2f|%5c/i.test(resolved.pathname)) {
    throw new CastProtocolError("Mottakaren avviste ei utrygg medieadresse");
  }
  ["api_key", "apikey", "token", "access_token", "x-emby-token"].forEach((name) => resolved.searchParams.delete(name));
  return resolved;
}

export function authHeaders(credentials: CastCredentialEnvelope): Record<string, string> {
  return credentials.service === "JELLYFIN"
    ? { Authorization: `MediaBrowser Client="Spole Cast", Device="Chromecast", DeviceId="${credentials.deviceId}", Version="1.0.0", Token="${credentials.accessToken}"` }
    : {
        "X-Emby-Token": credentials.accessToken,
        "X-Emby-Authorization": `MediaBrowser Client="Spole Cast", Device="Chromecast", DeviceId="${credentials.deviceId}", Version="1.0.0"`,
      };
}

/** Used in tests and diagnostics; a public load payload must never contain a credential field. */
export function containsCredential(value: unknown): boolean {
  if (typeof value === "string") return /accessToken|api[_-]?key|authorization|x-emby-token/i.test(value);
  if (Array.isArray(value)) return value.some(containsCredential);
  if (!isRecord(value)) return false;
  return Object.entries(value).some(([key, child]) => /token|credential|authorization|api[_-]?key/i.test(key) || containsCredential(child));
}
