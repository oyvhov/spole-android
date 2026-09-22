# Spole 0.11.0 verification

- Release artifact: `Spole-v0.11.0-debug.apk`; package `app.reelstack.debug`, version name `0.11.0-debug`, version code `22`, minimum Android API 26.
- Versioned debug app and Android test APK build successfully. All 145 JVM tests pass. Lint: 0 errors, 20 warnings.
- All 61 Android instrumentation tests pass on the isolated API 36 emulator against the exact release APK (131.244 seconds). Includes linked login in both directions, non-admin authentication, refusal of mismatched accounts, rejected companion credentials, sign-out, clipboard content, personal-view permissions, request flow, native icon rendering, setup, and screen regressions.
- The same APK was installed in place on the signed-in review emulator. Home still showed Spole, the verified Seerr account and actual library content. No real user account was signed out or replaced, and no playback/request/server-setting mutation was used for verification.
- Compared signing certificates with the APK downloaded from the existing v0.10.1 GitHub release. Both SHA-256 certificate digests are `a5dfdbc82a57f1b84c3778485dc448d589f9d7b6ae4566de3648b4eb777a872b`. Debug signing and application ID are unchanged, supporting in-place updates.
- APK SHA-256: `ec04412dda7f11c05f5ff318d1098e71b6eddae44c243af5f13f461b7756b4ad`.
- Runtime screenshots, account data and previous-release comparison APK remain under ignored build output. Only brand artwork, application sources, tests and documentation are committed.
- Real Emby password authentication has not been exercised with the user's private credentials. Unit/loopback tests verify the protocol and personal-session handling; the user completes live account sign-in.
- Publication was explicitly authorized after the earlier publication pause.
