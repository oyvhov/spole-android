# Spole 0.17.0-beta40 – verifikasjon

Dato: 2026-09-21

## Resultat

- Versjon: `0.17.0-beta40`, versionCode `119`
- Release-commit: `6dae576041e7b7c533460d84a37ae66500e329bf`
- GitHub-release: https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta40
- APK: https://github.com/oyvhov/spole-android/releases/download/v0.17.0-beta40/Spole-v0.17.0-beta40.apk
- APK-storleik: `11,234,170` byte
- APK SHA-256: `29bb55540269b7ba87fa844a77614aa6e14bdf46bb8f9c6f08b3137ca419bb8b`
- APK-signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`
- Offentleg GitHub-nedlasting kontrollert: hash og storleik stemmer med lokal APK.
- FFmpeg-arkiv SHA-256: `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`

## Endringar

- Responsiv og ryddigare barnemodus for mobil og nettbrett.
- Eigne bibliotekssider med filter og korte siste-rader på framsida.
- Valfri PIN for retur til vaksenmodus og val for biblioteksnamn under bilete.
- Lokal avspelingsframdrift ventar til reell avspeling, medan Jellyfin/Emby framleis eig synk-status.

## Test og bygg

- `git diff --check`: bestått.
- `testDebugUnitTest`: 701 testar, 0 feil, 0 errors, 0 skippa.
- `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease`: bestått.
- Lint rapporterte ingen lint-feil.
- APK-en er verifisert med pakken `app.reelstack`, versionCode `119` og versionName
  `0.17.0-beta40`.
- Full instrumenteringstest på isolert emulator og manuell gjennomgang på lagra
  real-data-emulator er ikkje køyrt i denne release-runden.
