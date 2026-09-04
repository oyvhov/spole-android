# HomeReel Android

HomeReel is a native Android companion for a self-hosted media stack. It brings Jellyfin, Emby, Seerr, Radarr, and Sonarr into one calm, cinematic interface without replacing the servers themselves.

The native app includes an aggregated multi-server Home, four interactive destinations, personal service accounts, a resilient live-data layer, offline dashboard cache, and background refresh. First-run setup offers an explicit demo preview. Once a service is connected, unconfigured services no longer contribute sample content.

## Download

Install the newest APK from [GitHub Releases](https://github.com/oyvhov/reelstack-android/releases/latest). The current early-access build is debug-signed and intended for direct testing, not Play Store distribution.

## Included now

- Native Kotlin and Jetpack Compose UI; no embedded web app
- Matte charcoal design with warm white type, lime accents, borderless posters and navigation that reserves space for content
- First-run service checklist, address-first connection setup, preserved tab scroll state and working Discover filters
- Aggregated Home dashboard with content from every connected server; no server switching required
- Multiple simultaneous Jellyfin/Emby sessions with independent playback controls
- Live Seerr search with one-tap “add to media collection” actions
- Unified activity timeline for Seerr, Sonarr, and Radarr events
- Settings and connection editors for Jellyfin, Emby, Seerr, Radarr, and Sonarr
- Native Jellyfin Quick Connect plus username/password and advanced access-token setup
- Seerr Jellyfin account sign-in and Seerr-mediated Quick Connect with encrypted session cookies and CSRF support
- Real connectivity/authentication probes for all five services
- Live Jellyfin/Emby playback sessions
- Separate Jellyfin/Emby recently-added movie and episode rails across all accessible libraries, with wide Thumb artwork for series
- Real Jellyfin/Emby remote pause and resume commands with pending/error feedback
- Live Radarr/Sonarr download queues with normalized progress
- Artwork-led Upcoming section and a 28-day agenda calendar: Radarr home releases (not cinema-only dates) plus Sonarr episodes
- Live Seerr trending discovery, request activity, and request submission
- Independent per-service refresh errors, stale-data retention, empty states, and pull-to-refresh
- Persistent non-secret dashboard cache and 30-minute WorkManager refresh with an optional Wi-Fi-only constraint
- Per-section Home visibility controls in Settings
- Remote artwork with local fallbacks and HTTPS-only external image filtering
- Android Keystore-backed AES/GCM encryption for API tokens
- HTTPS by default; plain HTTP accepted only for localhost and private-LAN hosts
- Section-shaped shimmer loading, animated navigation, uncropped film posters, wide series art, rich title sheets, feedback, and large touch targets
- Unit, lint, and on-device Compose smoke-test coverage

## Run it

Open this folder in Android Studio, let Gradle sync, and run the `app` configuration on an Android 8.0 or newer device.

From PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Connecting services

Choose a service during setup or in **Innstillingar**, enter its address and continue to sign-in. Jellyfin defaults to Quick Connect: approve the temporary code in an already signed-in Jellyfin client. Username/password and access-token setup are also available. Connection names and optional Jellyfin/Emby profile IDs live under advanced settings.

Seerr defaults to **Jellyfin-konto**, using the same username/password as your Jellyfin account through Seerr's own login endpoint. Seerr's server-side permissions apply to requests. Quick Connect is also available when the installed Seerr version supports it; older versions can use account login. Administrator API keys remain an alternative. Passwords are never stored. Tokens and Seerr session cookies are encrypted on the device; expired Seerr sessions ask for sign-in again.

Typical local addresses:

- Jellyfin: `http://192.168.1.20:8096`
- Emby: `http://192.168.1.20:8096`
- Seerr: `http://192.168.1.20:5055`
- Radarr: `http://192.168.1.20:7878`
- Sonarr: `http://192.168.1.20:8989`

Use HTTPS through a trusted reverse proxy when the services are reachable outside your home network. API tokens are never placed in URLs, logs, or the dashboard cache, and are encrypted at rest. Android backups are disabled for the app's local connection data.

## Architecture

- `ui/`: Compose navigation, screens, sheets, state, and theme
- `data/model/`: shared service and activity models
- `data/network/`: URL validation, HTTP transport, and service-specific probes
- `data/repository/`: connection profile coordination
- `data/security/`: encrypted token persistence

The app uses a single activity, immutable UI state, unidirectional events, and explicit service boundaries so live repositories can replace the demo feed without redesigning the screens.

## Next milestone

The next implementation pass expands actions and reliability: Radarr/Sonarr history and queue actions, paginated Seerr results, playback deep links, notification channels, and adaptive tablet layouts. See `docs/PRODUCT_PLAN.md` for the sequence and acceptance criteria.
