# HomeReel Android

HomeReel is a native Android companion for a self-hosted media stack. It brings Jellyfin, Emby, Seerr, Radarr, and Sonarr into one calm, cinematic interface without replacing the servers themselves.

This repository contains the fourth native milestone: an aggregated multi-server Home, four interactive destinations, secure connection profiles, a resilient live-data layer, offline dashboard cache, and background refresh. Curated demo content remains available until a service is connected; configured services then replace their portion of the UI with live data.

## Download

Install the newest APK from [GitHub Releases](https://github.com/oyvhov/reelstack-android/releases/latest). The current early-access build is debug-signed and intended for direct testing, not Play Store distribution.

## Included now

- Native Kotlin and Jetpack Compose UI; no embedded web app
- Aggregated Home dashboard with content from every connected server; no server switching required
- Multiple simultaneous Jellyfin/Emby sessions with independent playback controls
- Live Seerr search with one-tap “add to media collection” actions
- Unified activity timeline for Seerr, Sonarr, and Radarr events
- Settings and connection editors for Jellyfin, Emby, Seerr, Radarr, and Sonarr
- Native Jellyfin Quick Connect plus username/password and advanced access-token setup
- Real connectivity/authentication probes for all five services
- Live Jellyfin/Emby playback sessions
- Separate Jellyfin/Emby recently-added movie and series rails across all accessible libraries
- Real Jellyfin/Emby remote pause and resume commands with pending/error feedback
- Live Radarr/Sonarr download queues with normalized progress
- 28-day Radarr/Sonarr upcoming calendar with movie and episode artwork
- Live Seerr trending discovery, request activity, and request submission
- Independent per-service refresh errors, stale-data retention, empty states, and pull-to-refresh
- Persistent non-secret dashboard cache and 30-minute WorkManager refresh with an optional Wi-Fi-only constraint
- Per-section Home visibility controls in Settings
- Remote artwork with local fallbacks and HTTPS-only external image filtering
- Android Keystore-backed AES/GCM encryption for API tokens
- HTTPS by default; plain HTTP accepted only for localhost and private-LAN hosts
- Section-shaped shimmer loading, animated navigation, cinematic artwork-led detail sheets, progress states, feedback, and large touch targets
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

Open **Settings**, choose a service, then enter a display name and base URL. For Jellyfin, Quick Connect is the default: HomeReel displays a temporary code that you approve under **Settings → Quick Connect** in a Jellyfin client where you are already signed in. Username/password and advanced access-token setup are also available. Passwords are never saved, and the returned access token is encrypted on the device. Jellyfin/Emby API keys can use an optional profile ID.

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
