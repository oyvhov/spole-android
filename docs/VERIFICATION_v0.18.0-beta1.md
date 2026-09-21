# Spole 0.18.0-beta1 – verifikasjon

Dato: 2026-09-21

## Resultat

- Versjon: `0.18.0-beta1`, versionCode `121`
- Release-commit: [`a794d9d61b905c8d139cfc2f452229c41d70c153`](https://github.com/oyvhov/spole-android/commit/a794d9d61b905c8d139cfc2f452229c41d70c153)
- GitHub-release: https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta1
- APK: https://github.com/oyvhov/spole-android/releases/download/v0.18.0-beta1/Spole-v0.18.0-beta1.apk
- APK-storleik: `11 236 202` byte
- APK SHA256: `0237f6dd6c722ad9283d08d4d7a64b6769ea6fa12c413da332dc0c6dca9406c7`
- Offentleg GitHub-download har same SHA256 som lokal APK.
- Release-status: publisert GitHub prerelease, ikkje draft; fem vedlegg, eitt APK-vedlegg.
- APK-signatur skal vere den eksisterande Spole-signaturen:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`
- APK-en er kontrollert som `app.reelstack`, universal for `arm64-v8a`, `armeabi-v7a`, `x86` og `x86_64`.
- FFmpeg-arkiv SHA256: `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`

## Endringar

- Biblioteknamn under mobil-/nettbrettkort er standard for nye og eldre barneprofilar.
- Bibliotekssida sorterer etter lagt til, utgitt og vurdering.
- Film-/serie-filtera er fjerna frå barnemodus; statusvala er Alle, Ikkje sett og Favorittar.
- Dato for lagt til blir lesen frå tenaren og lagra i mediadata.

## Test og bygg

- `testDebugUnitTest`: 704 testar, 0 feil, 0 errors, 0 skippa.
- `lintDebug`: 0 errors, 136 warnings (eksisterande lint-varsel).
- Full releasekommando med debug-test, debug-APK, test-APK, lint og release-APK: bestått.
- APK-en blei installert med `adb install -r` på `emulator-5560`; `versionCode=121`, `versionName=0.18.0-beta1` og eksisterande `dataDir=/data/user/0/app.reelstack` blei bevart.
- Instrumentation-test og in-app update-flow blei ikkje køyrde i denne verifikasjonen; review-emulatoren stod på onboarding og gav ikkje eit innlogga bibliotek å teste mot.
