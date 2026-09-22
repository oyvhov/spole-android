# Verifisering: Spole 0.17.0-beta21

18. september 2026. Publisering av dei ferdige detaljrettingane etter brukarens førespurnad.

## Omfang

- Felles detaljvising skjuler overflødig bibliotekstatus for Jellyfin og Emby.
- Reine numeriske aldersgrenser 0–21 får lokaliserbar eining. Klassifikasjonskodar og
  manglande metadata blir ikkje omtolka eller funne på.
- Avspelingskode, ventetider og native FFmpeg-bibliotek er uendra frå beta20.
- Tidlegare lokal førekontroll: 630 JVM-testar og fire målretta Android-testar bestod,
  med skriftstorleik 2.0. Sjå `DETAIL_COPY_AND_DISK_STARTUP_2026-09-18.md`.

## Release-kontroll

Bygg, testar og artefaktkontroll er dokumenterte nedanfor. Oppdateringsflyten blir dokumentert
etter publisering i ein eigen rapport-commit utan flytting av taggen.
Review-profilar med ekte kontoar skal ikkje nullstillast eller brukast til instrumentering.

- Standard oppstart av lagra mobil-review-profil vart forsøkt, men stoppa før emulatorstart:
  delt ADB-port var oppteken (`Address already in use`). Ingen ADB-tenar vart drepen og
  ingen review-data vart endra. Signert føroppdatering på ekte konto kan ikkje verifiserast.
- Isolert 5562 er tilgjengeleg. Produksjons-beta20 (kode 99) med nynorsk/demodata er
  bevart for oppdatering gjennom GitHub etter publisering.
- Native-biblioteka er uendra. Kjelde-/relenkingsarkivet er kopiert frå beta20 med same
  SHA-256: `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`.

## Testar på endeleg beta21-kjelde

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  **BUILD SUCCESSFUL**, exit 0, 10 minutt 10 sekund. Lint: **0 feil, 93 åtvaringar**.
- JVM: **630/630**, null failures/errors.
- Android: **4/4**, 8,163 sekund. Debug-APK og test-APK frå beta21-bygget installerte
  med `-r` på isolert 5562. Dei to detaljtestane, stor-tekst-/aldersgrensetesten og
  testen av delvis/ventande/blokkert Seerr-status bestod.
- Dette er ein målretta UI-/metadataregresjon, ikkje ny stadfesting av fysisk
  Pixel-/Shield-avspeling eller diskoppstart. Produksjons-beta20 vart ikkje overskriven
  ved instrumenteringa.

## Endeleg artefakt

- `Spole-v0.17.0-beta21.apk`, **11 064 842 byte**, `app.reelstack`, kode **100**,
  minSdk 26/targetSdk 36. Ikkje debuggable.
- SHA-256: `affd969e4a6c8224940cab76deb42843ea13c29e19ca37560c9aa5e68b830071`.
- `apksigner verify --print-certs`: godkjend, same produksjonssertifikat
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- `zipalign -c -P 16 4`: godkjend. Universal arm64-v8a, armeabi-v7a, x86, x86_64.
- Åtte native `.so`-filer samanlikna med beta20: identisk innhald/hash for alle.
  APK-en inneheld Apache-2.0 og LGPL-2.1 under `assets/licenses/ffmpeg/`.
- APK og akkurat denne R8-mappinga er arkiverte under `app/build/release-v0.17.0-beta21/`.

## Publisering og oppdatering

- Kjeldecommit `9a2c06551e987dc9630d8b321cec466463f61848`; annotert tag
  `v0.17.0-beta21`, pusha atomisk saman med `main`.
- GitHub-kladden kontrollert: nøyaktig éin universal-APK og fire støttefiler, alle
  `uploaded`. APK-storleik/-digest og FFmpeg-arkivets digest samsvarar med lokale filer.
- Publisert som prerelease, `draft=false`, ikkje stabil latest.
- Offentleg release-liste utan Authorization inneheld beta21. APK lasta ned anonymt
  til ei separat kontrollfil har identisk SHA-256 med produksjonsbygget.
- Beta20 fann beta21 gjennom «Sjekk no», lasta ned APK-en og fullførte fil-/identitetskontrollen.
- Første trykk på Installer henta opp att den eksisterande Android-installatøroppgåva
  (`ActivityTaskManager` resultat 2) utan ny dialog. Installatørprosessen på isolert
  5562 vart stoppa utan sletting av data; eit nytt trykk viste rett oppdateringsdialog.
  Dette same problemet var observert i beta20-kontrollen. Testen kravde såleis dette
  miljøinngrepet; ein heilt friksjonsfri oppdatering er ikkje stadfesta.
- Android-godkjenning og Play Protect-skanning fullførte. Skanninga viste «This app looks safe».
  Dette er ikkje ei full tryggleiksrevisjon.
- **Oppdatering gjennom appen fullført frå beta20 til beta21**, utan `adb install` av
  produksjonspakken. Android stadfestar `versionCode=100`, `versionName=0.17.0-beta21`.
- Appen opna att med nynorsk og dei same demodataa (Maya/Severance). Ekte kontoar,
  fysisk Pixel/Shield og sovande mediediskar er ikkje testa.
