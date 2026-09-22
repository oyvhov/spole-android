# Spole 0.17.0-beta38 – verifikasjon

Dato: 2026-09-20

## Resultat

- Versjon: `0.17.0-beta38`, versionCode `117`
- Commit: `a9ad2b61f21f0b7fe4deca2c4172ebbf7fe99d4b`
- Tag: `v0.17.0-beta38`
- Release: https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta38
- APK: `Spole-v0.17.0-beta38.apk`, 11 208 262 byte
- APK SHA-256: `7d9e6448cbc4c994f7f529d90b3eba40daf023f6ec7f9b9769b05a4ecc651cd7`
- APK-signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`

Den publiserte APK-en vart lasta ned frå GitHub og fekk same SHA-256 som den lokale release-fila. Release-en er offentleg som prerelease.

## Endringar

- Feil i eksterne Jellyfin-undertekster stoppar ikkje lenger videoavspeling. Berre det defekte tekstsporet blir slått av, og videoen held fram frå same posisjon.
- Barnemodus brukar breie 16:9-kort på mobilbibliotek i staden for posterhøgde.
- Tenestekorta på mobil er bygde om slik at konto og status ikkje blir pressa inn i ein smal kolonne.
- Appnamnet i toppteksten kan endrast lokalt under Utsjånad → Profilering.

## Test og bygg

- `testDebugUnitTest`: 701 testar, 0 feil.
- `lintDebug`: 0 feil, 126 åtvaringar og 1 hint. Åtvaringane er eksisterande/deprecated API-varsel.
- Lokal release-bygging: `assembleDebug assembleDebugAndroidTest assembleRelease` passerte.
- APK verifisert som signert, ikkje debuggable, package `app.reelstack`, minSdk `26`.
- GitHub Actions `main`: run `35538208269` – success.
- GitHub Actions `v0.17.0-beta38`: run `35538208413` – success.
- Instrumenteringstestar vart ikkje køyrde i CI-jobben; dei vart hoppa over av workflow-oppsettet.
- Release-APK-en vart installert med `adb install -r` på `emulator-5564` utan å nullstille brukardata. TV-profilen starta frå eksisterande app-instans.
