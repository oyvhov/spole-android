# Verifikasjon · Spole 0.17.0-beta30

## Bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  BUILD SUCCESSFUL på versjon 0.17.0-beta30 / 109.
- 669 JVM-/Robolectric-testar, ingen feil. Inkluderer native rendering av foreldreval,
  kontosnarveg og menytilpassing, ståande mobil og liggjande mobil med skriftstorleik 2,0.
- 14 ulike Android-testar bestått på isolerte profilar: sju på mobil 5562
  (MobileSettingsTest, CompactNavigationTest), sju på TV 5566
  (WideNavigationSettingsTest og menytesten i TvSettingsRedesignTest).
- Første TV-runde: 6/7. Den gamle nettbretttesten venta på ein fjerna ekspanderknapp
  og arva TV-modus frå testmaskina. Testen er oppdatert til dagens direkte kategori
  og eksplisitt berøringsmodus; målretta omkøyring bestod.
- Første mobilrunde: 6/7. Android sin system-ANR-dialog tok vindaugsfokus og blokkerte
  Espresso sin tilbakeknapp. Etter lukking av systemdialogen bestod same test utan
  kodeendringar. Ingen testar hoppa over.
- Lint: 0 feil, 109 åtvaringar og eitt hint. Ingen påstand om at åtvaringane er rydda.
- `git diff --check`: utan feil.

## Artefakt

- Pakke `app.reelstack`, versjon `0.17.0-beta30`, versionCode `109`.
- MinSdk 26, targetSdk 36, ikkje debuggable.
- APK: `Spole-v0.17.0-beta30.apk`, 11 184 998 byte.
- SHA-256: `96d2dc4efeaa579e3c78f6b15911ab0c46c6171149ec2ce35c82f804602a75ee`.
- Signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK og nøyaktig R8-mapping arkiverte i `app/build/release-v0.17.0-beta30/`.
- Uendra native FFmpeg-modul: same tilsvarande kjelde-/relenkingsarkiv som beta23,
  SHA-256 `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`.

## Reelle data og grenser

Signert `install -r` på review-TV 5564 bevarte aktiv barneprofil, Verdsrom-tema,
bibliotek og hald-fram-tittel. Versjon 109 er kontrollert etter omstart. Barnemodus
vart dessutan manuelt kontrollert med temabyte, bibliotekfilter, episodetitlar og
episodebilete, ekte filmavspeling og skriftstorleik 2,0 før versjonsauken.

Review-mobil 5560 står på velkomstskjermen og er halden på beta29. Ingen ekte
mobilkonto eller passordgjenoppretting er testa. Automatisk instrumentering er
berre køyrd på 5562/5566. Ingen ekte kontoar er sletta eller omgått.

## Publisering og oppdatering

- Publisert som prerelease, `draft=false`, utan å flytte stabil latest.
- Release-taggen peikar på `bb36fa65dccd05191378bcc411d0f5a4eae838d9`.
- Offentleg GitHub-liste utan Authorization-header viser beta30. Nøyaktig éin APK;
  GitHub-digest og offentleg nedlasta kontrollfil samsvarer med SHA-256 ovanfor.
- Isolert TV 5566 med produksjons-beta29 og demo: «Check now» fann beta30;
  «Download update» fullførte appen sin kontroll og viste «Install update».
  Android sitt kjeldeløyve og oppdateringsdialog vart godkjende i grensesnittet.
  Etter installasjon viser pakkekontrollen `versionCode=109`, `0.17.0-beta30`.
- Dette er testa nedlasting/installasjon gjennom appen. Demo-eininga dokumenterer
  ikkje ekte kontobevaring; den er separat kontrollert på review-TV ovanfor.
- Release: https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta30
