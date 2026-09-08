# Verifisering: integrert Jellyfin-spelar

8. september 2026. Lokal prøvebygging etter 0.14.2, ikkje ein publisert release.

## Bygg og automatiske testar

Byggkommando: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`,
Windows Gradle med prosjektet sitt lokale TEMP/TMP og Gradle-lager.

- JVM: 252 testar, ingen feil eller hoppa over.
- Android: endeleg heil runde bestod **116/116** på `emulator-5562` (329,888 sekund),
  inkludert testen for framdriftsåtvaring ved tenarbrot og gjenoppretting.
- Lint: 0 feil, 28 åtvaringar. Fem er varsel om nyare Media3-versjon enn den testa 1.10.1;
  dei andre er eksisterande prosjektvarsel. Ingen lint-reglar er slått av for denne endringa.
- Produksjonsbygg bruker R8 og den eksisterande Spole-signeringa. Ingen ny nøkkel eller pakke.

Spelartestane dekker ekte dekoding av syntetisk MP4/HLS med AAC og WebVTT, tekstsporet sine
cues, spoling, pause, rotasjon/reoppretting, bakgrunn, kontobyte, gjenopptakingsval,
sesong/episode-val, nettverksfeil med ny prøve, lyd/kvalitet og pause ved sporbyte. UI-testar
dekker også skriftstorleik 2.0 og faktisk lys tittel på svart flate.

## Ekte konto og signert app

Berre manuell testing på `emulator-5560` (`Spole_Review`). Oppdatering med `install -r`, utan
avinstallering, sletting av kontoar eller instrumentering. Jellyfin-, Emby- og Seerr-kontoane
var framleis tilgjengelege etter oppdateringa.

Kontrollert med ein ekte Jellyfin-film:

- Avspelingsknappen ligg rett under tittel/poster, ikkje nedanfor heile omtalen.
- Faktiske videobilete gjennom Jellyfin si tilpassing/HLS, ikkje berre eit stillbilete.
- Pause via lokal Android MediaSession og vidare avspeling.
- Liggjande fullskjerm med lesbar tidslinje og kontrollar; rotasjon bevarer posisjonen.
- Spoling til 9:19 og stopp. Etter appoppdatering viste spelaren **Hald fram frå 9:19**.
  Valet starta videoen på rett stad og gjekk vidare til 9:27.
- Val av norsk tekst medan videoen var sett på pause bevarte pause.

Den første løysinga starta HLS-økta på nytt ved kvart tekstbyte. Den ekte filmen synte lang
bufring og uventa endring av posisjon, sjølv om kortfilm-testen var grøn. Tekstspor er difor
flytta til lokalt sporval i same medieøkt, med berre valde spor lasta. Ei grense på 30 sekund
gir ein veg vidare dersom videobufringa står fast.

Etter rettinga vart norsk tekst faktisk rendra ved 14:40 i filmen. Deretter vart tekst slått
av og på ved 15:43 i liggjande vising: bilete, posisjon og pause vart bevarte, og norsk tekst
kom tilbake utan ny videostraumsoppstart. Skjermbileta `player-final-text.png` og
`player-final-toggle.png` dokumenterer dette lokalt.

Ein forbigåande feil ved rapportering gav også ei åtvaring som vart ståande etter at tenaren
svara. Ho blir no berre vist for den gjeldande avspelingsøkta og blir fjerna ved neste
vellukka rapport. Dette er dekt av eigen gjenopprettingstest.

Til slutt vart den endelege signerte APK-en installert med `-r`. Testfilmen vart starta frå
byrjinga og tekstvalet sett tilbake til Av. Ei ny opning henta 0:00 utan gjenopptakingsval,
som stadfesta at testposisjonen på 15:43 ikkje vart ståande i Jellyfin. Spelaren vart lukka
att og emulatoren etterlaten på framsida. Ingen andre einingar sine avspelingar vart styrte.

## Avgrensingar

Emulatoren brukar programvaregrafikk under nestla virtualisering. Dette er ikkje ein måling av
flyt, batteribruk eller maskinvaredekoding på telefon. HDR, HEVC-direkte, alle bilettekstformat,
Bluetooth-lyd, mobilnettbyte og timevis med avspeling er ikkje verifiserte her. Tenaren må ha
løyve og kapasitet til omkoding når telefonprofilen krev det.

Automatiske testar bruker berre syntetiske kontoar på den isolerte emulatoren. Ingen reelle
passord eller tokens ligg i testane. Skjermbilete frå manuell kontroll ligg berre lokalt under
`app/build` og skal ikkje publiserast med persondata/bibliotekinnhald.

## Lokal APK

- Fil: `app/build/player-review/Spole-player-review.apk`
- Pakke: `app.reelstack`, minSdk 26, targetSdk 36.
- Versjonsfelt er framleis 0.14.2 / 41. Dette er eit lokalt **anna prøvebygg**, ikkje APK-en frå
  den publiserte 0.14.2-releasen. Neste publisering må få nytt versjonsnummer.
- Storleik: 3 460 917 byte.
- SHA-256: `922579b21bd9753fd8d38e264b5301bf0e411da91211bd29f2dfda9170fcaee6`.
- Signeringssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping: `app/build/player-review/mapping-player-review.txt` (berre for denne APK-en).
- Den signerte APK-en vart installert over eksisterande produksjonsapp på review-emulatoren.
- Ingen GitHub-tag eller release er oppretta i denne runden.
