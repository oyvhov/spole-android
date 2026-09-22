# v0.10.1 verification

- Native compilation, debug/test APK builds and 140 JVM tests passed. Android lint completed with 0 errors and 18 warnings.
- The complete 51-test Android suite passed on the isolated API 36 emulator, including four new Home header cases. These cover Seerr priority, Jellyfin/Emby fallback, account navigation, rejecting shared identities, no greeting/date and no empty/checking playback messages during refresh.
- 11 additional Home/header/viewer tests passed at 150% font scale. The test emulator's font scale was restored afterwards.
- Live in-place installation retained the signed-in services. The Home header displayed the actual Seerr profile photograph, the profile action opened Settings, and Home omitted the date, greeting and empty playback section. Private screenshots remain under ignored `app/build` output, not in the release.
- The existing `AccountAvatar` component and design-system colours/touch targets are reused. The header retains stable dimensions while photographs load, with the selected person's initial as fallback.
- No real requests, playback commands or server configuration changes were made during verification. The app ID and signing certificate remain unchanged.
