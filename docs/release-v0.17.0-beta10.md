# Spole 0.17.0-beta10

This prerelease focuses on Emby playback reliability on Android TV.

## Playback

- Emby playback requests now use Emby's native `X-Emby-Authorization` and `X-Emby-Token` headers.
- The HLS fallback is advertised as a streaming profile so Emby can negotiate direct stream and audio-only conversion correctly.
- Jellyfin keeps its existing `Authorization` flow and remains separate from Emby.
- Added regression tests for both service-specific headers and the streaming profile.

## Verification

- GitHub Actions: unit tests, lint, debug build, Android test APK build and unsigned release build passed.
- The signed APK uses the existing Spole production certificate and is installable over earlier Spole releases.
- Toy Story 5 is present in the local Emby library; final playback confirmation on the Android TV emulator follows installation of this release build.
