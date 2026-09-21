# Spole 0.17.0-beta41 – verifikasjon

Dato: 2026-09-21

## Resultat

- Versjon: `0.17.0-beta41`, versionCode `120`
- Release-commit: `0822a5d6333bb20fb5a29ba81e1b0cd789d11e80`
- GitHub-release: https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta41
- APK: https://github.com/oyvhov/spole-android/releases/download/v0.17.0-beta41/Spole-v0.17.0-beta41.apk
- APK-storleik: `11,234,170` byte
- APK SHA-256: `c0cac632484a92339360c0b484d6dc0389ef319acc0618fe08bc3ff0873cf524`
- GitHub APK-digest: `sha256:c0cac632484a92339360c0b484d6dc0389ef319acc0618fe08bc3ff0873cf524`
- APK-signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`
- Offentleg GitHub-nedlasting kontrollert: hash og storleik stemmer med lokal APK.
- Universal APK: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`
- FFmpeg-arkiv SHA-256: `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`

## Endringar

- Mobil/nettbrett-barnemodus får trygg statuslinjeavstand i toppheaderen.
- Bibliotekskort på barnemodus-framsida opnar eigne bibliotekssider.
- Compose-test dekkjer at bibliotekskortet kallar opningscallbacken.

## Test og bygg

- `testDebugUnitTest`: 702 testar, 0 feil, 0 errors, 0 skippa.
- `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease`: bestått.
- Lint: 0 feil og 134 warnings, alle eksisterande prosjekt-/API-warningar.
- APK-en er verifisert med pakken `app.reelstack`, versionCode `120` og versionName
  `0.17.0-beta41`.
- Signert APK installert med `adb install -r` på `emulator-5560`; installasjonen bevarte
  pakken og dataområdet `/data/user/0/app.reelstack`.
- Full instrumenteringstest på isolert emulator er ikkje køyrt i denne release-runden.
- Oppdateringsflyt gjennom Spole er ikkje køyrt; review-emulatoren stod på førstegongsoppsett,
  så ingen konto- eller bibliotekdata vart nullstilt eller bytta ut.
