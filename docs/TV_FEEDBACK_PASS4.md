# TV-gjennomgang 4

## Rettar

- Sidebaren får fokus på aktiv side eller aktiv biblioteksnarveg ved inngang.
- Episodar og sesongar bruker den breie detaljtoppen som seriar, med bilete,
  metadata og handlingar samla. TV-omtalen reserverer ikkje 174 dp tomrom.
- Tilbake lukkar OSD også under pause og frå lyd-/tekst-/kvalitetsdialogen.
- Bibliotekverktøy, Oppdag-filter, aktivitetsfilter og sesongval har eksplisitt
  ned-lenkje til første innhaldselement.
- Fokus i framsideheroen rullar heilt til toppen.
- Venstretasten prøver vanleg retningsnavigasjon før sidebaren. Ein plakat med
  ein nabo til venstre skal ikkje opne menyen.
- Neste-episode-kortet får fokus når det kjem fram og beheld det når OSD blir
  skjult. Kortet er meir gjennomsiktig, viser episodens eige bilete når det
  finst, og har framdriftslinje i tillegg til nedteljingsteksten.
- TV-omtalen er avgrensa til to linjer før «Les meir», med mindre skrift og
  toppavstand. Full omtale er framleis tilgjengeleg.
- Medverkande får portrett frå serverens person-ID og bilettag. URL-en er koda,
  utan token, og bilethentinga bruker eksisterande autentisering og mellomlager.
- Filmikonet er eit klappbrett. Tilkopla tenester får små symbol i sidemenyen.
- Bibliotekførespurnaden bruker `GroupItemsIntoCollections=false` i tillegg til
  item-type-filter; samlingar skal framleis opnast gjennom samlingsbiblioteket.

## Designmønster

Same topp for serie, sesong og episode; film behaldar støttande poster. Fokus på
hovudhandlingane viser toppen. Tom omtale skal vere ei kort linje, ikkje ei tom
flate. Tenestesymbol er passive, ikkje nye fokusmål. Eksisterande tema og former
er bevarte. Ingen bakgrunnsanimasjonar er lagde til.

## Forslag til seinare innstillingar

Samanlikna med Wholphin si offisielle innstillingsoversikt (sist merka v1.0.5):
https://github.com/damontecres/Wholphin/wiki/App-Settings

Prioritert for Spole, ikkje implementert i denne runden:

1. Separate spolehopp fram/bak og nokre sekund tilbakespol ved gjenopptaking.
2. Sovetimer / stopp automatisk neste episode etter lang tid utan fjernkontrollbruk.
3. Valfri vising av biletførevising ved spoling og tid før OSD forsvinn.
4. Tekststorleik, kant og bakgrunn for undertekstar, med eiga HDR-tilpassing.
5. Biletfrekvens som følgjer videoen på kompatible TV-ar, med enkel av-knapp.

Wholphin dokumenterer spolehopp, tilbakehopp ved pause, passout protection,
OSD-tid, D-pad seek modes, subtitle style og refresh rate switching. Spole har
allereie neste-episode-tid, automatisk vidareavspeling, meny-/framsideval, tema,
lett TV-modus og kvalitetsval; dei skal ikkje presenterast som manglande nyheiter.

## Verifisering

- Einingstestar: 453/453 bestått, inkludert nye portrett-, episodebilet- og bibliotekførespurnadskontrollar.
- Isolert TV-profil 5566: 27/27 bestått (navigasjon, detaljar, bibliotek og OSD).
  Testen av OSD dekkjer tilbake frå kvalitetsdialog under pause; bibliotektesten
  går ned frå høgre verktøyknapp og krev første film.
- Lint: 0 feil, 31 åtvaringar.
- Signert utviklingsbygg installert med `-r` på TV-profil 5564; kontoar, ekte
  bibliotek og tema bevarte. Ingen instrumentering på innlogga profilar.
- Episoden Alone Australia S3 E9 kontrollert visuelt med skriftstorleik 1.0 og
  2.0. Biletet er no til høgre for samla tittel/handlingar og omtalen har ikkje
  ei fast tom høgd. Skriftstorleik sett tilbake til 1.0 etter kontrollen.
- Retur til sidebaren frå Bibliotek gav fokus på Bibliotek, ikkje Heim.
- Manuell kontroll med ekte favorittar: venstre frå The Chair Company flytta til
  Affeksjonsverdi og lét sidebaren vere lukka.
- Retur frå nedrulla framsiderader til «Sjå meir» viste heile heroen. Eit første
  forsøk kolliderte med automatisk fokusrulling; returen ventar no til neste
  biletramme før han set toppen.
- Kompakt omtale kontrollert på Bear Grylls S1 E5; første episoderad er synleg
  saman med tittel, bilete, handlingar, omtale og sporval.
- Portrett er verifiserte med syntetiske serverdata; faktisk portrettdekning
  avheng av personbileta som finst på serveren. Ikkje alle bibliotek er manuelt
  gjennomgått for samlingar.
- Mobilprofil 5560 starta att med bevarte data etter den isolerte TV-køyringa.
- Endringane blir publiserte som alpha23. Alpha22-artefaktet er uendra.
