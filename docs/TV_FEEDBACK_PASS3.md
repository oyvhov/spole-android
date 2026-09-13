# TV: meny, detaljar og avspeling · 13. september 2026

Arbeid etter alpha21. Dette dokumentet er ikkje ein publiseringsrapport.

## Endringar

- Sidebaren måler kontrollane med fast breidd medan berre utsnittet opnar seg.
  Innhaldet får same breidd før, under og etter opning. Høgre frå menyen går tilbake
  til førre fokus i innhaldet. Framsida bruker minimal rulling for å vise fokus;
  helten sender ikkje lenger eit ekstra konkurrerande rullekrav.
- «Skjul sidemenyen» under TV-innstillingane for meny er av som standard. Når valet
  er på, får innhaldet heile breidda. Venstre frå innhaldskanten opnar menyen over
  sida; høgre eller val av side lukkar han. Valet er lagra på denne eininga.
- Seriesida har ein brei topp med tittel, fakta og avspeling ved sida av biletet,
  etterfølgd av omtale, sesongar og episodar over heile lesebreidda. Ho reserverer ikkje ei
  tom biletkolonne langs heile episodelista. Filmar held den mindre, faste plakaten.
  Retur til hovudhandlingane rullar heilt opp, slik at også tittelen kjem fram.
- Rettar samanlikninga mellom serie-ID og detalj-ID ved val av neste episode:
  «Spel av» kan no bruke den allereie henta neste episoden.
- Bibliotekveljaren på TV har ei rullbar bibliotekliste og eit eige panel for
  snarveg, ikon og rekkjefølgje for det aktuelle biblioteket. Lagre og Avbryt er faste.
  Endringar blir først lagra når brukaren vel Lagre.
- OSD-søkjelinja har inga ytre fokusramme. Ein større kvit prikk viser fokus.
  Spoleikona har større tal og meir plass, og spel-ikonet er ein enkel trekant.
  Clearlogo blir brukt når Jellyfin oppgir eit eige eller arva logo-bilete;
  vanleg tittel er reserve ved manglande bilete eller feil.

## Nye mønster i designsystemet

| Mønster | Tilstand og åtferd | Tilgjenge |
| --- | --- | --- |
| TV-meny som legg seg over sida | Fast innhaldsbreidd; synlege ikon eller heilt skjult som personleg val | Venstre opnar, høgre returnerer; tekstetikett på kvart ikon |
| Serietopp | To felt berre i toppen, full breidd for episodar under | Rullbar tekst; opp til hovudhandlingane viser tittelen att |
| Bibliotekval på TV | Liste med brytarar; detaljval for fokusert bibliotek ved sida av | Eitt fokusmål per bibliotek; eigen rulling i begge panel; fast lagring |
| Fokusert tidslinje | Større prikk og kraftigare spor, inga ramme rundt treffflata | Framleis 48 dp treffflate, framdriftssemantikk og direkte fjernkontrollspoling |

Flatene bruker eksisterande tema, typografi og avrunding. Ingen nye animasjonsløkker,
videoførehandsvisingar eller spekulative nettverkskall er lagde til.

## Idear til ei meir særprega framside

Dette er forslag, ikkje funksjonar som er bygde i denne runden.

1. **Kveldens val:** tre gode val frå eige bibliotek, med ei kort forklaring som
   «Ein episode att av sesongen» eller «Ein film på under 90 minutt». Berre bruk
   forklaringar som kan stadfestast av metadata og visningshistorikk.
2. **Kor mykje tid har du?** Diskrete val for 20, 45 eller 90 minutt som finn noko
   som passar. Bruk attståande tid på ein påbegynt tittel.
3. **Di eiga filmhylle:** la brukaren feste nokre favorittsamlingar på framsida,
   med ei stor signaturflate og Clearlogo framfor mange like plakatrader.
4. **Seriekompass:** ei kompakt oversikt over kvar du er i seriane dine, kva som
   kjem neste og kva som faktisk er klart å sjå. Unngå spoilande episodetekst.
5. **Sesongkveld:** jule- eller Halloween-uttrykket kan få ei frivillig samling
   frå eige bibliotek. Rolig kunst og gode titlar gir stemning utan bakgrunnsvideo.

Prioriter Kveldens val og tidsval først. Dei gjer det enklare å velje noko å sjå,
og kan bruke data appen alt har utan å gjere ein treg TV meir travel.

## Verifisering

- Endeleg bygg: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
  assembleRelease` fullført på 5 minutt 31 sekund. 451/451 einingstestar består.
- 41 TV-kontrollar består på isolert TV-profil 5566: navigasjon (2), detaljar og
  bibliotek (13), spelarkontrollar (10), TV-innstillingar (6), personlege val (5)
  og brei navigasjon (5). Dei siste 13 detalj-/bibliotektestane vart køyrde på
  endeleg kode, inkludert dobbel tekststorleik og bibliotek som kjem asynkront.
- 11/11 kontrollar av brei navigasjon, mobil-/nettbrettinnstillingar og lagra val
  består på isolert mobilprofil 5562. Ein nettbrettest vart først køyrd på TV og
  fann ikkje den mobilspesifikke kontrollen; han består på rett eining.
- Lint: 0 feil, 31 åtvaringar. Éi ny stilåtvaring gjeld at den nye `modifier`-
  parameteren står sist for å bevare eksisterande posisjonelle kall til navigasjonen.
- Signert lokalt bygg installert med `-r` på ekte TV-profil 5564. Visuell kontroll
  med ekte bibliotek av framside, skjult/synleg meny, bibliotekveljar og seriesida.
  Endeleg serieside kontrollert med systemskrift 1.0 og 2.0. Fontstorleik sett
  tilbake til 1.0, skjult meny tilbake til av; bibliotekutkast avbrotne utan lagring.
- Kontoar og appdata bevarte. Instrumentering er berre køyrd på isolerte profilar.
  Den faste mobilprofilen er starta att etter testane.
- Clearlogo-mapping og reserve ved manglande metadata er einingstesta. Ny faktisk
  videoavspeling med logo er ikkje visuelt verifisert i denne runden. Yting på
  brukarens fysiske TV er ikkje målt; geometrien er kontrollert i emulator.
- Loggar og skjermbilete ligg lokalt under `app/build/tv-pass3-*`, utanfor Git.
  `git diff --check` er rein.

Dette er lokale endringar etter alpha21, ikkje ein ny publisert APK. Det lokale
prøvebygget har framleis versjonskode 64 og skal ikkje distribuerast som alpha21.
Neste publisering må få ein ny versjonskode og følgje release-flyten.
