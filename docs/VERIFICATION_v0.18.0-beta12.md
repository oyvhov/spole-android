# Verifisering – Spole 0.18.0-beta12

Dato: 1. oktober 2026. Pakke `app.reelstack`, versionCode `132`.

Utgangspunkt: beta11, med mobilendringane i `MOBILE_DETAILS_2026-10-01.md`.

## Verifisert før release-bygg

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: BUILD SUCCESSFUL.
- **810/810 JVM-testar**, ingen feil. Lint: **0 feil**, 166 åtvaringar og 1 hint.
- Sju nye mobiltestar på Android 34 med innfødd Robolectric-grafikk, 360 × 780 dp:
  avspeling over gestfeltet, rulling/utviding, faktisk skriftstorleik 2.0, innlasting,
  lang filmtittel, manglande innlogging og serie utan avspelbar episode.
- Tre separate opningar er kontrollerte i 65 teikna rammer kvar, med metadata i
  ramme 0, 8 og 36. Ingen oversving, retningsbyte eller endra flatehøgd.
- Førehandsvisingar med syntetisk innhald er kontrollerte visuelt ved skriftstorleik
  1.0 og 2.0. Ingen skjermbilete eller kontodata blir publiserte.

## Release-bygg, Android-testar og artefaktar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  **BUILD SUCCESSFUL** på 12 minutt og 55 sekund. **810/810 JVM-testar** og **0 lint-feil**.
- Versjonsmetadata og omsetjingar: OK, 1426 standardnøklar og éin tillaten nøkkel
  berre i standardsettet.
- APK: `Spole-v0.18.0-beta12.apk`, **11 421 014 byte**, `app.reelstack`, kode 132,
  minSdk 26 og targetSdk 36. Universell for fire ABI-ar og ikkje `debuggable`.
- SHA-256: `f2d9de9966cc022959cef766fcc21254a93cb348a252a4ed63e069555d4eb607`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mappinga er arkivert og inneheld den nye `MobileCinematicDetails`-koden.
  Alle fire FFmpeg-JNI-biblioteka er byteidentiske med beta11. Begge lisensane
  ligg i APK-en, og tilsvarande kjelde-/relenkingsarkiv er lagt ved:
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

## Android-testar

- Den eksisterande isolerte TV-profilen 5566: **16 ulike utvalde testar køyrde**,
  ingen hoppa over. Første køyring gav 14 beståtte og to dialogtidsfristar.
- Dei to dialogtestane vart køyrde om att åleine. Vanleg dialog med sein metadata
  bestod; tastaturtesten nådde ikkje lukkebekreftinga innan 1000 ms. Endeleg er
  **15/16 ulike testar stadfesta**, ikkje ein heilt bestått suite.
- `AdaptiveDialogTest.keyboardKeepsTheCloseActionReachable` gav nøyaktig same
  tidsfristfeil i den lagra beta11-køyringa. Produksjonskoden for lukking er uendra.
  Denne avgrensinga er ikkje retta eller rekna som bestått her.
- Alle 14 TV-kontroll- og detaljtestane bestod, med mellom anna film, serie,
  Jellyfin/Emby-avspeling, høg kontrast og skriftstorleik 2.0.
- Full mobilinstrumentering vart ikkje køyrd: den delte mobiltestprofilen 5562
  er i bruk av ei anna aktiv oppgåve og vart ikkje teken over. Dei sju nye
  mobil-UI-testane i JVM-suiten er beståtte. Ingen instrumentering på 5560/5564.
- Den isolerte TV-testprofilen 5566 vart stengd etter testing, utan å slette
  eller nullstille data. Review-profilane og den delte mobiltestprofilen vart ståande.

## Installering med lagra profilar

- TV 5564: signert `install -r` frå beta11/kode 131 til beta12/kode 132.
  `firstInstallTime` er framleis **2026-09-09 20:50:58**.
- Appen opnar med same nynorske språk og tema og det ekte bibliotekinnhaldet,
  mellom anna Kongen befaler, Bear Grylls og Monsen og nasjonalparkene.
- Ei valfri Android-TV-melding om oppsett av skjermlås vart lukka med «Not now».
  Ingen PIN, konto eller brukardata vart endra eller nullstilt.
- Mobil 5560 vart halden på beta11/kode 131 til prøven gjennom GitHub-oppdatering.
  Profilen har lagra demoinnhald og engelske/grøne visingsval, ikkje innlogga
  mediekontoar. Desse vala og demoinnhaldet er bevarte etter oppdateringa.

## Publisering og oppdatering

- [GitHub Release](https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta12)
  er publisert som prerelease, ikkje draft eller stabil latest.
- Den annoterte taggen `v0.18.0-beta12` peikar på kjeldecommit
  [`eab075ff9c9af45430d0aed99432017d43014a0a`](https://github.com/oyvhov/spole-android/commit/eab075ff9c9af45430d0aed99432017d43014a0a).
  `SOURCE_COMMIT.txt` inneheld same commit. Rapportoppdateringa etter testing
  flyttar ikkje taggen og endrar ikkje APK-en.
- Alle fem assets er ferdig opplasta: éin universell produksjons-APK,
  `SHA256SUMS.txt`, R8-mapping, `SOURCE_COMMIT.txt` og FFmpeg-kjelde-/relenkingsarkiv.
  GitHub sine byte-storleikar og SHA-256-digestar samsvarer med dei lokale filene.
- Den offentlege release-lista er henta utan Authorization-header. Beta12 er
  synleg der med rett APK-digest. APK-en er deretter lasta ned offentleg til ei
  separat kontrollfil; SHA-256 er identisk med den bygde APK-en.
- På mobil 5560 vart beta12 funnen med «Check now» i appoppdateringar, lasta ned
  gjennom appen og godkjend i Android sin oppdateringsdialog. Android melde
  «App installed», og appen vart opna att. Dette var ikkje ei ADB-installering.
- Installert versjon er **0.18.0-beta12 / kode 132**. `firstInstallTime` er
  framleis **2026-09-15 10:09:51**, medan `lastUpdateTime` er
  **2026-10-01 08:17:20**. Ingen ny løyvedialog for ukjende kjelder var nødvendig.
- Den nye mobile detaljvisinga vart opna på den oppdaterte produksjonsappen:
  kunst i full breidd, sentrert tittel og metadata, mørk overgang og ingen
  generisk «Detaljar»-overskrift. Lukkeknappen gav normal retur til framsida.
- Mobilprofilen er i demo utan innlogga medieteneste og viser derfor ikkje eit
  avspelingsfelt. Den faste avspelingsknappen og trygg plassering over gestfeltet
  er verifiserte med dei sju syntetiske mobil-UI-testane; reell innlogging og
  avspeling frå denne mobilprofilen er ikkje testa.
