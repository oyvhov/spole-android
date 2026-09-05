# Viewer access in 0.10.0

The app's global administrator experience requires the ADMIN bit from Seerr's fresh `/api/v1/auth/me` response. A Jellyfin administrator token alone does not enable it. Ordinary users remain personal even when Seerr grants REQUEST_VIEW or MANAGE_REQUESTS: this is deliberately stricter than Seerr's general overview permissions.

## Data boundaries

- Jellyfin/Emby identities come from `/Users/Me`, not a connection name or manually entered profile ID. Sessions are filtered by exact `UserId` before mapping artwork or rendering. Equal display names never establish ownership.
- A linked Seerr Jellyfin ID must match the Jellyfin identity for a non-administrator. A shared media-admin account cannot stand in for a different ordinary user. If the necessary identity cannot be verified, sessions are empty.
- Verified Seerr administrators can obtain the server's session overview through an administrator media connection or a server API key. The media server still determines what its credential permits.
- Ordinary Seerr request feeds send `requestedBy` and filter the returned owner again. Personal Activity uses the account-scoped request tracker, not the shared Radarr/Sonarr queue.
- Shared `/queue` calls are skipped entirely for ordinary users. Release calendars are still fetched. Calendar dates are release information, not evidence that a user downloaded something.
- A request rechecks the actual actor, movie/TV request permissions and season eligibility before writing. Administrator API keys cannot submit personal requests. Remote playback commands recheck that the selected session is still visible to the current viewer.
- Account replacement/removal cancels the old refresh and clears the previous feed. Shared activity, queue and sessions are not saved in the dashboard cache. Startup does not restore an unverified dashboard; library refresh failures do not preserve a previous access scope.
- Ordinary configured Seerr users do not see shared Emby/Radarr/Sonarr connection editors. Stored secrets are not prefilled for an unverified administrator.

## Library exclusions

Recent-media queries use allowed library parent IDs only. Normalized exact names excluded are Barneserier, Barneseriar, Barne-TV, Barne-Tv Serier and Barne-TV Seriar. The last two naming patterns were checked against the user's actual Jellyfin/Emby views. Barnefilmar and unrelated libraries are not excluded. No server libraries, permissions or media files are changed. Empty/failed library discovery never triggers an unscoped library query.

This is a named-library exclusion for the Home feeds, not parental-control enforcement. Seerr discovery and global release calendars are not filtered by genre or age rating.

## Limits and deployment

Client-side filtering is not a substitute for server authorization. Do not distribute administrator API keys to ordinary users or treat a client holding such a key as a secure multi-tenant gateway. A hardened shared deployment should use personal server credentials or a server-side service that limits access. Seerr remains authoritative for approval rules, quotas and final request acceptance. Unknown identities fail closed; the app does not guess a user's identity across independent Emby and Jellyfin installations.

References: [Seerr permissions](https://github.com/seerr-team/seerr/blob/develop/server/lib/permissions.ts), [Seerr request routes](https://github.com/seerr-team/seerr/blob/develop/server/routes/request.ts), [Jellyfin session controller](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/SessionController.cs).
