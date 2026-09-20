# Verifisering: Spole 0.17.0-beta36

20. september 2026. Versjonskode 115. Publisert som testutgåve på uttrykkeleg førespurnad.

## Omfang

- Breie «Hald fram»-kort brukar hero-/thumbnail-kunst først og fyller kortet utan eit lite,
  sentrert bilete.
- Ulike bilethøve i breie resume-kort blir no croppa i staden for å bli skalerte ned til eit
  lite midtstilt bilete.
- Innstillingar > Om appen har no «Tøm biletbuffer», som tømmer Coil sitt minne- og disk-cache
  utan å logge ut eller slette mediedata.

## Endeleg bygg og artefakt

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  `BUILD SUCCESSFUL`, avslutta med kode 0.
- JVM-testar: 700/700 bestått, 0 feil, 0 hoppa over.
- Lint: 0 feil, 125 åtvaringar.
- Pakke `app.reelstack`, versjon `0.17.0-beta36`, kode 115, minSdk 26, targetSdk 36,
  ikkje debuggable. Universal: arm64-v8a, armeabi-v7a, x86, x86_64.
- APK: 11 205 770 byte. SHA-256:
  `4085298d3353445e50eca147078e33181350dc05afa85b8c8a2292bb54261cf6`.
- Signatur SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- FFmpeg-kjelde-/relenkingsarkiv SHA-256:
  `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`.

## Android-testar og review

Android-testane vart ikkje køyrde i denne release-runden. Berre `emulator-5564` var tilkopla,
og han er ein review-eining med ekte brukardata som ikkje skal få instrumentering. Isolerte
5562/5566 var ikkje tilgjengelege. Ingen ekte kontoar eller appdata vart sletta eller nullstilt.

APK-en vart installert med `adb install -r` på `emulator-5564` og kontrollert visuelt med ekte
data. «Hald fram»-rada viste store breie bilete som fylte korta, utan dei små sentrerte bileta
som utløyste feilen.

## Publisering

- Commit/tag: `94116efe791f9fc8c5c217965085b0e79fb59b22` / `v0.17.0-beta36`.
- Publisert som prerelease, ikkje draft, med nøyaktig fem artefaktar og éin APK.
- Offentleg nedlasta APK har same SHA-256 som den lokale bygningen.
- Release: https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta36
