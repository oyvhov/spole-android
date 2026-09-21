import "./style.css";
import {
  CastProtocolError,
  authHeaders,
  parseCredentialEnvelope,
  parseLoadSpec,
  routeUrl,
  type CastCredentialEnvelope,
  type CastMediaItem,
} from "./protocol";

declare const cast: any;

const status = document.querySelector<HTMLParagraphElement>("#receiver-status")!;
const title = document.querySelector<HTMLElement>("#receiver-title")!;
const source = document.querySelector<HTMLElement>("#receiver-source")!;
const say = (message: string) => { status.textContent = message; };

type ResolvedMedia = { contentId: string; contentType: string; sourceId: string; sessionId: string };
let credentials: CastCredentialEnvelope | undefined;
let lastProgressMs = -30_000;
let active: { item: CastMediaItem; media: ResolvedMedia } | undefined;
let seasonQueue: { spec: ReturnType<typeof parseLoadSpec>; prepared: Map<number, ResolvedMedia> } | undefined;

function clearCredentials() {
  credentials = undefined;
  active = undefined;
  seasonQueue = undefined;
  lastProgressMs = -30_000;
}

function serverHeaders() {
  if (!credentials) throw new CastProtocolError("Cast-innlogginga er utgått");
  return authHeaders(credentials);
}

/** A leading slash would otherwise throw away a Jellyfin reverse-proxy base path. */
function serverEndpoint(path: string): URL {
  if (!credentials) throw new CastProtocolError("Cast-innlogginga er utgått");
  return routeUrl(`${credentials.baseUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}`, credentials);
}

function deviceProfile() {
  return {
    MaxStreamingBitrate: 20_000_000,
    DirectPlayProfiles: [
      { Type: "Video", Container: "mp4,m4v,mkv,webm,ts" },
      { Type: "Audio", Container: "mp3,aac,flac,ogg,wav" },
    ],
    TranscodingProfiles: [{ Type: "Video", Container: "ts", Protocol: "hls", VideoCodec: "h264", AudioCodec: "aac" }],
    SubtitleProfiles: [{ Format: "webvtt", Method: "External" }, { Format: "srt", Method: "External" }],
  };
}

async function negotiate(item: CastMediaItem): Promise<ResolvedMedia> {
  if (!credentials) throw new CastProtocolError("Cast-innlogginga er utgått");
  const endpoint = serverEndpoint(`Items/${encodeURIComponent(item.itemId)}/PlaybackInfo`);
  endpoint.searchParams.set("UserId", credentials.userId);
  endpoint.searchParams.set("StartTimeTicks", String(item.resumePositionMs * 10_000));
  if (item.sourceId) endpoint.searchParams.set("MediaSourceId", item.sourceId);
  if (item.audioStreamIndex !== undefined) endpoint.searchParams.set("AudioStreamIndex", String(item.audioStreamIndex));
  if (item.subtitleStreamIndex !== undefined) endpoint.searchParams.set("SubtitleStreamIndex", String(item.subtitleStreamIndex));
  const response = await fetch(endpoint, {
    method: "POST",
    headers: { ...serverHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({
      UserId: credentials.userId,
      StartTimeTicks: item.resumePositionMs * 10_000,
      MediaSourceId: item.sourceId,
      AudioStreamIndex: item.audioStreamIndex,
      SubtitleStreamIndex: item.subtitleStreamIndex,
      DeviceProfile: deviceProfile(),
    }),
    redirect: "error",
  });
  if (response.status === 401 || response.status === 403) throw new CastProtocolError("Cast-innlogginga er utgått");
  if (!response.ok) throw new CastProtocolError("Tenaren kunne ikkje førebu videoen");
  const payload = await response.json();
  const sourceInfo = Array.isArray(payload.MediaSources) ? payload.MediaSources[0] : undefined;
  if (!sourceInfo) throw new CastProtocolError("Tenaren fann ingen spelbar video");
  const candidate = sourceInfo.SupportsDirectPlay && sourceInfo.DirectStreamUrl
    ? sourceInfo.DirectStreamUrl
    : sourceInfo.TranscodingUrl || sourceInfo.DirectStreamUrl;
  if (typeof candidate !== "string" || !candidate) throw new CastProtocolError("Formatet kan ikkje spelast på Chromecast");
  const stream = routeUrl(candidate, credentials);
  return {
    contentId: stream.toString(),
    contentType: candidate.includes(".m3u8") || candidate.includes("hls") ? "application/x-mpegURL" : "video/mp4",
    sourceId: String(sourceInfo.Id ?? item.sourceId ?? ""),
    sessionId: String(payload.PlaySessionId ?? ""),
  };
}

async function report(event: "Playing" | "Progress" | "Stopped", currentTime = 0, paused = false) {
  if (!credentials || !active) return;
  const endpoint = serverEndpoint(`Sessions/${event}`);
  const positionTicks = Math.max(0, Math.round(currentTime * 10_000_000));
  const body = {
    ItemId: active.item.itemId,
    MediaSourceId: active.media.sourceId,
    PlaySessionId: active.media.sessionId,
    PositionTicks: positionTicks,
    IsPaused: paused,
  };
  try {
    await fetch(endpoint, { method: "POST", headers: { ...serverHeaders(), "Content-Type": "application/json" }, body: JSON.stringify(body), redirect: "error" });
  } catch {
    // Playback must keep going if reporting is briefly unavailable.
  }
}

/**
 * CAF owns ordinary next/previous handling. This custom queue merely supplies the one bounded
 * shape Spole permits: the selected episode followed by items already validated as its season.
 * Queue data stays public — the credential envelope is held separately in this receiver process.
 */
const SpoleSeasonQueue = class extends cast.framework.QueueBase {
  initialize(loadRequestData: any) {
    const spec = parseLoadSpec(loadRequestData.media?.customData);
    credentials = parseCredentialEnvelope(JSON.parse(loadRequestData.credentials ?? "{}"), spec.service);
    seasonQueue = { spec, prepared: new Map() };
    const queueData = new cast.framework.messages.QueueData();
    queueData.name = spec.items.length > 1 ? "Resten av sesongen" : spec.items[spec.startIndex].title;
    queueData.items = spec.items.map((item, index) => {
      const queueItem = new cast.framework.messages.QueueItem();
      const media = new cast.framework.messages.MediaInformation();
      media.contentId = `spole://queue/${encodeURIComponent(item.itemId)}`;
      media.contentType = "video/mp4";
      media.customData = { spoleQueueIndex: index };
      media.metadata = new cast.framework.messages.GenericMediaMetadata();
      media.metadata.title = item.title;
      media.metadata.subtitle = item.subtitle ?? "";
      queueItem.media = media;
      queueItem.autoplay = true;
      return queueItem;
    });
    queueData.startIndex = spec.startIndex;
    queueData.startTime = spec.items[spec.startIndex].resumePositionMs / 1_000;
    return queueData;
  }
};

function queuedIndex(request: any): number | undefined {
  const value = request.media?.customData?.spoleQueueIndex;
  return Number.isInteger(value) && value >= 0 && value < (seasonQueue?.spec.items.length ?? 0) ? value : undefined;
}

async function resolveQueueItem(request: any, allowCredentials: boolean): Promise<{ item: CastMediaItem; index: number; media: ResolvedMedia }> {
  let index = queuedIndex(request);
  if (index === undefined) {
    const spec = parseLoadSpec(request.media?.customData);
    credentials = parseCredentialEnvelope(JSON.parse(request.credentials ?? "{}"), spec.service);
    seasonQueue = { spec, prepared: new Map() };
    index = spec.startIndex;
  } else if (!credentials && allowCredentials) {
    throw new CastProtocolError("Manglar Cast-innlogging");
  }
  const queue = seasonQueue;
  if (!queue || !credentials) throw new CastProtocolError("Manglar Cast-innlogging");
  const item = queue.spec.items[index];
  const media = queue.prepared.get(index) ?? await negotiate(item);
  queue.prepared.set(index, media);
  return { item, index, media };
}

window.addEventListener("load", () => {
  const context = cast.framework.CastReceiverContext.getInstance();
  const playerManager = context.getPlayerManager();
  const playbackConfig = new cast.framework.PlaybackConfig();
  const attachHeaders = (requestInfo: any) => {
    requestInfo.headers = { ...(requestInfo.headers ?? {}), ...serverHeaders() };
    return requestInfo;
  };
  playbackConfig.manifestRequestHandler = attachHeaders;
  playbackConfig.segmentRequestHandler = attachHeaders;
  playbackConfig.captionsRequestHandler = attachHeaders;
  playerManager.setPlaybackConfig(playbackConfig);

  playerManager.setMessageInterceptor(cast.framework.messages.MessageType.LOAD, async (request: any) => {
    try {
      const { item, media } = await resolveQueueItem(request, true);
      active = { item, media };
      request.media.contentId = media.contentId;
      request.media.contentType = media.contentType;
      request.media.duration = undefined;
      request.media.metadata = new cast.framework.messages.GenericMediaMetadata();
      request.media.metadata.title = item.title;
      request.media.metadata.subtitle = item.subtitle ?? "";
      title.textContent = item.title;
      source.textContent = credentials?.service === "JELLYFIN" ? "Jellyfin" : "Emby";
      say("Klargjer avspeling");
      return request;
    } catch (error) {
      clearCredentials();
      say(error instanceof Error ? error.message : "Cast kunne ikkje starte");
      throw error;
    }
  });
  // CAF may ask for the next item before the current one finishes. Resolve it with the same
  // short-lived credential, but do not move [active] until the actual LOAD starts reporting.
  playerManager.setMessageInterceptor(cast.framework.messages.MessageType.PRELOAD, async (request: any) => {
    try {
      const { media } = await resolveQueueItem(request, false);
      request.media.contentId = media.contentId;
      request.media.contentType = media.contentType;
      return request;
    } catch (error) {
      say(error instanceof Error ? error.message : "Neste episode kunne ikkje klargjerast");
      throw error;
    }
  });
  playerManager.setMessageInterceptor(cast.framework.messages.MessageType.STOP, (request: any) => {
    void report("Stopped");
    clearCredentials();
    return request;
  });
  playerManager.addEventListener(cast.framework.events.EventType.PLAYER_LOAD_COMPLETE, () => { say(""); void report("Playing"); });
  playerManager.addEventListener(cast.framework.events.EventType.TIME_UPDATE, (event: any) => {
    const now = Date.now();
    if (now - lastProgressMs >= 10_000) { lastProgressMs = now; void report("Progress", event.currentTime ?? 0, false); }
  });
  playerManager.addEventListener(cast.framework.events.EventType.MEDIA_FINISHED, (event: any) => {
    void report("Stopped", event.currentTime ?? 0, false);
    const index = active ? seasonQueue?.spec.items.findIndex(item => item.itemId === active?.item.itemId) ?? -1 : -1;
    const next = index >= 0 ? seasonQueue?.spec.items[index + 1] : undefined;
    if (next) {
      say(`Neste: ${next.title}`);
    } else {
      clearCredentials();
      say("Avspelinga stoppa. Sjå på telefonen for detaljar.");
    }
  });
  context.start({
    disableIdleTimeout: false,
    statusText: "Spole",
    queue: new SpoleSeasonQueue(),
  });
});
