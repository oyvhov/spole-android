# Changelog

## 0.16.0-alpha21 · Ny TV-spelar og betre oversikt

- Ny TV-OSD med ikon, tydeleg spolelinje, fungerande lyd-/tekstnavigasjon og direkte spoling med piltastane.
- Neste episode får eit diskret kort og nedtelling før episoden er slutt. Pause og avbryting er respekterte.
- Mindre omslag og ryddigare filmdetaljar. Oppdag og Aktivitet samlar informasjon inne i omslaget.
- Meir jule- og Halloween-pynt i bakgrunnar og menyar.
- Fiksa favorittfilmar, hovudknappen Bibliotek, fokusrammer og unødvendig vising av eiga avspeling.
- TV-heroen har ikkje oppdateringssymbol eller aksentramme rundt Sjå meir. Den samla rada heiter Sjå vidare.
- Stor skrift på mobil får ein lesbar meny med alle sidene.

## 0.16.0-alpha20 · Stødig TV-navigasjon, ryddige innstillingar og sesongpynt

- Framsida på TV har rolegare fokus og rulling oppover, jamnare kortrader og tydelegare knappar.
- Neste episode kan visast før episoden er slutt. Vel tidspunkt, automatisk avspeling og ventetid under Avspeling.
- Lett TV-modus reduserer animasjonar og biletlaga i toppfeltet.
- Mobilinnstillingane er samla i sju kategoriar med same utforming på val, brytarar og knappar.
- Sesongtema er lettare å finne under Utsjånad. Jul har nisselue på logoen, lys, granbar og snø. Halloween har spøkelse, edderkoppar og spindelvev. Sesongpynt kan slåast av; lettmodus bevarer statisk pynt.


## 0.16.0-alpha19 · Biblioteket som ei side, og ein mobil som ligg rett

- Ein serie opnar tittelsida si i staden for eit rutenett av sesongplakatar. Sida viser sesongane, episodane med eige stillbilete, lengd og framdrift, og «Spel av» peiker på den episoden tenaren seier kjem neste.
- Plassen under mediebiletet er teken i bruk: biletet er større og tek forma til sitt eige innhald, og år, lengd, kvalitet, sjangrar og tagline står under det i staden for å trengje seg inn mellom tittelen og handlingane.
- **Jul** og **Halloween** som sesongar i Utsjånad. Kvar av dei set bakgrunn og aksent i eitt, med snø som fell eller glør som stig over framsidefeltet. Ornamentet kan slåast av, og stoppar av seg sjølv når systemet har skrudd ned animasjonar.
- **Favorittar** er ei eiga rad på Heim. Merket du set frå eit kort eller ei tittelside fører endeleg ein stad — og favorittane du alt hadde i Jellyfin står der frå første opning.
- Spelaren tilbyr **neste episode** når ein episode er ferdig, med tolv sekunds nedteljing du kan avbryte. Han finn han med `adjacentTo`, så det fungerer òg over ein sesongovergang.
- **Hopp over introen**, når tenaren veit kvar introen er. Både Jellyfin 10.10 sine eigne merke og Intro Skipper-tillegget blir spurde.
- Spelarkontrollane ligg innanfor TV-en si trygge sone. Før låg dei heilt mot kanten, der mange apparat klipper.
- Versjonsveljaren på tittelsida startar faktisk den versjonen du vel. Før lista sida begge klippa og starta kva som helst.
- «Spelar no» blir oppdatert når Jellyfin seier frå, ikkje kvart femte sekund. Pollinga står att som tryggingsnett og kjem tilbake med ein gong om tenaren ikkje har ein slik kanal.
- Appen brukar mindre tid på seg sjølv: eitt systemkall per ramme per plakat er borte, heile skjermen blir ikkje bygd opp att kvart femte sekund når ingenting har endra seg, og biletlastaren har fått eigen minne- og diskcache.
- Biblioteket lastar neste side sjølv når du rullar til botnen. Listevisinga viser fakta og opninga av omtalen i staden for å la tre fjerdedelar av rada stå tom.
- Ein sesong med to hundre episodar opnar like raskt som ein med åtte, og ein episode som aldri blei matcha på tenaren viser ikkje lenger filnamnet sitt som tittel.
- «S03 E01» er borte frå spelaren òg.
- **Alle ikon i appen er no Spole sine eigne.** Det var 140 Material-ikon; no er det null, og biblioteket dei kom frå er teke ut av prosjektet.
- **Bibliotek** er ei side om bibliotek, ikkje fire mappefliser på svart. Kvart bibliotek får ei overskrift du kan trykke og ei rad med det nyaste det held, så du ser kva som er der inne før du opnar det.
- Kvar bibliotekrad leier med det du er midt i i akkurat det biblioteket, før det nyaste.
- Fyrste rada på Oppdag og Aktivitet får plass til titlane sine på ein 1080p-skjerm. Fem omslag i staden for fire, og toppen av neste rad er synleg.
- Biblioteksida si overskrift og dei tre knappane står på same linje på TV. Innhaldet startar ei heil rad høgare.
- Ei **episodeside viser sesongen sin**: sesongstripa rullar til rett sesong, rada du er på er merkt, og omtalen har flytta under biletet der spalta stø tom.
- Innstillingar brukar breidda: verdien står til høgre på same linje som valet, med ein fargeprikk ved sida av. Det gjeld menyrekkjefølgja og tenestene dine òg — seks tenester får plass der tre gjorde før. Førehandsvisinga er tre kort på ei hylle i staden for abstrakte klossar.
- «Sesong 6 - Ep 13» står no overalt — helten, Spelar no-kortet og hyllene skreiv det på tre ulike måtar.
- Ei avkorta linje ser avkorta ut. Tretten tekstar i appen kutta midt i eit ord utan teikn på at det var meir.
- Førespurnadshistorikken viser namn, type og dato under kvart omslag. Før stod det fire plakatar utan tekst, fordi overskrifta tok halve skjermen og bilettekstane fall utanfor.
- Appen byggjer mindre opp att seg sjølv. Ei oppdatering som hentar dei same dataa på nytt kostar no ingenting i staden for å teikne kvart kort om att.
- Ein episode utan namn heiter «Episode 1», ikkje «1 · Episode 1». Vurderingsstjerna peikar på vurderinga i staden for på årstalet. Medverkande står ved sida av biletet på TV.
- «Vel bibliotek» seier kva brytaren gjer éin gong øvst i staden for under kvart kort.
- Sjå [omfang og grunngjeving](docs/TV_REDESIGN_2026-09-12.md).

## 0.16.0-alpha18 · Lågare og stabil TV-hero

- Heroen på TV er lågare, slik at den første innhaldsrada får plass i det første skjermbiletet.
- Toppen, tenestenamnet og profilen blir verande synlege når «Sjå meir» får fokus.
- Lufta over heroen og fram til «Hald fram å sjå» er stramma inn på TV. Mobil og stor tekststorleik beheld fleksibel høgd.

## 0.16.0-alpha17 · Lagra kontoar og tryggare Seerr-innlogging

- Ein mellombels feil ved profilkontrollen blir ikkje lenger vist som om Jellyfin-kontoen er sletta. Innstillingane seier no tydeleg at innlogginga framleis er lagra.
- Dersom Seerr Quick Connect ikkje er tilgjengeleg, byter innloggingsarket automatisk til Jellyfin-konto og forklarer kva du skal gjere vidare.
- Oppdateringa er bygd og kontrollert for installasjon over ei eksisterande Spole-utgåve utan sletting av appdata.

## 0.16.0-alpha16 · Bibliotekhyller, sporval og rolegare TV

- Biblioteka har eigne hyller. Opnar du Filmar eller Seriar, står «Hald fram å sjå» og «Neste episode» for nettopp det biblioteket over rutenettet, med framdrift på biletet. Før fanst dei berre på Heim, blanda på tvers av alle biblioteka.
- Hald inne eit kort for å fjerne det frå Hald fram å sjå, gjere det til favoritt eller markere det som sett. Alt blir skrive til Jellyfin, så dei andre klientane dine ser det same.
- Tittelsidene viser lydspor, undertekstar og versjon før du trykkjer på Spel. Valet følgjer med inn i spelaren. Ein film med 43 tekstspor står som éi linje — «Undertekstar  English - SUBRIP  Endre» — og heile lista ligg bak «Endre».
- Tittelsida er stramma inn. Framdrifta ligg inni «Hald fram · 16 min att» i staden for på ei linje for seg med prosent under, sett og favoritt er ein hake og eit hjarte utan tekst, dei sju metadataplatene er blitt éi dempa linje, og episodebiletet har fått plassen som stod tom ved sida av.
- «Marker som sett» og «Legg til i favorittar» står på sjølve tittelsida, ikkje berre bak eit langt trykk. Spole kunne filtrere på begge frå før, men ikkje setje dei.
- Ingen ring rundt heile helten på framsida lenger. På TV er det «Sjå meir»-knappen som tek fokus, så markeringa har storleiken til ein knapp og ikkje til eit heilt bilete.
- Biblioteksnarvegane i menyen kan flyttast opp og ned. Rekkjefølgja blir lagra og brukt slik du la ho.
- Søkefeltet i Oppdag er alltid synleg på TV, men tek ikkje fokus lenger. Det er ein knapp med same form som feltet, og blir feltet — med tastatur — når du vel det.
- «Sjå førespurnadshistorikk» ligg på same linje som aktivitetsfiltera, som same knapp. Ein pil skil han frå filtera: dei endrar sida, han forlèt ho.
- «S19 E09» blir skrive ut: «Sesong 19 - Ep 9», og med episodenamn «Sesong 6 - Ep 11 · Hvilken Side Er Du På? – Del 1». Kortlister, rutenett og detaljsider brukar same setning; helten har plass og skriv «Episode 13» i fullt ord.
- Eit tomt serienamn frå Jellyfin gjorde at eitt kort på hylla stod heilt utan tittel. Blankt er ikkje eit namn lenger.
- Produksjonsåret står ikkje lenger i episodelinja, der det las som om året var episodenamnet.

- Byter du plakat eller klarlogo i Jellyfin, kjem det nye biletet fram i appen. Før var biletadressa den same før og etter, så det gamle biletet vart vist uansett kor mange gonger du oppdaterte.
- Bibliotekfilter og vising ligg no på sida i staden for i ein popup. Å opne filtera på TV sende tastaturet opp over halve dialogen; det skjer ikkje lenger.
- Du kan velje vising, kortstorleik og bilettype (plakat, thumb, banner, logo) per bibliotek, slik Jellyfin har det. Valet blir hugsa for kvart bibliotek for seg.
- Titlar under omslaga kan slåast av for ein tettare vegg av bilete.
- Filtervala slår inn med ein gong. Søket sender når du er ferdig å skrive, ikkje for kvar bokstav.
- Helten på framsida hoppa når eit klarlogo mangla og tittelen tok over. Begge deler får no same reserverte høgd, også ved stor skrift.

- Lint er køyrd for første gong: 0 feil, og åtvaringane er nede frå 58 til 46. Ni ubrukte strengressursar er fjerna, og to unntak er dokumenterte med grunngjeving i koden.
- Heim-funksjonen på nettbrett las skjermhøgda i staden for vindaugshøgda, og fekk difor feil oppsett i delt skjerm. Terskelen ligg no i `WindowLayoutPolicy` saman med dei andre.
- Ny automatisk kontroll av omsetjingar: manglande strengar, ulike formatparameter og ulike mengdeformer mellom nynorsk og engelsk feilar no i testane i staden for ved køyring.

## 0.16.0-alpha15 · Roigare TV-toppfelt

- Felles toppmarg, mindre tittel og diskret «Sjå meir»-knapp.
- Tenestelogo og namn erstattar «Frå biblioteket ditt».
- Mjuk rotasjon mellom opptil tre ulike seriar, utan episoderepetisjonar. Pausar ved fokus, skjult felt, detaljar eller bakgrunn.
- Stabil teksthøgd og handlingsknapp gjennom overgangane, og dempa profilring.

## 0.16.0-alpha14 · Mediekort og påliteleg innlasting

- Mobilradene går heilt til skjermkanten; overskrifter, søk og siste kort har framleis luft.
- TV-fokus fargar ikkje lenger bakgrunnen rundt tittelen. Berre mediebiletet får ramma, med vald avrunding.
- Retta S-konturen i Android TV-banneret.
- Ufullstendig profil-/biblioteklasting får automatisk nytt forsøk på Heim. «Hald fram å sjå» forklarer innlastingsfeil i staden for å forsvinne.
- Sjå [rettingar og årsaker](docs/TV_MEDIA_REFINEMENT_ALPHA14.md).

## 0.16.0-alpha13 · Appoppdateringar, bibliotek og betre TV-utforming

- Automatisk GitHub-sjekk, oppdateringsvarsel, nedlasting og signaturkontroll. Android ber om godkjenning før installasjon.
- Nytt bibliotekval med eigne menyikon, fleire bibliotekfilter og fokusramme rundt kunst i staden for teksten under.
- Førespurnadsknappen er synleg og fokusert tidleg på TV-detaljsida. Ny oppstartsanimasjon medan innhaldet lastar.
- Oppstartsverktøy for TV-/mobilemulatorane med lagra kontoar, og dokumentert release-flyt for framtidige oppdateringar.

- Skrifta har éin skala. Fem nivå som blei brukte utan å vere definerte — mellom dei sidetittelen på Bibliotek — fall før dette tilbake til Material sine Roboto-standardar. Bibliotek og Oppdag står no i same skrift.
- Éin sidetittelstorleik på Heim, Oppdag, Aktivitet og Bibliotek. TV-menyetiketten er ei kolonneoverskrift, ikkje ein sidetittel.
- Seksjonsoverskrifter på Heim har éin rytme. Åtte overskrifter på same nivå hadde tre ulike toppavstandar og tre ulike botnavstandar.
- Bakgrunnsstemningane tek med seg nøytralane sine. Skiljelinjer, kantar og dempa tekst var faste skogsgrå, så MIDNIGHT og PLUM fekk grøne strekar mot blå og lilla flater. FOREST er uendra.
- Lasteskjelett følgjer stemninga og hjørnevalet i staden for å skimre grønt i alle tema.
- Innstillingsrader ligg på ei tydelegare flate. Ingen nye kantlinjer.
- «Spelar no»-widgeten følgjer stemninga og aksenten du valde i Utsjånad.
- Botnlinja klipper ikkje lenger «Innstillingar» ved stor skrift.
- Deaktivert send-knapp følgjer aksentfargen i staden for å vere grøn uansett val.
- Appen kan installerast og finnast på Google TV: TV-startpunkt, TV-banner og valfri berøringsskjerm. TV-testing på fysisk eining står framleis att.
- Jellyfin ser kva eininga heiter — «Pixel 6», ikkje «Android» — så telefon, nettbrett og TV er til å skilje i dashbordet.
- Spole melder støtte for videoavspeling. Mottak av fjernstyringskommandoar er ikkje implementert og blir ikkje annonsert som støtta.
- «Spelar no» på nettbrett og TV er kunstkort som resten av framsida, ikkje grå plater med ulik høgd.
- Alle brytpunkt for vindaugsbreidd er samla i `WindowLayoutPolicy`.

## 0.15.1 · Animert Spole-logo og raskare avspelingskontrollar

- Oppstarten formar den eksisterande Spole-logoen over namnet: to filmruter glir på plass før midtstykket bind dei saman.
- Fast layout, ingen gjentakande spinner og avgrensa venting på nettverket. Android sitt val for animasjonsfart blir respektert.
- Fast tilbakeknapp under video, raskare vising av avspelingskontrollar ved berøring og ingen automatisk skjuling medan ein spolar med tidslinja.
- Android si tilbake-/gestnavigering er tilgjengeleg under avspeling. Tilbake er registrert før innlasting og krev ikkje at kontrollane først blir viste.

## 0.15.0 · Integrert Jellyfin-spelar

- Jellyfin får no klientnamnet «Spole» ved innlogging, bibliotekkall og avspeling, ikkje «HomeReel». Lagra einings-ID og kontoar er uendra.
- Spel filmar og episodar direkte i Spole, med personleg Jellyfin-konto.
- Vel sesong/episode, hald fram frå lagra posisjon, spol, byt lydspor og undertekst eller vel lågare databruk.
- Fullskjerm med diskrete kontrollar, rotasjon, pause i bakgrunnen og feil med prøve-på-nytt.
- Framdrift blir rapportert til Jellyfin. Ingen ny backend eller styring av andre sine avspelingar.
- Sjå `docs/JELLYFIN_PLAYER.md` for avgrensingar og formatstøtte.

## 0.14.2 · Tydelegare sesongflyt

- «Sjå sesongar» skil bibliotekstatus frå ei ny førespurnad. Ingen sesongar er førehandsvalde.
- Komande sesongar får premieredato når kjend; udaterte sesongar blir ikkje framstilte som forseinka.
- Delvis tilgjengelege og alt førespurde sesongar kan følgjast med eit lokalt varsel, utan ny førespurnad.
- Berre Seerr-kontoen er nødvendig. Ingen Sonarr-tilkopling, ny backend eller endring av automatisk henting.
- Roligare sesongark med skeleton til opninga er ferdig, tydeleg status, fast lukking og støtte for stor tekst.

## 0.14.1

- Tydeleg søkeinngang rett under Spole og profilbiletet på Heim.
- Feltet glir over til søket i Oppdag. Tastaturet kjem fram etter overgangen.
- Eit nytt søk frå Heim går til toppen og fjernar gamle filter, men bevarer søketeksten.
- Vanlege fanebesøk opnar ikkje tastaturet automatisk.
- Fem nye Android-testar for navigering, fokus, éi trykkhandling og stor tekst.

## 0.14.0

Ein publiseringsrunde: appen er 69 % mindre, feil som før viste eit statusnummer forklarer no kva
brukaren skal gjere, og ein krasj etterlèt for første gong eit spor det går an å lese.

### Nye funksjonar

- **Lokal krasjlogg.** Stoppar Spole uventa, blir versjon, einingsmodell og stack trace lagra i ei fil på eininga. Tenaradresser og alt som liknar eit tilgangsteikn blir fjerna før fila blir skriven. Ingenting blir sendt nokon stad — Innstillingar får ein «Del feilrapport»-knapp du sjølv trykkjer på, og ein «Slett» ved sida av.
- **Personvernerklæring** i `docs/PRIVACY.md`, med kva som blir lagra kvar, kva som blir kryptert, og dei to adressene utanom dine eigne tenarar som appen kontaktar.
- **Attribusjon i Om appen**: TMDB, og at Spole ikkje er tilknytt eller godkjend av Jellyfin, Emby, Overseerr/Jellyseerr, Radarr eller Sonarr.

### Rettingar

- **Ei omdirigering forklarer seg sjølv.** Ein omvend proxy som sender `http://` vidare til `https://`, eller som legg på ein skråstrek, gav før «Jellyfin svara med status 301» og ingen veg vidare. Appen les no `Location` og seier kva adresse tenaren faktisk svarar på — i tilkoplingstesten, der du står i det feltet du må rette. Omdirigeringar blir framleis aldri følgde automatisk; det kunne sende eit tilgangsteikn til kva vert som helst.
- **Same tittel blir vist éin gong.** Eit søk etter ein serie du har på både Jellyfin og Emby gav to kort med same plakat og ingenting som skilde dei, fordi tenarane gir same verk kvar sin ID. Treffa blir no slegne saman på TMDB-ID, eller på namn, type og år når tenaren ikkje oppgir nokon. Episodar forsvinn frå treffa når serien deira alt er der — dei bar serienamnet sitt som tittel og teikna med same plakat.
- **Ei avbroten handling er ikkje ein feil.** Lukka du eit popupark midt i eit kall, eller bytte fane, fanga `runCatching` avbrotet som om det var ein feil og viste ei feilmelding for noko du sjølv valde å avbryte. Alle 22 nettverkskalla i ViewModel går no gjennom ein `attempt`-hjelpar som slepp avbrotet vidare.
- **Ei ulesbar innlogging seier frå.** Blir Keystore-nøkkelen ugyldig — etter ei gjenoppretting til ei ny eining, til dømes — kunne appen ikkje lenger dekryptere tilgangsteiknet, men Innstillingar sa framleis «Konfigurert» medan Heim stille fall tilbake til demoinnhald. Tilstanden er no eigen, og seier at du må logge inn på nytt.
- **Berre tekst som er skriven for deg, blir vist til deg.** Nettverkslaget kastar no ein eigen unntakstype. Ein intern feil kan ikkje lenger hamne i det same feltet som «Sjekk tenaradressa og nettet», der dei to var umoglege å skilje.
- **`Retry-After` blir gjenteken.** Er tenesta oppteken og seier kor lenge, står talet i meldinga i staden for berre «prøv igjen».
- **Eitt nytt forsøk på GET.** Eit tapt fyrste forsøk over mobilnett blir prøvd om att. POST blir aldri prøvd på nytt — ein førespurnad skal ikkje kunne nå Seerr to gonger.
- **Bakgrunnsoppdateringa gir opp.** Ein tenar som står av vart før prøvd på nytt i det uendelege. Etter fire forsøk blir køyringa avslutta, og neste periode kjem uansett om ein halvtime.
- **Widgeten rekk å svare.** Tidsavbrotet var 12 sekund, meir enn ein kringkastingsmottakar trygt kan halde på resultatet. Det er no 8, og widgeten har eigne, kortare tidsavbrot mot tenaren, så «Fekk ikkje kontakt» kjem i staden for eit kort som blir ståande på «Hentar…».
- **Tilrådingslista blir ikkje henta når rada er av.** Det er det einaste kallet appen gjer til ein vert som ikkje er din eigen, og no stoppar valet i Innstillingar sjølve førespurnaden, ikkje berre visinga.

### Endringar under panseret

- **APK-en er 2,24 MB, ned frå 7,14 MB.** 5,1 MB av det gamle bygget var tre demobilete i PNG som ein tilkopla brukar aldri ser. Dei er no WebP (PSNR 43–48 dB, visuelt uskilbart). Daude ressursar frå «Reelune»-namnet er fjerna frå kjelda.
- **Einings-ID er ikkje lenger `ANDROID_ID`.** Det var ein varig maskinvarebunden identifikator som overlever avinstallering og må oppgjevast som einings-ID i eit butikkskjema. Det er no ein tilfeldig verdi laga per installasjon. Eksisterande installasjonar tek med seg ID-en dei alt hadde, så tenaren din får ikkje ei duplisert einingsoppføring.
- **Tilrådingslista ligg på ein versjonert sti** (`v1/recommendations.json`). Den gamle stien blir framleis prøvd som reserve, så rada held fram å virke medan katalogen blir flytta.
- **`mapping.txt` skal no følgje kvar release.** Utan mappinga for nøyaktig den versjonen er ein stack trace frå eit R8-bygg uleseleg, og fila blir sletta av `clean`.
- **GitHub Actions** køyrer einingstestar, lint og byggkontroll på kvar push.
- Oppstarten les ikkje lenger innstillingar frå disk på hovudtråden.
- Lint: 26 → 18 åtvaringar. `HardwareIds` og `InsecureBaseConfiguration` er borte.


## 0.13.0

### Nye funksjonar

- **Hald fram å sjå**: ny rad på Heim med halvsette filmar og episodar, henta frå Jellyfin/Emby si eiga resume-liste. Framdrifta låg alt i modellen, men vart aldri vist. Rada kan skruast av i Innstillingar, og barnebiblioteka blir aldri spurde.
- **Søk i eigne bibliotek**: Oppdag søkjer no i Jellyfin og Emby i tillegg til Seerr. Treff du alt eig kjem øvst under «I biblioteka dine», og dei to kjeldene svarar uavhengig av kvarandre, så eit søk verkar sjølv om Seerr er nede.
- **Trekk tilbake ein førespurnad**: aktive førespurnader i Aktivitet kan trekkjast tilbake, med stadfesting. Appen kontrollerer at førespurnaden er din før han sender slettinga.
- **To adresser per teneste**: legg til ei alternativ adresse under avanserte val, til dømes heimenettet og ein proxy utanfrå. Appen byter automatisk når den vanlege adressa ikkje svarar, og hugsar valet. Tilgangsteiknet høyrer til tenaren, så du treng ikkje logge inn på nytt.
- **Ekte «opne i appen»**: detaljpopupen finn ein installert Jellyfin-/Emby-klient som handterer adressa og opnar han direkte, i staden for alltid å hamne i nettlesaren. Knappen namngjev appen han opnar.
- **Varselkanalar per hending**: «klart i biblioteket», «lastar ned» og «stoppa» er no tre kanalar i Android-innstillingane, så dei kan stillast eller slåast av kvar for seg.
- **Mellomlager i SQLite (Room)**: dashbordet blir lagra i ein database i staden for éin JSON-tekst i SharedPreferences. Rader kan lesast sidevis, og ei forelda kopi blir sletta i staden for å bli tolka.
- **Paginering i Oppdag**: søkjeresultat frå Seerr stoppar ikkje lenger på dei første 20. «Hent fleire treff» hentar neste side når det finst ein.
- **Heimeskjermwidget**: «Spelar no» viser kva som spelar på Jellyfin og Emby. Avspeling blir aldri mellomlagra, så widgeten spør tenaren direkte og fell tilbake til ei tekstlinje om han ikkje når fram.

### Rettingar

- Innloggings- og tilkoplingsfeil viser no nynorsk tekst med neste steg i staden for rå Java-nettverkstekst. Eit vertsnamn som ikkje svarar, ein tenar som er av og eit sertifikat Android ikkje stolar på får kvar si melding. Jellyfin, Seerr og tilkoplingstesten følgjer no same regel som Emby alt gjorde.
- Eit uventa svar frå ein omvend proxy (til dømes ei HTML-feilside med status 200) blir rapportert som «uventa svar» i staden for parser-tekst.
- Vanleg HTTP er igjen berre tillate for ekte private adresser. Sjekken var eit prefikstest på vertsnamnet, så namn som `fcbarcelona.com` eller `192.168.1.5.nip.io` opna for ukryptert trafikk over det opne internettet.
- Heim viser den sist stadfesta feeden med ein gong ved kald start. Mellomlageret vart skrive ved kvar oppdatering, men aldri lese, så alle rader stod tomme til første nettverkssvar kom. Lageret er knytt til akkurat dei innlogga kontoane, slik at ein tidlegare brukar sin feed aldri kan dukke opp for den neste.
- Aktivitet grupperer no etter faktisk tidspunkt. «For 5 dagar sidan» hamna under «I DAG» fordi han deler prefiks med «For 5 min sidan», og ein førespurnad du nettopp sende hamna under «TIDLEGARE».
- Detaljpopupen viser igjen kvar tittelen kjem frå: «Nyleg tilgjengeleg · Radarr», «Bibliotek i Jellyfin», «I biblioteket ditt». Linja vart laga for kvar popup, men begge overskriftene kasta henne og skreiv berre tenestenamnet.
- Ei uventa feil under oppdatering krasjar ikkje lenger appen og lèt ikkje spinnaren gå for alltid. Ei innlogging som ikkje kan lagrast trygt på eininga blir sagt frå om i staden for å bli rapportert som tilkopla.
- Følgjing av førespurnader går no i lågt tempo når Aktivitet ikkje er open. Kvart intervall kostar eit profilkall, ei førespurnadsliste og opptil tjue detaljoppslag, og det gjekk kvart 30. sekund frå alle faner.
- Éin uventa oppføring frå ei teneste tømmer ikkje lenger heile rada.
- Rettar eintal i personvernkortet: «Alle 1 tilkoplingane går over HTTPS.»

## 0.12.4

- Gjer innlogga tenester kompakte i innloggingspopupen: grøn stadfesting først, konto- og utloggingsval ved behov.
- Animerer berre innhaldet inne i den faste popup-ramma, slik at opning og utviding ikkje flyttar arket.
- Rettar krasj i aktive Sonarr-nedlastingar når fleire episodar frå same sesongpakke deler nedlastings-ID.
- Avdupliserer køaktivitet ved innlesing og vernar lista mot identiske tredjeparts-ID-ar.

## 0.12.1

- Rettar X som hamna inne i arket på korte detaljoverskrifter: éi felles topplinje med fast lukkeknapp øvst til høgre for alle popupane.
- Skil rulling av innhaldet frå rørsla til heile popupen, slik at arket ikkje blir drege med ved rullegrensene. Lukking skjer med X, trykk utanfor eller Android Tilbake.
- Held òg innlogging og avspeling i den faste popup-ramma. Feil, innloggingssteg og nye opplysningar endrar ikkje den ytre høgda.
- Flyttar innlogginga sin lukkeknapp ut av rulleinnhaldet og fjernar dobbel tastaturmarg.
- Animerer X-lukking før arket blir fjerna; vernet mot lukking under sending omfattar no òg Android sin tilbakeknapp.
- Nye Android-testar kontrollerer plassering medan fingeren er nede, raske rullerørsler, dataoppdateringar, alle popup-typar, tastatur og stor skrift.

## 0.12.0

- Nyleg tilgjengeleg krev no filmkopi i eigne bibliotek og stadfesta digital utgjevingsdato; nye tilgjengelege episodar bruker sin eigen premieredato. Ingen Radarr-/Sonarr-nøkkel er nødvendig for denne rada.
- Kjem snart held fram med Radarr-/Sonarr-kalenderen; manglande tilkopling er ikkje lenger feilmerkt som ein tom kalender. Trygg delt kalender for vanlege brukarar er planlagd, ikkje sett opp.
- Normaliserer store bokstavar i protokoll og domenenamn ved innlogging; slår av automatisk stor forbokstav/retting i adressefelt og viser konkrete adressefeil.
- Samlar detaljar, kalender og førespurnader i ei fast, lik popup-ramme; utvidbar omtale og tekst som toler stor skrift.
- Viser medverkande og roller frå Jellyfin/Emby/Seerr, med Seerr-portrett når tilgjengelege.
- Legg til bibliotekfilter i Oppdag og Alle / På veg / Klare for eigne førespurnader.
- Ny lokal filmstripe i førstegongsoppsettet og ein felles visuell framdrift for førespurnader.
- Ryddar avstandar og statusmerke på Heim; cover får plass til både merke og større skrift.
- Rettar anbefalingskjelde og bevarer Oppdag-søket når ei anbefaling blir opna.
- Stoppar produksjonsbygg utan den eksisterande signeringskonfigurasjonen.

## 0.11.7

- Publiserer ein produksjons-APK med pakkenamnet `app.reelstack`, i staden for den mellombelse
  debug-pakken som gjorde Android-oppdateringar avhengige av lokal debug-signering.
- Innfører stabil lokal releasesignering, slik at nye APK-ar kan installerast som vanlege oppdateringar
  framover.

## 0.11.6

- Sjekkar anbefalingar mot Seerr under synkronisering, slik at Home viser om tittelen alt ligg i
  biblioteket eller er førespurd før ein opnar detaljane.
- Låser detaljpopupen til ei føreseieleg standardramme med fast metadata- og omtaleplass, skeleton
  medan data blir henta og avgrensa tekstlinjer for å unngå hopp når ekstra informasjon kjem inn.

## 0.11.5

- Peikar den felles anbefalingslista til det offentlege `spole-recommendations`-repoet, slik at alle
  installasjonar kan hente henne utan GitHub-innlogging.

## 0.11.4

- Rettar Anbefalingar til å bruke den felles, statiske `recommendations.json`-lista frå GitHub.
- Seerr blir brukt for live detaljar, bibliotekstatus og førespurnader når ein opnar ei anbefaling,
  men ikkje som kjelde for sjølve Home-rada.
- Cache-ar GitHub-lista lokalt slik at Home ikkje blir tom ved eit mellombels nettverksbrot.

## 0.11.3

- La til Anbefalingar frå Seerr som ei eiga, valfri Home-rad med moderne overlay-kort og detaljvising.
- La til Nyleg tilgjengeleg som ein separat release-feed frå Radarr og Sonarr, sortert etter digital/heime- eller episode-release.
- Avgrensa release-historia til dei siste 28 dagane og ignorerer filmar som berre har kinodato, slik at gamle bibliotekfilmar ikkje dukkar opp som nye.
- Held fram med eigne Jellyfin- og Emby-rader for sist lagde bibliotekinnhald, utan å blande kjeldene.
- La til skeleton-lasting, cache-støtte, detaljpopup og Innstillingar-migrering for dei nye Home-delane.

## 0.10.1

- Removed the Home greeting and date. The compact brand header now includes a personal profile picture that opens account settings.
- Prefer the verified personal Seerr account, then Jellyfin, then Emby. Missing pictures use that person's initial; shared API keys are never presented as personal accounts.
- Hide the entire Now Playing section when there are no active sessions, including during refresh. The multi-session count is preserved when playback exists.
- Added native regression coverage for profile priority/fallback, profile navigation, no greeting/date and silent empty playback.

## 0.10.0

- Renamed the app to Reelune, with a new film-ribbon R mark, adaptive launcher icon and restrained Home header. Existing app ID and update signature are preserved.
- Added a server-verified Seerr administrator boundary: ordinary users see only their own playback and personal request Activity, without administrator filters or explanatory restriction banners.
- Applied session filtering by actual media user ID, owner filtering to Seerr requests, fresh permission checks before requests/playback commands, and administrator-only shared Radarr/Sonarr queues.
- Removed persisted shared activity/sessions and stale personal-library fallback. Account changes cancel old refresh work and clear the previous account's feed.
- Excluded Barneserier/Barneseriar, Jellyfin's Barne-TV and Emby's Barne-Tv Serier libraries from recent media queries. Other libraries, including children's films, remain untouched.
- Redesigned Upcoming as full-bleed image cards with readable bottom overlays, and Discover as poster cards with type/status tags and embedded actions. Multiple active playback sessions now have an explicit count.
- Added role, owner, library-selection and native UI regressions; aligned loading placeholders and notification settings with the actual experience.

## 0.9.0

- Added a personal Seerr request sheet with explicit missing-season selection, fresh availability checks and visible account identity before submission.
- Added a default-on library notification choice, optional poster artwork and a text-only fallback when artwork is unavailable.
- Added personal request tracking in Activity: requested, actual downloading, waiting for library import and confirmed library availability. Approval alone never means downloading.
- Isolated local follows by server and verified account, preserved notification choices across app restarts, and kept existing requests notification-off until explicitly enabled.
- Replaced ambiguous availability copy with “I biblioteket ditt” and a library icon. Kept series season selection available even when existing seasons are already in the library.
- Added regression coverage for season eligibility, request identity, 4K status, persistence, notification artwork and the native confirmation flow.

## 0.8.0

- Added authenticated Jellyfin/Seerr account panels with server-provided names and profile pictures, and a visible request identity in Discover and title details.
- Personal requests now require a Seerr session and recheck its actual user ID immediately before posting. Administrator API keys remain read-only for requests; no impersonation field is sent.
- Profile pictures use origin-scoped credentials and do not follow redirects; missing pictures use an initial rather than demo artwork.

- Refined the complete native layout against authenticated service data: shorter source-labelled Home headings, clean poster art, room for longer titles and episode subtitles, and a calendar shortcut beside the greeting.
- Kept inactive playback compact during refresh, with matched artwork dimensions for skeletons and loaded cards.
- Moved Discover availability below posters and Activity status beside text; improved hierarchy across all four tabs.
- Reworked upcoming cards around readable dates and artwork; added daily calendar counts and predictable filter scrolling.
- Prioritized synopsis in detail sheets, removed repeated type/year information and made extra facts wrap without changing the sheet height.
- Added Norwegian/English synopsis fallback, richer Seerr series facts, translated statuses and bounded account-scoped request-metadata caching.
- Preserved separate Jellyfin/Emby film and episode controls, existing account sessions and the established APK upgrade identity.

## 0.6.0

- Rebuilt the visual foundation with matte charcoal surfaces, warm white typography and a restrained lime accent; removed the decorative space background and floating navigation overlay.
- Reserved real screen space for navigation and system bars, preserved tab scroll positions and shortened card reveals and touch feedback.
- Replaced Discover's oversized boxed rows with an adaptive poster grid, working movie/series filters, clear-search control and functional detail actions.
- Added a first-run service checklist, explicit demo preview, persistent setup completion and a two-stage address/account login flow with optional advanced settings.
- Added Seerr sign-in using a Jellyfin username/password or Seerr's native Jellyfin Quick Connect endpoints, with encrypted per-account session cookies and CSRF support. Administrator API keys remain supported.
- Improved login cancellation, password visibility, keyboard actions, account/permission errors and compatibility guidance for Seerr versions without Quick Connect.
- Stopped mixing sample feeds into a connected setup; real connections clear preview content before loading.
- Refined playback controls, borderless artwork, detail typography, uncropped series imagery and matching loading skeletons.
- Added network and Android UI regression coverage for account sessions, Quick Connect, setup and Discover filters.

## 0.5.3

- Added more breathing room above the Home greeting and a quiet, localized date line.
- Removed hard outlines and the dark bottom veil from Home artwork so posters and thumbnails keep their natural color and edge.
- Refined media cards with soft depth, asymmetric corners, and taller movie-poster proportions.
- Added brief staggered reveal motion to Home rails and responsive spring feedback when cards are pressed.
- Kept loading skeletons aligned with the updated card proportions to prevent layout jumps.

## 0.5.2

- Redesigned «Kjem snart» as artwork-led cards with calm date chips and a dedicated 28-day agenda calendar.
- Radarr now excludes cinema-only dates and shows only digital or physical home releases; Sonarr continues to show upcoming episodes.
- Added full-poster movie detail layouts with the poster on the left and title information on the right, without cropping the artwork.
- Switched recently added series to wide Jellyfin/Emby Thumb artwork and exact ungrouped episode results with season, episode number, and title.
- Removed the leftover playback-progress strip from recently added cards.
- Added clearer «Om filmen», «Om serien», and «Om episoden» sections, taglines, richer metadata, and studio/status details when supplied by a service.
- Removed the HomeReel name and logo from Home and moved the app identity and version into Settings.
- Updated loading skeletons to match the new portrait, landscape, and upcoming card shapes.

## 0.5.1

- Fixed Jellyfin account sign-in on servers that disable deprecated Emby authorization headers.
- Send exactly one standards-based Jellyfin authorization header during sign-in, avoiding ambiguous duplicate credentials that could surface as a server error only for valid passwords.
- Use the same modern Jellyfin authorization scheme for connection checks, library loading, authenticated artwork, details, and playback sessions after sign-in.
- Added Jellyfin Quick Connect with a native six-character code, automatic approval checking, and secure token exchange without entering a password in HomeReel.
- Redesigned playback and title sheets around cinematic artwork, layered information, smoother content changes, lighter metadata, and pill-shaped actions instead of a grid of heavy boxes.
- Refined every bottom sheet with a softer floating shape, quieter surface, and more consistent spacing.
- Keep Emby on its compatible token header and added regression coverage for the two distinct authorization paths.

## 0.5.0

- Added subtle, section-shaped shimmer skeletons for initial Home, Discover, and Activity loading without replacing useful cached content during background refreshes.
- Added Jellyfin username/password sign-in as the default setup path; only the returned access token and profile ID are stored, and the password is never persisted.
- Replaced local-only Discover filtering with debounced, authenticated Seerr search for new movies and series.
- Added richer title sheets across library cards, Discover, upcoming releases, downloads, and Activity, with live Jellyfin/Emby or Seerr detail enrichment where available.
- Added overview, runtime, year, rating, certification, genre, source, date, and status metadata to supported title details and the offline cache.
- Reworked user-facing request language from “order” terminology to the calmer “add to the media collection” flow.
- Expanded network, parser, security, loading-state, and on-device UI regression coverage.

## 0.4.6

- Fixed automatic profile detection so active child sessions or the first returned user can no longer select a restricted child profile for Home.
- Prefer an enabled administrator or full-library profile when a Jellyfin or Emby API key is not tied to a user.
- Read the available movie and series library views and interleave their newest items, preventing one busy library from filling an entire row.
- Added regression coverage for restricted child profiles and multiple movie and series libraries.

## 0.4.5

- Gave Jellyfin and Emby separate recently-added movie and series rows instead of mixing their libraries.
- Replaced letter badges with tiny service logos on media cards and row headings.
- Added Jellyfin and Emby brand marks to their Settings connection rows.
- Removed the connection count and sync warning from the Home header.
- Localized navigation, controls, states, empty messages, and errors into Nynorsk.
- Added an Android regression test that verifies both servers render as separate Home rows.

## 0.4.4

- Removed Continue Watching from Home, Settings, syncing, and cached dashboard data.
- Split recently added media into dedicated movie and series rows sourced from both Jellyfin and Emby.
- Switched Jellyfin to direct, type-filtered Latest Media calls that do not require a Profile ID.
- Kept the current and legacy user-scoped routes as fallbacks for Jellyfin and Emby compatibility.
- Migrated existing Home-section preferences automatically and made partial-service warnings amber instead of error red.

## 0.4.3

- Fixed the launch crash caused by an unsupported live-media placeholder drawable.
- Prevented authenticated Emby and Jellyfin artwork from repeatedly opening Android's secure key store while the home screen is rendered.
- Added a crash-safe artwork fallback so a malformed or rejected image request cannot close the app.
- Added an Android regression test for authenticated artwork failures.

## 0.4.2

- Kept Jellyfin and Emby marked as connected when only a personal feed or playback-session call is unavailable.
- Added partial media-server refreshes so Recently Added can still load when Continue Watching needs a Profile ID.
- Added a global Jellyfin Recently Added fallback when no media profile can be detected.
- Distinguished connected-with-limited-data warnings from actual connection failures throughout Home and Settings.
- Replaced demo artwork on Jellyfin and Emby library/session cards with authenticated server artwork while keeping tokens out of image URLs.
- Added a neutral HomeReel placeholder for media that genuinely has no server artwork instead of showing an unrelated demo poster.

## 0.4.1

- Fixed Jellyfin library loading by using the current `/UserItems/Resume` and `/Items/Latest` routes with legacy fallbacks.
- Fixed Emby library loading by using its user-scoped resume and latest-media routes.
- Added automatic media-profile discovery for API-key connections without a configured profile ID.
- Added visible, actionable Home states when media-library refresh fails or returns no items.

## 0.4.0

- Renamed the app to HomeReel and introduced a new home-and-play launcher icon.
- Aggregated Jellyfin and Emby sessions, continue-watching items, and recently-added media on one Home screen.
- Added a multi-session carousel with independent pause and resume controls.
- Added a 28-day Upcoming rail backed by the Radarr and Sonarr calendar APIs.
- Added Home-section visibility controls under Settings.
- Added artwork-rich Activity rows and Seerr media-detail enrichment for recent requests.
- Replaced the empty playback card with a compact, subdued status line.
- Preserved cached content per service during partial refresh failures.

## 0.3.0

- Added live Jellyfin and Emby continue-watching and recently-added feeds.
- Added remote pause and resume for active Jellyfin and Emby sessions.
- Added a persistent, non-secret dashboard cache for useful startup and offline state.
- Added 30-minute background refresh with an optional Wi-Fi-only constraint.
- Added an optional Jellyfin/Emby user ID for server API-key setups.
- Added animated library rails, progress artwork, and media detail sheets.
- Expanded parser, client, cache, and on-device UI coverage.

## 0.2.0

- Added live playback, queue, Seerr discovery/request feeds, remote artwork, pull-to-refresh, and partial-service failure handling.

## 0.1.0

- Added the native Compose foundation, cinematic visual system, connection editors, encrypted token storage, and service health checks.
