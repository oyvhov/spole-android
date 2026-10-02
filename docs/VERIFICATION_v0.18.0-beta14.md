# Verifisering – Spole 0.18.0-beta14

Dato: 3. oktober 2026. Pakke `app.reelstack`, versionCode `134`.

## Endringar og testomfang

TV-varselet var eit ikkje-modalt lag over framsida, utan fokusoverføring. På TV eig det no
eit dialogvindauge. «Sjå oppdatering» får startfokus, begge vala har synleg fokusramme,
og venstre/høgre og opp/ned fungerer. «Seinare» og Tilbake lukkar varselet. Mobil beheld
varselet på framsida. Oppdateringsdialogen set fjernkontrollmodus og fokuserer ei tilgjengeleg
handling ved sjekk, nedlasting, avbryting og ferdig installasjonspakke.

Seriesida har 16 dp avstand under logoen, større mellomrom før omtale og handlingar og
eit naturleg veksande toppområde med minimum 52 % av skjermhøgda. Sesongvalet ligg framleis
nærare handlingane enn i beta13. Store tekstar kan rullast utan å miste handlingane.

Hero-/OSD-endringar og rettinga av nesten ferdige episodar er forklarte i
`TV_REFINEMENT_2026-10-02.md`. Det eksisterande mobil-Hero-arbeidet i arbeidsmappa er
gjennomgått og er med i releasen, med tre eigne JVM-/Compose-testar.

## Endeleg bygg og artefaktar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  BUILD SUCCESSFUL på 1 minutt og 48 sekund. 821/821 JVM-testar utan feil eller hoppa over.
- Lint: 0 feil, 168 åtvaringar og eitt hint.
- APK: `Spole-v0.18.0-beta14.apk`, 11 437 326 byte, kode 134, minSdk 26 og targetSdk 36.
  Universal-APK for fire ABI-ar; ikkje `debuggable`.
- SHA-256: `d3248c1ad9b33548c390c6a1e064c6dc7a2ade2f238c26c0d1b6a6d5b732502f`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mappinga er arkivert. Dei fire FFmpeg-JNI-biblioteka og begge lisensane er
  byteidentiske med beta13. Tilsvarande kjelde-/relenkingsarkiv er arkivert med SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

## Android-testar

- Isolert TV-profil 5566: 31/31 beståtte i normalstorleik. Fire oppdateringstestar,
  14 avspelingskontrolltestar, fem journaltestar og åtte Hero-/detaljtestar.
- Separat køyring med systemets `font_scale=2.0`: 5/5 beståtte. Dei fire oppdateringstestane
  og navigasjon på seriesida. Dialogteksten sin faktiske densitet er kontrollert.
- Totalt 36/36 køyringar og 32 ulike testtilfelle, ingen hoppa over. Skjermbilete av varsel,
  oppdateringsdialog, episode, serie og spolemelding er kontrollerte visuelt.
- Heile Android-suiten er ikkje køyrd. Ingen instrumentering er køyrd på dei lagra
  kontoemulatorane 5560/5564. Nettverksregelen for fullføring bruker syntetisk HTTP-transport;
  ingen ekte episodar er merkte som sette som del av testen.

## Installering, publisering og oppdatering

- TV 5564 er oppdatert frå beta13/kode 133 til beta14/kode 134 med signert `install -r`.
  `firstInstallTime` er framleis 2026-09-09 20:50:58; `lastUpdateTime` er 2026-10-03 00:40:36.
- Eksisterande profilar, tema og faktisk bibliotekinnhald er bevarte.
- Den faktiske Monsen S2/E6-sida frå brukarbiletet er opna med ekte metadata og sesongar.
  Logoen har synleg luft under seg, sesongane ligg under handlingane, og heile episodebileta
  er synlege før rulling. Ingen avspeling eller sett-status er endra for denne kontrollen.
- Mobil blir halden på beta13 for prøven gjennom GitHub-oppdatering. Resultatet frå
  publisering og appoppdatering blir ført inn etter kontrollen.
