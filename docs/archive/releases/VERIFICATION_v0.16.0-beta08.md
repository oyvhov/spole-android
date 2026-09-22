# Verifisering: 0.16.0-beta08

Versjonskode 75, pakke `app.reelstack`. Same eksisterande signeringssertifikat.

## Release-bygg

- `./gradlew.bat testDebugUnitTest assembleRelease lintRelease` køyrd (PASS).
- `assembleRelease` produserte `Spole-v0.16.0-beta08.apk`.
- Release-katalog `app/build/release-v0.16.0-beta08/` inneheld APK, `mapping.txt`, `SHA256SUMS.txt` og `SOURCE_COMMIT.txt`.

## Appikon-fiks

- `spole_launcher_foreground.xml` er tilbakeført til varianten med både merke og ord i ikonfelta.
- `app/build.gradle.kts` oppdatert til `versionCode=75`, `versionName="0.16.0-beta08"`.

## APK-verifikasjon

- SHA-256: `50ce00ba60c81e5b75999c33280a45182968973e1f3bf0971a1995d925d9b9c5`
- Pakkeidentitet frå `aapt dump badging`: `app.reelstack`, `versionCode='75'`, `versionName='0.16.0-beta08'`, `minSdk='26'`, `targetSdk='36'`.
- Signert med oppgåva sitt faste sertifikat (`36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`).
