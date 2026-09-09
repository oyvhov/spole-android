# Wide content and real Google TV — 9 September 2026

## Implemented

- Compact wide-screen live-session rail; source/account filtering and existing playback authorization are unchanged.
- Discover heading, search and avatar share a row at ordinary wide-screen text sizes; large text retains a stacked layout.
- Visible remote focus on the library feature action and Discover filters.
- TV wide-canvas detection accounts for the navigation rail. Short-window library artwork is more compact.
- Shared existing matte surfaces, spacing, focus outlines and button patterns; no extra glass effects or auto-scrolling artwork.

## Verified

- 283 JVM tests, zero failures/errors.
- Debug, instrumentation and signed release builds succeed. Lint: zero errors, 32 warnings; warnings remain follow-up work.
- Updated production review device with `install -r`, retaining real accounts. Visually inspected Home and Discover at 1920×1200 / 240 dpi. Real library art, Seerr availability states and account avatar load. No active session remained at final capture, so the new compact session was fixture-tested, not visually verified against an ongoing live session.
- Local private screenshots: `app/build/wide-after.png` and `app/build/discover-after.png`. Not committed or published.
- Actual Google TV API 36 x86_64 revision 4 image, TV/Leanback features and television UI mode verified. AVD `Spole_GoogleTV_Test`, isolated port 5564, 1920×1080 at 320 dpi.
- 16 focused Android tests pass on that TV: WideContentTest (2), TvPlaybackControlsTest (3), WideNavigationSettingsTest (5), SetupAndDiscoverTest (5), and JellyfinPlayerTest.googleTvRemoteControlsRealVideoAndBackReturnsSafely (1).
- The player test decodes synthetic video through the actual player and loopback server. Remote media Pause is idempotent, Play resumes, first Back dismisses controls, second exits, and a Stopped event is reported. No real user's playback was controlled.
- The original sidebar test incorrectly assumed 600 dp was available on a 540 dp-high TV. It now verifies that collapse/expand preserves the actual starting height.

## Not yet verified / next TV work

- Full live-account Home → title → player journey on TV, physical remote/hardware, long playback and network recovery.
- TV onboarding needs a compact wide layout; its existing tall artwork puts service choices below the fold.
- TV launcher/banner packaging and complete TV readiness remain open. The isolated TV launches the activity explicitly without completing Google-account setup.
- No release or version bump in this source pass. Existing alpha05 download is not silently replaced.

## Device safety

The production review device at port 5560 retains its real accounts. Never run fixture instrumentation there. Google TV tests use only the isolated 5564 image; no account tokens are copied between devices. Private review screenshots stay under ignored `app/build/`.

Official platform reference: [Build TV apps](https://developer.android.com/training/tv/get-started/create).
