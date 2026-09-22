# Verifisering: Spole 0.17.0-beta22

18. september 2026. Versjonskode 101. Publisering på uttrykkeleg førespurnad.

## Omfang

Avgrensa ompakking ved behaldarfeil, sesongnavigering og kompakt kontovisning,
bibliotekfilter, endringslogg og felles fokusgeometri. Sjå
`CONTAINER_RECOVERY_2026-09-18.md` og `TV_CONTROLS_2026-09-18.md`.
Native FFmpeg-filer og lisensar er uendra frå beta21.

Førekontroll før versjonsløft: 636 JVM-testar, 32 UI-testar og 8
avspelingstestar bestod. Lint hadde 0 feil og 93 åtvaringar. Faktiske resultat
frå release-bygget, signaturkontroll og GitHub-oppdatering blir førte nedanfor.

Fysiske Pixel/Shield, original Kiki-fil og ekte mediekontoar er ikkje
verifiserte gjennom dei syntetiske testane. Ingen review-data skal slettast.

## Avgrensing ved review

Oppstart av den eksisterande telefonprofilen med `Start-SpoleEmulators.ps1
-Device Phone -NoWindows` feila fordi den delte ADB-porten var oppteken
(`Address already in use`). Ingen prosessar vart drepne og ingen profildata
endra. Berre isolerte 5562 var tilgjengeleg. Produksjonsappen der er bevart
på beta21 for den ekte oppdateringsflyten etter publisering; han har demodata,
ikkje ekte kontoar. Førehandsinstallasjon på ein ekte review-konto kunne derfor
ikkje gjennomførast.

## Endeleg bygg og artefakt

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  BUILD SUCCESSFUL, avslutta med kode 0 etter 10 minutt og 53 sekund.
- JVM: 636/636, ingen feil eller hoppa-over testar. Lint: 0 feil, 93 åtvaringar.
- Pakke `app.reelstack`, versjon `0.17.0-beta22`, kode 101, minSdk 26,
  targetSdk 36, ikkje debuggable. Universal: arm64-v8a, armeabi-v7a, x86, x86_64.
- APK: 11 064 842 byte. SHA-256:
  `b132a34a414f9cba269ce20cd15a899575d2bb7a2ddb9a46b92754c9de34c7fd`.
- Signatur stadfesta av apksigner:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
  `zipalign -c -P 16 4` bestod.
- Alle åtte native-biblioteka er byte-identiske med beta21. Apache- og
  LGPL-lisensane ligg i APK-en. Tilsvarande kjelde-/relenkingsarkiv er bevart:
  `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`.
- APK og den tilhøyrande R8-mappinga er arkiverte i den ignorerte release-mappa.

## Android-testar på endeleg debug-bygg

Telefonkøyring: 61 testar, 60 bestått og 1 feil (375,608 s). Alle 23 UI-testane
og 37 av 38 avspelingstestar bestod, inkludert begge nye behaldartestane.
`miniPlayerKeepsVideoRunningAndReturnsWithoutRestarting` fekk tidsavbrot ved
retur frå picture-in-picture til fullskjerm (linje 469). Separat omkøyring
gav same feil etter 24,497 s. Dette er ikkje meldt som bestått eller avskrive
som tilfeldig; minispelarretur står som uavklart avgrensing i release-notata.
Ingen kjelde vart endra for å skjule feilen eller omgå testen.

TV-kontrollar og fokus: 9/9 bestått (21,543 s) ved 960 × 540 dp på 5562.
Opphavleg oppløysing/tettleik vart gjenoppretta etter køyringa. Totalt 69/70
ulike Android-testar bestod; den separate omkøyringa av minispelar feila òg.

## Publisering og oppdatering

- Kjeldecommit `5d6c4acbd997f8c560fa581f8dd1b9200d7ea7f0`; annotert tag
  `v0.17.0-beta22`, pusha atomisk saman med `main`.
- GitHub-kladden kontrollert: nøyaktig éin universal-APK og fire støttefiler, alle
  `uploaded`. APK-storleik/-digest (`sha256:b132a34a414f9cba269ce20cd15a899575d2bb7a2ddb9a46b92754c9de34c7fd`)
  og FFmpeg-arkivets digest samsvarar med lokale filer.
- Publisert som prerelease, `draft=false`, ikkje stabil latest.
- Offentleg release-liste utan Authorization inneheld beta22. APK lasta ned anonymt
  til ei separat kontrollfil har identisk SHA-256 med produksjonsbygget.
- Beta21 på isolert 5562 fann beta22 gjennom «Sjekk no», lasta ned APK-en og fullførte fil-/identitetskontrollen.
- Første trykk på Installer henta opp att den eksisterande Android-installatøroppgåva frå førre installasjon. Installatørprosessen vart stoppa på 5562 utan datatap; nytt trykk opna rett oppdateringsdialog («Do you want to update this app?»).
- Google Play Protect bad om skanning, som fullførte med «This app looks safe». Installasjonen vart deretter godkjend i systemdialogen.
- **Oppdatering gjennom appen fullført frå beta21 til beta22**, utan `adb install` av produksjonspakken. Android stadfestar `versionCode=101`, `versionName=0.17.0-beta22`.
- Appen opna att med nynorsk og dei same demodataa (Maya/Severance) bevarte. Ekte kontoar, fysisk Pixel/Shield og sovande mediediskar er ikkje testa.

