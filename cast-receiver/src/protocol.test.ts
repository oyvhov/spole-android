import { describe, expect, it } from "vitest";
import { CastProtocolError, containsCredential, parseCredentialEnvelope, parseLoadSpec, routeUrl } from "./protocol";

const spec = { version: 1, service: "JELLYFIN", profileId: "adult", startIndex: 0, items: [{ itemId: "movie", title: "Film", resumePositionMs: 1200 }] };
const credentials = { version: 1, service: "JELLYFIN", baseUrl: "https://media.example/spole", userId: "u", accessToken: "secret", deviceId: "cast" };

describe("Cast receiver protocol", () => {
  it("keeps credentials out of the load contract", () => {
    expect(parseLoadSpec(spec)).toEqual(spec);
    expect(containsCredential(spec)).toBe(false);
  });
  it("accepts a credential envelope only for its service", () => {
    expect(parseCredentialEnvelope(credentials, "JELLYFIN").userId).toBe("u");
    expect(() => parseCredentialEnvelope({ ...credentials, service: "EMBY" }, "JELLYFIN")).toThrow(CastProtocolError);
  });
  it("keeps media on the selected route and strips query credentials", () => {
    const parsed = parseCredentialEnvelope(credentials, "JELLYFIN");
    expect(routeUrl("/spole/Videos/movie/stream?api_key=nope", parsed).toString()).toBe("https://media.example/spole/Videos/movie/stream");
    expect(() => routeUrl("https://elsewhere.example/video", parsed)).toThrow(CastProtocolError);
  });
});
