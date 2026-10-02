# Verifisering – Spole 0.18.0-beta13

Dato: 2. oktober 2026. Pakke `app.reelstack`, versionCode `133`.

Endringane og den første kontrollen er dokumenterte i `UI_REFINEMENT_2026-10-02.md`.

## Endeleg bygg og artefaktar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  BUILD SUCCESSFUL på 3 minutt og 50 sekund. 810/810 JVM-testar, ingen feil eller hoppa over.
- Lint: 0 feil, 167 åtvaringar og eitt hint. Versjonsmetadata og alle 1426 standardnøklar
  for omsetjing er kontrollerte.
- APK: `Spole-v0.18.0-beta13.apk`, 11 420 942 byte, kode 133, minSdk 26 og targetSdk 36.
  Universal-APK for fire ABI-ar, ikkje `debuggable`.
- SHA-256: `2f743ea7ce91f95ca93ae7e9cafb6a1d9750389ed2a5b97a620ed45c8c520046`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mappinga er arkivert. Dei fire FFmpeg-JNI-biblioteka og begge lisensane er
  byteidentiske med beta12. Tilsvarande kjelde-/relenkingsarkiv er lagt ved med SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

## Android-testar

- Isolert mobilprofil 5562: 14/14 beståtte testar i `TabletFeatureTest`, `HomeHeaderTest`
  og `SheetEscapeTest`. Ingen hoppa over. Mellom anna karusell, stabile handlingar,
  profilikon, dobbel tekststorleik og lukking av dialogar.
- Isolert TV-profil 5566: 10 ulike testar køyrde. Dei fire Hero-testane og fem av seks
  `TvControlsRegressionTest` bestod i første køyring. Sesongtesten ved dobbel tekststorleik
  mista Compose-hierarkiet i første køyring; han bestod ved separat omkøyring.
  Endeleg er 10/10 ulike TV-testar stadfesta, ingen hoppa over.
- Full Android-suite er ikkje køyrd. Ingen instrumentering er køyrd på profilane med
  lagra kontoar. Dei isolerte emulatorane er stengde utan å slette brukar- eller appdata.

## Installering med lagra profil

- TV 5564 er oppdatert med signert `install -r` frå beta12/kode 132 til beta13/kode 133.
  `firstInstallTime` er framleis 2026-09-09 20:50:58.
- Eksisterande barnemodus, tema, språk og ekte bibliotekinnhald er bevarte.
  Bibliotekkorta har ingen valhake. Ingen kontoar eller appdata er sletta.
- Mobil 5560 er halden på beta12/kode 132 for prøven gjennom GitHub-oppdatering.
  Mobilprofilen har lagra demo og berre hovudprofil, så barnemodus med ekte mobilkonto
  er ikkje manuelt verifisert i denne releasen.

Offentleg GitHub-oppdatering blir dokumentert etter publisering.
