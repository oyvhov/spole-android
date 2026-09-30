# Verifisering – Spole 0.18.0-beta11

Dato: 1. oktober 2026. Pakke `app.reelstack`, versionCode `131`.

Utgangspunkt: beta10 med endringane dokumenterte i `TV_OSD_HERO_2026-09-30.md`
og teknisk medieinfo/innstillingsplassering frå `SETTINGS_PAGES_2026-09-29.md`.

## Bygg og artefakt

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  **BUILD SUCCESSFUL** på 9 minutt og 22 sekund. **803/803 einingstestar**, ingen feil eller hoppa over.
- Etter oppdatering av ei eldre test-fixture og ny test for teknisk medieinfo:
  `assembleDebugAndroidTest assembleRelease` fullførte på 2 minutt og 50 sekund.
  Produksjonskoden var uendra; release-APK-en var oppdatert og kontrollert av Gradle.
- Endeleg `assembleDebugAndroidTest lintDebug assembleRelease` etter retting av
  test-fixtures: **BUILD SUCCESSFUL** på 3 minutt og 5 sekund. APK-hashen er uendra.
- Lint: **0 feil**, 161 åtvaringar og 1 hint. Versjonskontroll og omsetjingskontroll:
  OK, 1426 standardnøklar og éin tillaten nøkkel berre i standardsettet.
- APK: `Spole-v0.18.0-beta11.apk`, **11 421 082 byte**, `app.reelstack`, kode 131,
  minSdk 26, targetSdk 36. Universell for fire ABI-ar og ikkje `debuggable`.
- SHA-256: `9c75b10d2db52900ef8e3a1961638a37a0d0d57264018ea080fd8a27a31651a7`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mappinga er arkivert. Alle fire FFmpeg-JNI-biblioteka er byteidentiske med
  beta10, og begge lisensane ligg i APK-en. Den tilsvarande kjelde-/relenkingspakken
  er uendra: `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

## Review med lagra profilar

- TV 5564: signert `install -r` frå 130 til 131. `firstInstallTime` er uendra
  (2026-09-09 20:50:58). Same profil, ekte bibliotekinnhald, språk og visingsval.
- På framsida får heile første rad med titlar og episodetekst plass. Kjelde står berre
  til høgre i radoverskrifta og ikkje i heroen. Rotasjon er observert med det lagra
  valet «Rolege overgangar» på, mellom anna frå Kongen befaler til Slow West.
- Ekte episode med video og undertekst er opna. OSD viser klokke øvst til høgre,
  tid att under tidslinja og ingen avspelingsmåte-linje som standard.
- Mobil 5560 vart halden på 130/beta10 til oppdateringsprøven gjennom appen.
  Profilen har lagra demoinnhald, ikkje innlogga medietenester. System UI viste
  ein ANR etter den trege kaldstarten; lagra appdata er bevarte.
- Ingen instrumentering er køyrd på profilane 5560/5564. Skjermbilete er berre lagra
  lokalt i ignorert byggmappe.

## Android-testar og oppdatering

- TV 5566, fem klassar: 65 ulike testar. Første køyring gav 61 beståtte og fire feil.
  Tre test-fixtures var utdaterte: mobilens avspelingsknapp var brukt på TV, minuttformatet
  var endra, og vurderinga skulle kome frå `tmdbRating` (0–100) med TMDB-formatet, ikkje
  frå eit gamalt stjerne-faktafelt. Teknisk medieinfo er slått på eksplisitt i testen som
  kontrollerer vising. Ny test kontrollerer standard av og reaktivt av/på ved skrift 2.0.
- Endeleg: **64/65 ulike TV-testar bestod**, ingen hoppa over. TV-klassen gav 34/36;
  den siste korrigerte vurderings-fixturen og medieinfo-testen vart køyrde om att:
  **OK (2 tests)**. Dermed er 35/36 i TV-klassen stadfesta. Dei andre klassane gav
  29/29 beståtte: TV-kontrollregresjon (6), TV-avspeling (12), TV-innstillingar (6) og utval (5).
- Attståande feil: `libraryEditorGivesARemoteItsFirstFocus` får ikkje første fokus på
  brytaren, òg når testen køyrer åleine med tastaturmodus. Bibliotekeditorens produksjonskode
  er uendra frå beta10. Dette er ei avgrensing i den breiare kontrollen, ikkje eit bestått resultat.
- Mobil 5562: forsøket på heile pakken vart avbrote etter 20 resultat (19 beståtte,
  éin feil i `AdaptiveDialogTest`). Ei anna app/testoppgåve brukte same isolerte profil
  samtidig; eit framandt appvindauge vart observert under vår instrumentering.
  Desse resultata blir ikkje rekna som ein full, uavhengig mobilregresjonskontroll.
  Ingen data eller prosessar frå den andre oppgåva vart stoppa eller nullstilte.

## Publisering

- Release-taggen `v0.18.0-beta11` peikar på `b8ab038d409d14592a5f856e9f0e98b43dda24e8`.
- Publisert prerelease, ikkje draft eller stabil latest:
  <https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta11>.
- Alle fem assets er kontrollerte før publisering: éin universal-APK, SHA256SUMS,
  R8-mapping, SOURCE_COMMIT og FFmpeg-kjeldepakken. Storleik, uploaded-status og
  GitHub-digest samsvarer med dei lokale filene.
- Den offentlege release-lista utan autorisasjon inneheld beta11 med rett APK-digest.
  APK-en er lasta ned frå den offentlege lenkja og har same SHA-256 som bygget.
- Mobil 5560: **App updates → Check now → Download update → Install update → Android
  Update → App installed → Open** er fullført gjennom appen. Ingen ADB-installasjon
  vart brukt på denne profilen. Android bad om «Allow from this source» for Spole;
  dette vart gitt. Play Protect viste «App scan recommended» fordi APK-en var ny,
  og det vanlege valet «Install without scanning» fullførte installasjonen.
- Installert versjon gjekk frå 130/beta10 til 131/beta11. `firstInstallTime` er uendra
  (2026-09-15 10:09:51). Appen opnar med same engelske språk, mørke/grøne tema,
  demoinnhald og lagra val. Ingen innlogga mediekontoar på mobil vart påstått kontrollerte;
  dei ekte kontoane og innhaldet er kontrollerte på TV.
- Mobilnettet vart validert etter den trege kaldstarten. System UI-dialogen vart lukka
  med «Wait», og normal app- og installasjonsflyt fungerte etterpå.
- Den isolerte TV-testprofilen 5566 er stoppa etter testane; brukardata er bevarte.
