# Changelog

## 0.4.2

- Kept Jellyfin and Emby marked as connected when only a personal feed or playback-session call is unavailable.
- Added partial media-server refreshes so Recently Added can still load when Continue Watching needs a Profile ID.
- Added a global Jellyfin Recently Added fallback when no media profile can be detected.
- Distinguished connected-with-limited-data warnings from actual connection failures throughout Home and Settings.
- Replaced demo artwork on Jellyfin and Emby library/session cards with authenticated server artwork while keeping tokens out of image URLs.
- Added a neutral HomeReel placeholder for media that genuinely has no server artwork instead of showing an unrelated demo poster.

## 0.4.1

- Fixed Jellyfin library loading by using the current `/UserItems/Resume` and `/Items/Latest` routes with legacy fallbacks.
- Fixed Emby library loading by using its user-scoped resume and latest-media routes.
- Added automatic media-profile discovery for API-key connections without a configured profile ID.
- Added visible, actionable Home states when media-library refresh fails or returns no items.

## 0.4.0

- Renamed the app to HomeReel and introduced a new home-and-play launcher icon.
- Aggregated Jellyfin and Emby sessions, continue-watching items, and recently-added media on one Home screen.
- Added a multi-session carousel with independent pause and resume controls.
- Added a 28-day Upcoming rail backed by the Radarr and Sonarr calendar APIs.
- Added Home-section visibility controls under Settings.
- Added artwork-rich Activity rows and Seerr media-detail enrichment for recent requests.
- Replaced the empty playback card with a compact, subdued status line.
- Preserved cached content per service during partial refresh failures.

## 0.3.0

- Added live Jellyfin and Emby continue-watching and recently-added feeds.
- Added remote pause and resume for active Jellyfin and Emby sessions.
- Added a persistent, non-secret dashboard cache for useful startup and offline state.
- Added 30-minute background refresh with an optional Wi-Fi-only constraint.
- Added an optional Jellyfin/Emby user ID for server API-key setups.
- Added animated library rails, progress artwork, and media detail sheets.
- Expanded parser, client, cache, and on-device UI coverage.

## 0.2.0

- Added live playback, queue, Seerr discovery/request feeds, remote artwork, pull-to-refresh, and partial-service failure handling.

## 0.1.0

- Added the native Compose foundation, cinematic visual system, connection editors, encrypted token storage, and service health checks.
