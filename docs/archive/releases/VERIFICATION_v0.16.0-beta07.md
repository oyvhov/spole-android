# Verifisering: 0.16.0-beta07

Versjonskode 74, pakke `app.reelstack`. Same eksisterande signeringssertifikat.

## Release-bygg

- `./gradlew.bat testDebugUnitTest assembleRelease` køyrd (PASS).
- `./gradlew.bat lintRelease` køyrd (PASS).
- `assembleRelease` produserte `Spole-v0.16.0-beta07.apk`.
- Release-katalog `app/build/release-v0.16.0-beta07/` inneheld APK, `mapping.txt`, `SHA256SUMS.txt` og `SOURCE_COMMIT.txt`.

## Appikon-fiks

- `spole_launcher_foreground.xml` er tilbakeført til varianten utan teksten, slik at launcherikonet berre viser marken.
- `app/build.gradle.kts` oppdatert til `versionCode=74`, `versionName="0.16.0-beta07"`.

## APK-verifikasjon

- SHA-256: `b18131684729f69a822c0f8542f600336720676e2d71c633f8803bbcd5f047d8`
- Pakkeidentitet frå `aapt dump badging`: `app.reelstack`, `versionCode='74'`, `versionName='0.16.0-beta07'`, `minSdk='26'`, `targetSdk='36'`.
- Signert med oppgåva sitt faste sertifikat (`36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`).
