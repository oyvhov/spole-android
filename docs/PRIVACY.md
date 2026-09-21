# Privacy in Spole

Last updated: September 16, 2026. Applies to Spole 0.17.0-beta09 and newer.

Spole is a client for media services that you operate yourself. The developer has no
central server, account system or database for Spole users. There is nowhere for us
to collect your media data, and we do not do so.

## In short

- Spole sends media requests only to the server addresses that you provide.
- Nothing is sent to the developer. There is no analytics, tracking or advertising.
- Credentials are stored encrypted on the device and do not leave it through Spole.
- In addition to your own servers, the app can contact TMDB for artwork and GitHub for the update catalogue. Those services receive the request and your IP address, not your identity or account credentials.

## What is stored on the device

| Data | Location | Encrypted |
| --- | --- | --- |
| Access tokens and Seerr sessions | App-private storage | Yes, with Android Keystore (AES-GCM) |
| Server addresses, user IDs and preferences | App-private storage | No; these are not secrets |
| A random device ID | App-private storage | No |
| Cached titles and artwork URLs | App-private SQLite database | No |
| Crash report, if created | App-private file | No; addresses and tokens are removed |

App-private storage is readable only by Spole. Access tokens are also excluded from
cloud backup and device transfer so they do not follow an installation to a new device.

**Passwords are never stored.** They are sent once to the service you sign in to; Spole
then keeps the access token returned by that service.

**Playback sessions, queues and the activity feed are not written to disk.** Only titles
and artwork URLs are cached so that Home has something to display while the first refresh runs.

## What is sent, and where

**To your own servers** (Jellyfin, Emby, Seerr, Radarr and Sonarr): your sign-in,
search terms, requests and a device ID so the server can show "Spole on Android" in
its device list. What these servers log depends on your own configuration.

**During playback in the integrated player:** video and subtitles are fetched from
the selected Jellyfin or Emby server. The item ID, playback session, position,
pause/stop events and selected tracks are sent back to that same server under your
account so it can save progress. Android receives title and playback state through a
local media session for system and headset controls. Video is buffered in memory; the
player does not store a downloaded movie file or a permanent playback log.

**To `image.tmdb.org`:** poster and backdrop URLs that the app requests for artwork.
TMDB sees your IP address and the image requested. No account information or access
tokens are sent there; service credentials are only attached to your own server requests.

**To `raw.githubusercontent.com`:** one static JSON file containing the update catalogue.
GitHub sees your IP address and the requested file. Nothing about your media account is
sent. The catalogue request can be disabled in **Settings -> Home -> Recommendations**.

**To `api.github.com` and GitHub release URLs:** the app can check for official Spole
releases at startup, at most every twelve hours. GitHub receives your IP address and
app version, but no media accounts, access tokens or library data. Automatic checks can
be disabled under **Settings -> Updates**. An APK is downloaded only after you choose
the update; the checksum, package name, version and signing certificate are checked
before Android asks for installation approval. The download stays in app-private cache
and can be removed by Android.

No other external addresses are contacted by Spole for these features.

## Device ID

Spole creates a random device ID the first time it starts and sends it to Jellyfin and
Emby so they can distinguish this installation from other devices in their device lists.
It is not a hardware ID. It is random, unique to the installation and removed when the
app is uninstalled.

Installations from before 0.14.0 keep the ID they already had so an update does not
create a duplicate device entry on the server.

## Crash reports

If Spole stops unexpectedly, it can write a file containing the app version, device
model and what went wrong. Server addresses and anything resembling an access token are
removed before the file is written.

The file is never sent anywhere automatically. It stays on the device until you share
it from Settings or delete it there.

## Unencrypted traffic

Self-hosted servers often use HTTP on a private home network. Spole permits it only for
`localhost`, `.local` names and literal private addresses (`10.x`, `172.16-31.x`,
`192.168.x`, Tailscale sitt `100.64.0.0/10`, `::1`, `fc00::/7`, `fe80::/10`). An HTTP
address pointing til ein offentleg vert er avvist, ikkje berre åtvara om.

When HTTP is used, Settings shows which connections are affected. Outside your own
network, use HTTPS through your own proxy.

## Deletion

Uninstall the app to remove the local data listed above. You can also sign out of one
service at a time under Settings; that immediately deletes that service's access token.

Spole cannot and does not delete anything from your servers. Cancelling a request in
the app performs the same action that Seerr itself would perform.

## Children

Spole does not collect data and has no separate age requirement for privacy reasons.
The content shown by the app is the content available on your own servers.

## Changes

This file is updated with the app. The date at the top shows when it was last reviewed.
Significant changes are also recorded in `CHANGELOG.md`.

## Contact

Privacy questions can be sent as an issue in the [GitHub repository](https://github.com/oyvhov/spole-android/issues).
