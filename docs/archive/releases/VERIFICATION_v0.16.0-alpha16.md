# Verifikasjon - Spole 0.16.0-alpha16

11. september 2026. Pakke `app.reelstack`, versjonskode 59.

## Bygg og testar

- Full Windows-byggje- og testrekkje køyrd med KSP fallback fordi gamle WSL-stiar låg i mellomlageret:
  `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease --no-build-cache --rerun-tasks -Pksp.incremental=false`.
- JVM-testar: 400 køyrde, 400 bestått, 0 feil, 0 hoppa over.
- Lint: 0 feil, 46 åtvaringar.
- Android-instrumentering på isolert `Spole_Instrumentation` / `emulator-5562`: 247 køyrde, 247 bestått.
- Review-profilane med ekte data vart ikkje brukte til instrumentering.

## APK

- APK: `Spole-v0.16.0-alpha16.apk`
- Storleik: 3 948 913 byte
- SHA-256: `701d7b05f88b70d43a05789a327258817ecb66e5802a72875c193aa87d835785`
- Pakke: `app.reelstack`
- Versjon: `0.16.0-alpha16`
- Versjonskode: `59`
- minSdk: 26
- targetSdk: 36
- `application-debuggable` finst ikkje i badging-output.
- Signeringssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`

## Artefaktar

- Universal release-APK arkivert i `app/build/release-v0.16.0-alpha16/Spole-v0.16.0-alpha16.apk`.
- R8-mapping arkivert i `app/build/release-v0.16.0-alpha16/mapping-v0.16.0-alpha16.txt`.
- `SHA256SUMS.txt` arkivert saman med APK-en.
- Source commit-artefakt: `e25483d2faeb2510fc0146209e1d9a5ff38e33c6`.

## GitHub-release

- Publisert som prerelease: <https://github.com/oyvhov/spole-android/releases/tag/v0.16.0-alpha16>
- Offentleg APK-lenke: <https://github.com/oyvhov/spole-android/releases/download/v0.16.0-alpha16/Spole-v0.16.0-alpha16.apk>
- GitHub asset digest: `sha256:701d7b05f88b70d43a05789a327258817ecb66e5802a72875c193aa87d835785`
- Offentleg nedlasta APK vart hasha til same SHA-256: `701d7b05f88b70d43a05789a327258817ecb66e5802a72875c193aa87d835785`.
- GitHub `latest` peikar framleis på stabil `v0.15.1`, ikkje denne alpha-utgåva.

## Merknader

- Den dokumenterte `app/build/test-avds/start-headless.sh` fanst ikkje i denne arbeidsmappa. Den eksisterande isolerte AVD-en `Spole_Instrumentation` vart difor starta direkte frå WSL med `ANDROID_AVD_HOME=/mnt/c/JellyBin/.spole-test-avds` og port 5562.
- Oppdateringsgrunnlaget via publisert GitHub-release er verifisert med offentleg release-API og offentleg APK-nedlasting.
