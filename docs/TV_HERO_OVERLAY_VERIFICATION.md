# TV home profile overlay — 9 September 2026

## Change

- Wide Home with a library feature no longer reserves a separate profile header row.
- The feature starts with 16 dp top spacing; the account avatar overlays its upper-right artwork on a matte circular surface.
- The existing profile identity and account action are retained, with shared keyboard/remote focus treatment.
- Phone and no-feature layouts keep their normal header.

## Verified

- [x] Release/debug builds and Android test APK built successfully.
- [x] 283 JVM tests passed; zero failures or errors.
- [x] Three TabletFeatureTest instrumentation tests passed on isolated emulator-5562, including large text and independent profile/title actions.
- [x] Android lint: zero errors, 32 warnings remain.
- [x] Release certificate verified against the existing Spole signing certificate.
- [x] Signed release installed in place on emulator-5564; real Jellyfin/Seerr accounts retained.
- [x] Actual Google TV Home screenshot visually checked: Ted Lasso library feature at the top, profile clear of text, more of the next row visible.

Local screenshot: `app/build/tv-hero-overlay.png` (ignored build artifact).

No live playback controls or media requests were sent. No instrumentation ran on the real-account TV device. No GitHub release or version bump was made for this change.

An existing Sonarr unit-test fixture used a fixed upcoming date that had elapsed. Its test-only dates now remain relative to the current time; production calendar logic was not changed.
