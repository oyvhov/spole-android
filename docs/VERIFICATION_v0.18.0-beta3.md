# Verifisering — Spole 0.18.0-beta3

Dato: 22. september 2026

## Bygg og identitet

- `testDebugUnitTest`, `lintDebug` og `assembleRelease`: bestått i WSL-reservebygg med
  inkrementell KSP av, etter at Windows-Gradle sin loopback-prosess feila.
- Pakke: `app.reelstack`; versjon: `0.18.0-beta3` (versionCode 123); minSdk 26.
- Signeringssertifikat SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK: `Spole-v0.18.0-beta3.apk`, 11 300 046 byte, SHA-256
  `3cbb2982955727907d51706e2a79a5fd769662b80fdf022143e6ab303d3aa222`.
- GitHub-releasen er publisert som prerelease med nøyaktig éin APK, SHA256SUMS, R8-mapping,
  kjeldecommit og FFmpeg-kjelde-/relenkingsarkiv.

## Teststatus

- Det målretta isolerte regresjonsutvalet køyrde 86/86 grønt på `emulator-5562` før
  releasegjennomgangen.
- TV-spesifikke Compose-testar blir no avgrensa til faktisk TV-mål; dei blir ikkje late-køyrde på
  telefonemulatoren. Ekte TV-profilar blir aldri brukt til instrumentering.

Ekte kontoar, tenesteadresser og skjermbilete er ikkje ein del av Git eller release-assetane.
