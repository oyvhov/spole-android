# Verifisering: Spole 0.17.0-beta37

20. september 2026. Versjonskode 116. Publisert som testutgåve på uttrykkeleg førespurnad.

## Omfang

- TV-barnemodus beheld den etablerte filmatiske hero-layouten, karusellen og mediaradene.
- Mobil-barnemodus har ein kortare hero utan lang omtale, medan mediarader og avspelingsknappar
  er uendra.
- Temabakgrunnen er tona ned, og mobilheroen går mjukare over i bakgrunnsfargen.
- README-en er oppdatert til siste versjon, slik at GitHub Actions sin release-konsistenssjekk går
  grønt.

## Endeleg bygg og artefakt

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  `BUILD SUCCESSFUL`, avslutta med kode 0.
- JVM-testar: 700/700 bestått, 0 feil, 0 hoppa over.
- Lint: 0 feil, 124 åtvaringar.
- Pakke `app.reelstack`, versjon `0.17.0-beta37`, kode 116, minSdk 26, targetSdk 36,
  ikkje debuggable. Universal: arm64-v8a, armeabi-v7a, x86, x86_64.
- APK: 11 206 338 byte. SHA-256:
  `15a40b79f74e10fc85eeea023907f2adf4b8bbad6d845dd7f281e8eee808c0ca`.
- Signatur SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- FFmpeg-kjelde-/relenkingsarkiv SHA-256:
  `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`.

## Android-testar og review

Android-testane vart ikkje køyrde i denne runden. Den tilkopla `emulator-5564` er review-eining
med ekte brukardata og skal ikkje instrumenterast; isolerte 5562/5566 var ikkje tilgjengelege.
`assembleDebugAndroidTest` bygde testpakken utan feil. Ingen kontoar eller appdata vart sletta
eller nullstilt.

Release-APK-en vart installert med `adb install -r` på `emulator-5564`. TV-barnemodus vart
kontrollert visuelt med ekte data: TV-heroen er bevart, bakgrunnsdekorasjonen er svakare, og
overgangen under heroen er mjukare.

## GitHub Actions og publisering

- GitHub Actions: [Bygg og test #35531871659](https://github.com/oyvhov/spole-android/actions/runs/35531871659)
  bestått; instrumenteringsjobben vart hoppa over av workflowen.
- Commit/tag: `2826171107c60b9a071cd1e685670314d4e5d2b5` / `v0.17.0-beta37`.
- Publisert som prerelease, ikkje draft, med nøyaktig fem artefaktar og éin APK.
- Offentleg nedlasta APK har same SHA-256 som den lokale bygningen.
- Release: https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta37
