# Verifisering — Spole 0.18.0-beta5

Dato: 22. september 2026
Kjelde-commit og publisert tagg: `a288e5a7e63a095b35ade5b366026e628e6e552f`

## Bygg og artefakt

- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og
  `assembleRelease` fullførte med **BUILD SUCCESSFUL**. Det tvinga releasbygget køyrde
  254 oppgåver etter at ein tidlegare, uferdig APK først vart oppdaga og forkasta.
- Einingstestar: **727 køyrde, 0 feila, 0 hoppa over**.
- Lint: **0 feil**. Prosjektet har 147 eksisterande åtvaringar, mellom anna API-/deprecated-
  åtvaringar i avspelingskapabilitetane; ingen ny lint-feil vart akseptert for denne utgåva.
- Produksjons-APK: `app.reelstack`, `0.18.0-beta5` / versionCode `125`, minSdk `26`,
  targetSdk `36`, og ikkje `debuggable`.
- APK: `Spole-v0.18.0-beta5.apk`, 11 353 122 byte, SHA-256
  `83ae667eb659d3a2605f47b2b759cc35061671630a822637f8edf3b2d33d6351`.
- Signatur: sertifikat-SHA-256
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping, `SOURCE_COMMIT.txt`, `SHA256SUMS.txt` og den tilhøyrande FFmpeg-kjelde- og
  relenkingspakken er med i releasen. Det finst nøyaktig éin APK-asset.

## Offline-flyten

- Detaljhandlinga er den einaste startstaden: ho er synleg berre for konkret film/episode/video
  hos Jellyfin eller Emby, og berre i vaksen mobil-/nettbrettmodus. Ho opnar ikkje spelaren.
- Ein serie opnar episodeveljaren. «Neste episode» ligg øvst, og kvar rad sender berre den valde
  episoden vidare; han kan ikkje gjere ein serie om til ei masse-nedlasting.
- `OfflineStoragePolicyTest` verifiserer direkte komplett fil som einaste tillatne kandidatform
  og avviser transkoding, live-TV og feil teneste. `OfflineDownloadsUiTest` verifiserer ferdig
  cache-only-avspeling og at fjerning krev eksplisitt stadfesting.
- TV-en vart oppdatert til beta5 med ekte bibliotekdata og stadfestar at verken
  nedlastingsdestinasjon eller nedlastingshandling er synleg der.
- Review-mobilen viste velkomstflyten etter installasjonen og hadde då ikkje ei lagra teneste å
  bruke til direkte filoverføring. Ingen ny innlogging eller kunstig konto vart laga, og difor
  er ekte serveroverføring på mobil **ikkje markert som gjennomført** i denne rapporten. Dette er
  avgrensinga som står att for ende-til-ende-verifisering med ei innlogga mobilprofil.

## GitHub-oppdatering med ekte TV-profil

1. TV-profilen starta på `0.18.0-beta2` med lagra Jellyfin-bibliotek.
2. **Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no** fann
   `v0.18.0-beta5`, rett storleik og dei publiserte notata.
3. **Last ned oppdatering** fullførte appen sin integritetskontroll og viste
   **Installer oppdatering**.
4. Android sin **Update**-dialog installerte pakken. Etter oppstart var versionCode `125` og
   `0.18.0-beta5` installert; den eksisterande kontoen og bibliotekinnhaldet var framleis til
   stades.
5. Den offentlege GitHub-nedlastinga vart henta på nytt utan autorisasjon. Hashen var identisk
   med byggartefakten og GitHub sin asset-digest.

Release: <https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta5>
