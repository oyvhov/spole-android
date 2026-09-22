<p align="center">
  <img src="design/brand/spole-icon.svg" width="96" alt="Spole icon" />
</p>

<h1 align="center">Spole</h1>
<p align="center">Your movies. Your shows. One place.</p>
<p align="center">Android TV · Google TV · Tablet · Phone</p>
<p align="center">
  <a href="https://github.com/oyvhov/spole-android/releases"><strong>Download APK</strong></a>
  &nbsp; · &nbsp;
  <a href="docs/APP_GUIDE.md">Setup guide</a>
  &nbsp; · &nbsp;
  <a href="https://github.com/oyvhov/spole-android/issues">Report an issue</a>
</p>

Spole is a native Android client that brings Jellyfin, Emby, Seerr, Radarr and Sonarr
into one focused experience. Browse each media server as its own library, play from
Jellyfin or Emby, and track your requests from Seerr.

**Active beta development.** The latest public APK is
[0.18.0-beta2](https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta2).
It is signed for in-place updates over earlier Spole beta releases. The source for
this build is available on [main](https://github.com/oyvhov/spole-android/tree/main).
Pre-release builds are published under [Releases](https://github.com/oyvhov/spole-android/releases).

## On your screens

### TV
![Spole on TV with cinematic hero, content rows and remote-friendly navigation](docs/images/devices/tv-home.png)

The TV experience is designed for remote control: a persistent side panel,
horizontal content rows, focused settings, and a playback OSD with seek, audio,
subtitles and next-episode controls.

### Tablet
![Spole on tablet with a wide home layout and side navigation](docs/images/devices/tablet-home.png)

A wide layout keeps navigation visible while giving content more room.

### Phone
<p align="center">
  <img src="docs/images/devices/phone-home.png" width="32%" alt="Spole home on phone" />
  &nbsp;
  <img src="docs/images/devices/phone-discover.png" width="32%" alt="Discover on phone" />
  &nbsp;
  <img src="docs/images/devices/phone-settings.png" width="32%" alt="Settings on phone" />
</p>

Bottom navigation and grouped settings make the smaller layout easy to scan.

*Screenshots were captured in the emulator on September 13, 2026, using demo data.
They are representative and may differ from the current beta. [View all device
screenshots and anonymisation notes](docs/images/devices/README.md).*

### Christmas and Halloween

<p>
  <img src="docs/images/devices/tv-home-christmas.png" width="49%" alt="Christmas theme on TV" />
  <img src="docs/images/devices/tv-home-halloween.png" width="49%" alt="Halloween theme on TV" />
</p>

Seasonal themes can change the background, menu accents and decoration. Decoration
can be disabled, and reduced motion keeps the seasonal styling static. [See the
seasonal screens for all three form factors](docs/images/devices/README.md#christmas-and-halloween).

## Features

- **Separate Jellyfin and Emby experiences.** Libraries, Continue Watching rows and playback state stay tied to the service they came from instead of being mixed together.
- **Integrated playback.** Spole requests direct playback when possible and can fall back to audio-only or full server transcoding when the device or media requires it. Jellyfin playback is verified on Android TV; Emby playback is implemented, but some files still need investigation. Actual format support depends on the Android device, player and server configuration.
- **Cinematic detail pages.** Backdrops, clearlogos, seasons, episodes, cast, age ratings, runtime and estimated finish time are presented in an Infuse-inspired TV layout.
- **Visual ratings.** TMDB and Rotten Tomatoes ratings use their visual marks when the server provides the data, with ratings kept on one clear line.
- **Remote-first TV navigation.** The interface, menus and playback controls are built around predictable D-pad focus and short actions.
- **Requests and discovery.** Browse Seerr, request movies or seasons, follow request status, and see upcoming Radarr and Sonarr items when your account has access.
- **Themes.** Personalise accent colours, seasonal Christmas and Halloween themes, home rows and TV side-panel options.
- **In-app updates.** Spole can check a published APK on GitHub and verify it before Android asks to install the update.

The production APK uses a small, plain placeholder when artwork is unavailable. It
does not bundle the large demo backdrops used during earlier development builds.

## Supported services

| Service | Available in Spole |
| --- | --- |
| **Jellyfin** | Library browsing, separate Continue Watching, favourites, playback, audio and subtitle selection, and Quick Connect or password login. |
| **Emby** | Library browsing, separate Continue Watching, favourites, playback, audio and subtitle selection, and server or password login. |
| **Seerr** | Discover titles, request movies and seasons, and follow your own request status. |
| **Radarr / Sonarr** | Upcoming content and download status when the connected account has access. |

All services are optional. You need your own servers and accounts; Spole does not
provide media. Your account permissions determine what you can view and do.

## Known limitations

- **Emby playback:** Library browsing, artwork and ordinary direct playback are supported. Some transcoded streams and uncommon audio formats may still need fixes for the particular Emby version.
- **Offline:** Complete, directly compatible Jellyfin and Emby files can be added from the phone or tablet player to an app-private, account-scoped Media3 queue. TV never offers downloads. Storage limits and a dedicated downloads library are needed before offline is broadly available. See [mobile playback and offline](docs/MOBILE_PLAYBACK_AND_OFFLINE.md).
- **Physical devices:** Continuous integration and regression testing run on Google Android TV and phone emulators (API 26–36). Manufacturer variants, such as Fire OS or operator-customised boxes, can differ slightly in appearance or remote behaviour.
- **Parental controls and PIN:** Leaving kids mode is protected with PBKDF2-HMAC-SHA256 (100,000 rounds) and increasing delays after failed attempts. Older lower-cost PIN records are strengthened after a correct PIN. Resetting a forgotten PIN requires the primary adult media-server account.

## Getting started

1. Download the APK from [Releases](https://github.com/oyvhov/spole-android/releases) and install it on Android 8.0 or newer.
2. Select **Get started**. Open or paste a setup link from your server administrator, or enter the server addresses manually.
3. Use **Approve on phone** with Quick Connect, or sign in with a username and password.
4. Connect Seerr, Emby, Radarr or Sonarr separately when you want those services in the app.

Other sign-in methods are available under **Other sign-in methods**. A demo mode is
also available for trying the interface without connecting a server.

When sharing Spole with family or friends, share the APK link and a setup link.
Setup links contain server addresses, never passwords or access tokens. Each person
signs in with their own account. See the [user and administrator guide](docs/APP_GUIDE.md).

## Privacy

Credentials are stored encrypted on the device. Spole uses the server addresses you
provide and has no central account service. TMDB may be used for artwork, while
GitHub is used for the update catalogue and published updates; those services receive
the request and your IP address. App update checks also send the Spole version.
Account credentials are not sent to GitHub.

The screenshots in this repository use demo data. No real names, email addresses,
server addresses, login codes or access tokens are published in them. Read the
[privacy policy](docs/PRIVACY.md) for the current source tree.

## Feedback and development

[Report a bug or request a feature](https://github.com/oyvhov/spole-android/issues).
Include the app version, device and the action that led to the problem. Remove names,
addresses and login details before sharing screenshots or logs.

Spole is built with Kotlin and Jetpack Compose. The `main` branch contains the
current source; release tags identify the source used for each published APK. Read
the project [AGENTS.md](AGENTS.md) and [docs/AI_INSTRUCTIONS.md](docs/AI_INSTRUCTIONS.md)
before making changes.

Spole is not affiliated with or endorsed by Jellyfin, Emby, Seerr, Radarr, Sonarr or
TMDB. Their names and marks belong to their respective owners.
