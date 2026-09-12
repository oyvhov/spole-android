# Mobilinnstillingar og sesongtema

Oppdatert 13. september 2026, som lokale endringar etter TV-gjennomgangen.

## Innstillingar på mobil

Den lange sida er erstatta av ei oversikt med sju kategoriar: Utsjånad, Heimskjerm, Meny, Avspeling, Tenestene dine, Varsel og oppdatering, og Om appen. Ei kategori opnar ei eiga side. Tilbake-knappen og Android sin tilbakefunksjon går tilbake til oversikta. Rulleposisjonen blir bevart for kvar kategori.

Utsjånad opnar temavala direkte. Ho har ikkje lenger eit ekstra panel som må utvidast først. «Jul og Halloween» står i inngangen, og vala ligg under **Utsjånad → Sesongtema**. Språk er samla på same side. Heimskjerm og meny har eigne sider, og avspeling har både hald-fram-valet og tidsvala for neste episode.

Val, brytarar og handlingar brukar dei same felles innstillingsradene. Sekundærknappar i mobilinnstillingane har same diskrete ramme og hjørne som på TV. Språkvalet brukar òg ei vanleg valrad. Lengre titlar og store skriftstorleikar får verdien under tittelen, slik at radene kan vekse utan fast teksthøgd.

Breie nettbrett held på kategoriane i sida. TV held på sitt eige oppsett og fokusnavigasjon. Konto-, teneste- og bibliotekhandlingar brukar dei eksisterande callbackane og tilgangsreglane.

## Jul og Halloween

Begge temaa fanst frå før som kombinasjonar av bakgrunn og aksentfarge. Dei har no fått meir synleg særpreg:

- **Jul:** nisselue på Spole-logoen, granbar, varme julelys, stjerner og snø over toppfeltet. Mobilframsida har ei eiga julehelsing med illustrasjon.
- **Halloween:** spøkelse, spindelvev, hengande edderkoppar og glør over toppfeltet. Logoen får eit lite spøkelse, og mobilframsida har ei illustrert Halloween-helsing.
- Utsjånad viser same sesongillustrasjon som førehandsvising. Fargar kan framleis justerast uavhengig.
- **Sesongpynt** slår av dekorasjonane, inkludert lua og spøkelset på logoen. Redusert animasjon og Lett TV-modus bevarer statisk pynt, men stoppar partiklar og rørsler.

Pynten er vektorteikning inne i appen. Ho hentar ingen nye bilete, krev ingen ekstern teneste og har ingen trykkflater. Logoen held same layoutstorleik med og utan pynt. Animasjonen over toppfeltet har avgrensa oppdateringsfrekvens; statiske banner og logopynt køyrer ingen animasjonsklokke.

Endringane er lokale og er ikkje publiserte som ein ny app-release. Skjermbilete med ekte kontoar skal berre liggje i den Git-ignorerte byggmappa.

## Verifisering

- Endeleg produksjonsbygg, debug-bygg og Android-testpakke bygde utan feil.
- 448 einingstestar bestått. Android lint: ingen feil, 29 åtvaringar.
- 26 ulike Android-testar bestått: mobilinnstillingar, UI-konsistens, brei navigasjon/nettbrett, TV-innstillingar, personalisering og tre innstillings-/kontoflytar. Første køyring hadde éin utdatert forventning om den gamle mobilutvidaren; testen vart oppdatert til kategoriflyten og heile den testklassa bestod på nytt.
- Mobilinnstillingane, begge sesongtema og skriftstorleik 2.0 er kontrollerte på emulator. Testane kontrollerer mellom anna kategorinavigasjon, lagring av sesongval, avslått pynt og tidsval for neste episode.
- Signert APK er installert med oppdatering over eksisterande app på dei innlogga mobil- og TV-emulatorane. Ingen kontoar eller appdata er sletta.
- APK SHA-256: `bdbeb864be5834e6c7ae8c7b6b0285c9b5181477fad70dfd1f28a128ad323929`.

Yting på ein fysisk, svak TV er ikkje målt. Lett TV-modus brukar statisk sesongpynt, og logo/banner har ingen eigen animasjonsklokke. Den eksisterande botnmenyen på mobil kan framleis bryte lange etikettar ved dobbel skriftstorleik; dei nye innstillingsradene veks med teksten.
