# Verifisering – Spole 0.18.0-beta15

Dato: 3. oktober 2026. Pakke `app.reelstack`, versionCode `135`.

## Endringar og omfang

Mobil-Hero vel bakgrunnskunst framfor `Thumb`/`Primary`, med serien som bileteigar
for episodar. Adressa blir bevart i eksisterande metadata-cache. Nettverkskalla
ber alt om bakgrunnsbilete, så det er ikkje nødvendig med ekstra kall per kort.
TV og bibliotek bruker dei eksisterande adressene sine.

Ein innfødd sidekarusell gir sveiping begge vegar, fast toppmeny og vertikal rulling
på framsida. Sideprikkane har 48 dp trykkflate. Automatisk bytte får ein ny pause
etter sveiping og avbryt ikkje sin eigen animasjon. Hero og knappar kan vekse ved
stor skrift. Detaljar og lokal før-release-test er dokumenterte i
[mobilgjennomgangen](MOBILE_HERO_2026-10-03.md).

## Endeleg bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  **BUILD SUCCESSFUL** på 4 minutt 57 sekund.
- JVM: **831/831**, ingen feil eller hoppa over. Ni testar for mobil-Hero, i tillegg
  til parser-, adresse- og cache-testar for rett bakgrunnskunst.
- Lint: **0 feil**, 168 åtvaringar og eitt hint.
- APK: `Spole-v0.18.0-beta15.apk`, **11 453 710 byte**, kode 135, minSdk 26,
  targetSdk 36, fire ABI-ar og ikkje `debuggable`.
- APK SHA-256: `a2a8e1ca98d561154e873c10900701c8e07d66f3fe45d6d8f543aae3f892859a`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping er arkivert. Dei fire FFmpeg-JNI-biblioteka og begge lisensane er
  byteidentiske med beta14. Tilsvarande kjelde-/relenkingsarkiv har SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

## Android-testar

- Endeleg beta15-debug-/test-APK på isolert mobilprofil 5562: **6/6** med normal
  skrift og **1/1** med fysisk systemskrift 2.0. Ingen hoppa over. Testane dekkjer
  begge sveiperetningar, rett detaljhandling, fast toppmeny, sløyfe, prikkval,
  reduserte rørsler, automatisk pause og vertikal rulling.
- Heile Android-suiten er ikkje køyrd. Ingen instrumentering er køyrd på dei lagra
  kontoemulatorane 5560/5564. Ekte mobilkunst frå Jellyfin/Emby er kontrollert med
  parser-, adresse- og cache-testar; den lagra mobilprofilen var allereie i demo.

## Installering og publisering

- TV 5564 er oppdatert frå beta14 til beta15/kode 135 med signert `install -r`.
  Opphavleg installasjonsdato er bevart. Nynorsk, grøn profil, lagra kontoar og
  faktisk bibliotekinnhald er kontrollerte på framsida. Ingen avspeling er starta.
- Mobil 5560 har fått den publiserte beta14-APK-en med `install -r`, med bevarte
  brukardata og opphavleg installasjonsdato. Denne utgåva blir brukt til prøven
  av faktisk GitHub-nedlasting og Android-installasjon etter publisering.

## Publisering

- [GitHub Release](https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta15)
  er publisert som prerelease, ikkje draft eller stabil latest. Taggen peikar på
  kjeldecommit `002d44dafbf9bf21dbca1afe3e28a3d790fc5feb`.
- Alle fem assets er opplasta: éin universal produksjons-APK, R8-mapping,
  `SHA256SUMS.txt`, `SOURCE_COMMIT.txt` og FFmpeg-kjelde-/relenkingsarkiv.
  Storleik og GitHub-digest samsvarar med alle lokale filer.
- Offentleg release-metadata er henta utan Authorization-header. Den offentlege
  APK-en er lasta ned til ei separat kontrollfil; SHA-256 samsvarar med bygget.

## Faktisk oppdateringsprøve

- Mobil 5560: Innstillingar → Notifications and updates → App updates → Check now
  fann beta15. Download update lasta ned og kontrollerte APK-en gjennom appen.
- Install update opna Android. Update, Play Protect-skann og deretter Install
  vart gjennomførte. Play Protect viste «This app looks safe», og Android viste
  «App installed».
- Installert pakke er beta15/kode 135, `lastUpdateTime=2026-10-03 12:32:13`.
  `firstInstallTime=2026-09-15 10:09:51` er uendra. Beta15 vart ikkje installert
  med ADB på mobilen.
- Open frå Android-installatøren opna den nye framsida med mobil-Hero og
  prikkval. Engelsk språk, grøn/Lime-profil og eksisterande demoinnhald er
  bevarte. Mobilprofilen var i demo før oppdateringa; ekte mobilkonto er difor
  ikkje verifisert i denne prøven. Begge dei lagra emulatorane står opne.
