# Verifisering — Spole 0.18.0-beta4

Dato: 22. september 2026

## Bygg og identitet

- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og
  `assembleRelease` er bestått frå release-commiten
  `472087d87f077c7bbc3842c750baf803ccb064d1`.
- 726 einingstestar: 726 bestått, 0 feil, 0 hoppa over. Lint: 0 feil.
- Pakke: `app.reelstack`; versjon: `0.18.0-beta4` (versionCode 124); minSdk 26.
- Signeringssertifikat SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK: `Spole-v0.18.0-beta4.apk`, 11 334 058 byte, SHA-256
  `79f963d270fef6b489559901bb99d6631e4832f0e14e1ed712eb449188007504`.

## Distribusjon og oppgradering

- GitHub-releasen `v0.18.0-beta4` er ein offentleg prerelease med nøyaktig éin APK,
  SHA256SUMS, R8-mapping, kjeldecommit og FFmpeg-kjelde-/relenkingsarkiv.
- GitHub sin asset-digest og ei offentleg, uautentisert nedlasting av APK-en er samanlikna
  byte-for-byte mot den lokale release-artefakten.
- Den bevarte mobilprofilen vart oppgradert med `adb install -r` frå `0.18.0-beta1` /
  versionCode 121 til beta4 / versionCode 124. Installeringa og påfølgjande oppstart av
  `MainActivity` var vellukka; konto og appdata vart ikkje sletta.

## Avgrensingar

- Instrumenterings-APK-en vart bygd, men instrumentering vart ikkje køyrd mot profilar med
  ekte kontoar. Dei blir aldri nullstilte eller brukte som test-AVD-ar.
- Ekte kontoar, tenesteadresser og skjermbilete er ikkje ein del av Git eller release-assetane.
