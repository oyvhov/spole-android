# v0.10.0 verification

## Automated evidence

- Native Kotlin compilation, debug APK, test APK and Android lint completed successfully. Lint reports 0 errors and 18 warnings; this is not a warning-free build.
- 140 JVM tests passed, including 14 new cases for viewer identity, administrator authority, request permissions, owner filtering, library exclusions and skipping shared queues.
- 47 Android instrumentation tests passed on the isolated API 36 test emulator. The first run found one obsolete text-badge assertion; it was changed to assert the new accessible cover-status icon, then the complete suite passed.
- 16 additional native UI checks passed at 150% font scale, covering request flow, calendar/sheets, settings, personal/admin modes, playback count, onboarding and Discover filters/actions. Font scale was restored afterwards.
- Native role tests cover personal Activity without admin filters, verified administrator filters, and explicit counts for simultaneous playback. Network tests use controlled responses, including equal usernames with different IDs and mismatched shared administrator credentials.

## Live checks and limitations

Existing signed-in accounts are retained by in-place installation. No real Seerr request, remote playback control, library modification or queue mutation is performed during verification. Account-specific live screenshots remain in ignored local build output and are not published.

Upcoming full-image cards and Discover poster/status/action cards were visually inspected with actual service artwork. Library discovery identified the actual excluded names as Jellyfin `Barne-TV` and Emby `Barne-Tv Serier`; filtering also supports Barneserier/Barneseriar variants without excluding children's films.

The ordinary-user boundary is covered by controlled identity/transport and native UI tests, not by signing into a second real non-administrator account. This does not establish the security of the user's server configuration or replace server-side permissions. See [viewer access](VIEWER_ACCESS.md).

The live emulator process stopped during parallel emulator use; no app data was cleared. After a cold start and another in-place install, all five services reported active, the verified administrator labels appeared, and the recent-series rails showed the regular TV library rather than the excluded children's TV library. Test instrumentation is run only on the isolated test emulator, never on the signed-in emulator.
