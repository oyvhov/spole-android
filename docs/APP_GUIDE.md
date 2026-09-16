# Getting started with Spole

This guide applies to Spole 0.17.0-beta09.

## For viewers

1. Install the APK from [Spole Releases](https://github.com/oyvhov/spole-android/releases). Android may ask you to allow installation from the browser or file app you use.
2. Open Spole and select **Get started**.
3. If you received a setup link, open it or select **I have a setup link** and paste it. Check the server addresses; without a link, enter them manually.
4. Select **Approve on phone** and approve the displayed Quick Connect code in a Jellyfin client where you are already signed in, or choose username and password.
5. Spole opens Home after sign-in is confirmed. If Seerr was selected during setup, its personal login is completed in the same flow when the server supports it.

On TV, a phone remote-control app can make text entry easier. A setup link is not a login by itself; you still approve your own account.

### When setup stops

- **The link does not open the app:** install Spole first, then paste the link into setup.
- **The server does not respond:** check the address and confirm that the phone or TV can reach the server network.
- **Quick Connect is unavailable:** try username and password, or contact the server administrator.
- **Jellyfin works but Seerr fails:** Seerr must be connected to the same Jellyfin server and support the selected sign-in method. You can disable Seerr and sign in to Jellyfin alone, or use separate sign-ins.

## For people sharing the app

1. Create a separate Jellyfin account for each viewer and grant access only to the libraries they should use.
2. If viewers should request content, connect Seerr to the same Jellyfin server and check the user's permissions there.
3. Test the server addresses from the network the recipient will use. A local home address does not automatically work outside the home.
4. In Spole, open Jellyfin under **Settings -> Services**. Select **Share setup with a user** or **Copy setup link**.
5. Send the recipient the [APK link](https://github.com/oyvhov/spole-android/releases) and the setup link. The recipient signs in with their own account.

The setup link contains the Jellyfin address and, optionally, the Seerr address. It
does not contain passwords, access tokens or a user session. Addresses may still be
private, so share the link with its intended recipients rather than publishing a
screenshot. Never share Radarr, Sonarr or Seerr administrator tokens with regular users.

## TV, tablet and phone

- **TV:** use the D-pad and OK. Back exits the current panel or screen. TV has its own settings categories and remote-first navigation.
- **Tablet:** wide windows provide side navigation and more room for content.
- **Phone:** bottom navigation and grouped settings are used for the smaller layout.

Accent colour, seasonal themes and visible Home rows can be adjusted in Settings.
On TV, the side panel can also be hidden until you navigate to the left edge.

## Current features

- **Separate Jellyfin and Emby libraries:** each service keeps its own library, Continue Watching rows and playback state.
- **Cinematic detail pages:** backdrops, clearlogos, seasons, episodes, cast, age ratings, runtime, estimated finish time and visual ratings when supplied by the server.
- **Integrated playback:** audio and subtitle selection, file and quality selection, progress reporting and next episode. Direct playback is requested when possible, with audio-only or full server transcoding fallback when required by the device or media.
- **TV playback controls:** seeking, audio, subtitles, next episode and playback diagnostics. The Speed action is intentionally not part of the current OSD.
- **Requests and discovery:** Seerr requests plus Radarr and Sonarr upcoming content when the connected account has access.
- **Themes:** accent colours, Christmas and Halloween styling, with reduced-motion support.

## Updates and limitations

Use the app's update check or install a newer APK from Releases over the existing
installation. You do not need to uninstall first. Pre-release builds can contain bugs.

Format support depends on the Android device, player and server transcoding
permissions. Image-based subtitles may require burn-in. Intro and credits actions
require markers from the server; Emby uses its chapter markers. Live TV, music,
disc/ISO menus and offline downloads are outside this playback flow. Child mode,
QR display for setup links and short setup codes are not implemented.

[Back to Spole](../README.md)
