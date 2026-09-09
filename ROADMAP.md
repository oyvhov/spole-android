# Spole — veikart mot 1.0

Oppdatert 9. september 2026. Publisert utgangspunkt: **0.15.1 / bygg 43**, commit `265321c`. Ny lokal testversjon: **0.16.0-alpha05 / bygg 48**; ikkje publisert på GitHub. [Test-APK](docs/TEST_APK_ALPHA05.md) · [Minimerbar sidemeny](docs/SIDEBAR_ALPHA04.md) · [Førre arbeidsbolk](docs/ROADMAP_BATCH_ALPHA03.md).

**Målet:** Frå å finne ein film eller serie, via å leggje til eller følgje han, til å sjå han — med din eigen konto, i ei samanhengande og gjennomarbeidd Android-oppleving.

Dette er den gjeldande prioriteringsplanen. Versjonsnummera nedanfor er føreslegne milepålar, ikkje lovnader om datoar. Ein milepåle er ferdig når krava er dokumentert oppfylte, ikkje berre når koden er skriven.

**Implementeringa er i gang:** Språkval, engelsk førehandsvising, breiare mediesider og vindaugstilpassa detaljpanel er implementerte. Sjå [nattrapporten](docs/OVERNIGHT_REPORT_2026-09-09.md), [språkrettleiinga](docs/LOCALIZATION.md) og [testmatrisa](docs/QA_MATRIX.md). Avkryssing skil mellom automatiske resultat, tidlegare releasekontroll og testar rapporterte av brukaren. Eit avkryssa delpunkt godkjenner ikkje automatisk heile milepålen.

Siste kjeldearbeid etter alpha04: [Kalenderspråk og stor skrift](docs/CALENDAR_LANGUAGE_PASS.md). Med i den lokale alpha05-test-APK-en; ingen ny GitHub-publisering.

## Kvar står vi?

| Status | Område |
| --- | --- |
| Levert | Spole-identitet, animert oppstart, nynorsk grensesnitt og personleg profil. |
| Levert | Jellyfin-/Emby-bibliotek, Seerr-søk og førespurnader, sesongval, personleg aktivitet og lokale varsel. |
| Levert | Jellyfin-spelar med spoling, lydspor, undertekst, kvalitet og framdriftsrapportering. |
| Levert med avgrensingar | Kalender gjennom direkte Radarr-/Sonarr-tilkoplingar. Ikkje ein delt kalender for alle Seerr-brukarar. |
| Må kvalitetssikrast vidare | Fysiske telefonar, lange avspelingar, nettbyte, verkelege brukarrettar og førespurnad → bibliotek → varsel. |
| Brukartesta | To fysiske mobiltelefonar: Pixel 9 Pro XL og truleg ein OnePlus. Modell på den andre, app-/Android-versjon og konkrete scenario er ikkje stadfesta. |
| Delvis implementert | Språkval og utvidbart ressursoppsett; engelsk er uttrykkeleg ei førehandsvising. Full omsetjing står att. |
| Implementert grunnlag; vidare testing før 1.0 | Nettbrett med breiare mediesider, sentrerte detaljpanel og eigne vindaugstestar. Fysisk nettbrett er ikkje verifisert. |
| Planlagt TV-milepåle | Android TV / Google TV med fjernkontrollstyrt grensesnitt, personleg innlogging og Jellyfin-avspeling. Ikkje støtta som TV-app i 0.15.1. |
| Planlagt, ikkje levert | Valfri trygg delt kalender, Emby-avspeling, casting, bilete-i-bilete og offline-avspeling. |

0.15.1 bestod **253 einingstestar og 127 Android-testar**, med **0 lint-feil og 28 åtvaringar**. Oppdatering med bevarte ekte kontoar er kontrollert. Dei automatiske testane brukar isolerte testkontoar; grøne testar er ikkje det same som full dekning av ekte tenarar eller fysiske telefonar. Sjå [verifiseringa](docs/VERIFICATION_v0.15.1.md).

## Retninga vi held fast på

- **Ingen obligatorisk Spole-backend eller ny Spole-konto.** Appen brukar tenestene brukaren allereie har.
- **Personleg først.** Seerr-identiteten styrer førespurnader. Vanlege brukarar ser seg og sitt; administratorfunksjonar krev verifiserte rettar. Klientfiltrering erstattar ikkje tilgangskontroll på tenaren.
- **Innhald før administrasjon.** Ikkje eit eige nedlastingskontrollpanel. Framdrift høyrer heime i førespurnaden ho gjeld.
- **Presise datakjelder.** «Nyleg tilgjengeleg» betyr nyleg digital utgjeving og stadfesta kopi i biblioteka dine. «Kjem snart» kjem frå Radarr/Sonarr, ikkje frå popularitetslister.
- **Heim kan tilpassast.** Eigne film- og episoderader for Jellyfin og Emby, med separate brytarar. Dei valde barne-TV-biblioteka skal framleis haldast utanfor Heim; dette er ikkje foreldrekontroll for heile appen.
- **Anbefalingar er redaksjonelle.** GitHub-katalogen er kjelda, med bibliotekstatus før ein opnar detaljane. Ikkje kamuflerte Seerr-tilrådingar eller ein ny sosial backend.
- **Eit tydeleg visuelt særpreg.** Matte flater, gode bilete, roleg typografi og presis rørsle. Ingen ny merkevare eller full redesign for kvar versjon.
- **Fleirspråkleg frå 1.0.** Nynorsk og engelsk skal vere fullverdige språk. Nye språk skal kunne leggjast til med omsetjingar, utan å endre skjermlogikk eller byggje eigne skjermar.
- **Mobil, nettbrett og TV har ulike behov.** Nettbrett inngår før 1.0. Android TV / Google TV får ei eiga leveranse med felles data- og tilgangsreglar, men grensesnitt laga for fjernkontroll og vising på avstand.

## Milepålar

| Rekkefølgje | Milepåle | Resultat for brukaren | Avhengig av |
| --- | --- | --- | --- |
| 1 | **0.16 — Stabil kvardag** | Appen toler ekte bruk, nettproblem og lange avspelingar. | Baseline frå 0.15.1 |
| 2 | **0.17 — Heile den personlege flyten** | Finn, legg til, følg og sjå utan uklare statusar. | Stabil konto- og databehandling |
| 3 | **0.18 — Spole-finishen, språk og nettbrett** | Ei konsekvent oppleving på mobil og nettbrett, på nynorsk og engelsk. | Avklart innhald, brukarhandlingar og språkgrunnlag |
| 4 | **0.19 — Lukka beta** | Funksjonsfryst kandidat, prøvd av fleire og klar for butikkvurdering. | Godkjende kjerneflytar og publiseringsførebuingar |
| 5 | **1.0 — Første offentlege lansering** | Ei avgrensa, påliteleg og godt støtta førsteutgåve. | Beta- og lanseringskrava nedanfor |
| 6 | **1.1 — Spole på Android TV / Google TV** | Finn, følg og sjå frå sofaen med fjernkontrollen. | Stabil felles kjerne og eiga TV-beta; TV-forarbeid frå 0.18 |

Små feilrettingar kan publiserast mellom milepålane. Kritiske feil i tilgang, installasjon eller avspeling går framfor nye funksjonar.

### 0.16 — Stabil kvardag

- [x] Opprett testmatrise med dokumentert status, testkjelde og gjenståande scenario. Sjå [QA-01](docs/QA_MATRIX.md).
- [x] Køyr einingstestane på nytt utan gjenbruk av testresultat: **253/253 bestod**, 8. september 2026.
- [x] Full Android-testpakke bestod på 0.15.1: **127/127**. Dette er den tidlegare releasekontrollen, ikkje ei ny køyring i denne arbeidsrunden.
- [x] Ny implementeringskontroll på alpha01: **269/269 einingstestar, 138/138 Android-testar og 46/46 ekstra breiddetestar**. Signert oppdatering frå 0.15.1 og språkbyte med bevarte ekte kontoar er kontrollert på review-emulatoren. Sjå [nattrapporten](docs/OVERNIGHT_REPORT_2026-09-09.md).
- [x] Registrer brukartest på to fysiske telefonar: **Pixel 9 Pro XL** og **truleg OnePlus**. Stadfesta av brukaren 8. september 2026; detaljert omfang er enno ukjent.
- [ ] Fullfør fysisk testprotokoll med appversjon, Android-/tenarversjon, andre telefonmodell, scenario og resultat. Stadfest dekning av ein telefon med meir avgrensa yting; dette er ikkje kjent frå «OnePlus» åleine.
- [ ] Køyr minst éi samanhengande 60-minutts avspeling på kvar telefon. Test direkteavspeling, HLS/omkoding, lyd, tekst, spoling, skjuling av kontrollar, Tilbake og rotasjon.
- [ ] Test nettbrot, Wi-Fi/mobilnett-byte, bakgrunn, prosessavslutting og gjenopning. Ingen uventa avspeling etter retur eller kontoendring.
- [ ] Prøv ekte administrator og vanleg brukar, utgått innlogging, tilbakekalla tilgang, utlogging og kontobyte. Ingen gamle profilar, aktivitetar eller avspelingar skal lekke mellom kontoar.
- [ ] Mål kaldstart, første popup-opning, scrolling og kontrollrespons på fysisk eining. Fang spor for hakk, ikkje berre vurder emulatorvideo.
- [x] Sorter dei **28 lint-åtvaringane** etter risiko og oppfølging i [testmatrisa](docs/QA_MATRIX.md#kodekontroll--første-sortering).
- [ ] Rett relevante lint-funn og dokumenter endeleg vurderte unntak. Første sortering er ikkje ein ferdig tryggleiks- eller kompatibilitetskontroll.

**Ferdig når:** Testmatrisa er gjennomført, ingen kjende krasj eller tilgangsbrot står att i kjerneflytane, og kvart reproduserbart feilfunn har ein regresjonstest der det er praktisk mogleg. Funksjonar som krev omkoding, skal forklarast utan å love støtte for alle kodekar.

### 0.17 — Heile den personlege flyten

- [ ] Verifiser førespurnad → eventuell godkjenning → faktisk nedlasting → import → tilgjengeleg i bibliotek → varsel, med kontrollerte ekte tenester.
- [ ] Fullfør tydeleg kvote- og godkjenningsinformasjon før sending. Bruk berre handlingar Seerr støttar og kontoen har rett til.
- [ ] Gå gjennom søk med fleire resultatsider og personleg historikk. Utvid historikken der dagens avgrensingar skjuler eldre førespurnader; eksisterande søkepaginering skal ikkje byggjast på nytt.
- [ ] Test manglande sesongar, delvis tilgjengelege seriar, komande episodar og seriar som held fram. Behald «følg utan ny førespurnad» og unngå duplikat. Ikkje lov automatisk framtidig innhenting utan stadfesting.
- [ ] Vis bibliotekstatus på anbefalingar før detaljopning, med sikre ID-treff, tydeleg ukjend status og robust handtering når GitHub-katalogen ikkje kan hentast.
- [ ] Test «Nyleg tilgjengeleg» med digital dato innanfor 28 dagar og faktisk bibliotektilgang. Gamle filmar som nett er importerte, skal ikkje bli nye utgjevingar. Manglande dato skal ikkje diktast opp.
- [ ] Kontroller «Kjem snart» mot verkelege Radarr-/Sonarr-data: heimeutgjevingar, episodar, tidssoner, ukjende datoar og delvis tenarsvikt. Kinodato er ikkje heimeutgjeving.
- [ ] Test varsel med løyve på/av, appen lukka, batterisparing og kontobyte. Bilete er valfritt; privat innhald skal ikkje eksponerast unødig på låseskjermen.

**Ferdig når:** Ein vanleg brukar kan finne ein serie, velje ein manglande sesong, følgje framdrifta og finne innhaldet att utan Radarr-/Sonarr-nøklar. Kalender utan slike tilkoplingar er eit separat sidespor. Varsel blir omtala som periodisk kontroll, ikkje garantert sanntids-push.

Verkelege førespurnader kan starte nedlastingar. Slike testar skal bruke eigargodkjende testtitlar og kontoar; dette veikartet er ikkje løyve til å endre produksjonsbiblioteka.

### 0.18 — Spole-finishen, språk og nettbrett

- [ ] **Heim:** Integrert, diskret søk på mobil som fører naturleg til Oppdag; nettbrett brukar søket i Oppdag; stabil profil; tydeleg tal ved fleire aktive avspelingar; ingen tom «spelar no»-seksjon.
- [ ] **Detaljar:** Fast opningsgeometri, reserverte bilet- og tekstområde, roleg skeletonlasting og ein alltid tilgjengeleg lukkeknapp. Sein metadata skal ikkje flytte heile popupen.
- [ ] **Oppdag:** Eitt tydeleg hovudfilter, sekundærfiltrering ved behov, lesbare statusmerke og handlingar på cover. Ingen ekstra kontorad når profilen kan liggje i toppen.
- [ ] **Aktivitet:** Ei oversiktleg personleg tidslinje med plakatar, konkrete statusar og neste handling. Førespurnader og følgde titlar skal vere lette å skilje.
- [ ] **Kalender:** Gode daggrupper og bilete, lesbare datoar, diskret kjelde og eit tydeleg skilje mellom filmutgjeving og episode. Ikkje berre fleire innramma kort.
- [ ] **Innlogging og innstillingar:** Tilkopla tenester blir kompakte, utvidbare rader med grøn hake. Enkel utlogging, kopierbar Quick Connect-kode og få val før vanleg innlogging.
- [ ] **Rørsle og tilgjengelegheit:** Same overgangsprinsipp i heile appen, respekt for redusert rørsle, TalkBack, stor skrift og gode trykkflater. Nettbrett og liggjande vising skal ha gjennomtenkte oppsett.

**Ferdig når:** Første og gjenteken opning av detaljar er stabile gjennom minst 20 opne/lukke-rundar med treg datalasting på fysisk telefon. Ei samanliknbar før/etter-måling viser at visuell finish ikkje har gjort scrolling eller oppstart tregare. Kjernehandlingane fungerer med skjermlesar og skriftstorleik 2.0.

#### Språk — nynorsk og engelsk først, enkelt å utvide

Ny, avgrensa kalenderbolk etter alpha04:

- [x] Flytt kalenderens kontrollar, tomtilstandar, datoetikettar og teljingar til nynorsk/engelsk-ressursar; 17 tekstnøklar og to fleirtalsressursar.
- [x] Test at kalenderen bevarer vald dag og typefilter ved språkbyte. Datorekkjefølgje følgjer UI-språket; klokkevising brukar Android sitt tidsformat.
- [x] Rett klipt datotekst og test kalenderkontrollar på breitt vindauge med skriftstorleik 2.0.
- [x] Fullhøgd, lagra og minimerbar sidemeny med fokus-/animasjonstestar vart levert i alpha04. Dette godkjenner ikkje heile TV-milepålen.

Fullkrava nedanfor står opne der delarbeidet ikkje dekkjer heile kravet. Levert i alpha03:

- [x] Fjern søkesnarvegen frå Heim på breie vindauge; bevar søket i Oppdag og snarvegen på mobil.
- [x] Personlege einingsval for aksent og biletstorleik, med automatisk Jellyfin-framhald som standard.
- [x] Brei ikon-/tekstmeny, kategori/innhald i Innstillingar og immersive-video med tilpass/fyll-val. Fysisk nettbrett og Android TV er framleis opne krav.
- [x] Kompakt, sidestilt episodehovud og større plakat i førespurnadspanelet på nettbrett.
- [x] Flytt kontooversikt, sesongval og personleg førespurnadsframdrift til nynorsk/engelsk-ressursar, inkludert fleirtal og datovisning.

Levert i alpha01:

- [x] Språkveljar med «Følg eininga», nynorsk og engelsk førehandsvising; lagra val og gjenskaping av skjermen er Android-testa.
- [x] Nynorsk blir bevart ved migrering frå eldre appval eller lagra tilkoplingar; nye installasjonar følgjer eininga.
- [x] Migrer hovudnavigasjon, onboarding, sentrale skjermtekstar, widget og spelarkontrollar til språkressursar.
- [x] Bruk stabile meny-ID-ar i spelaren, uavhengig av omsett etikett.
- [x] Legg til ressurs-, parameter-, fleirtals- og migreringstestar og [omsetjarrettleiing](docs/LOCALIZATION.md).

Språkgrunnlaget startar parallelt med 0.17, slik at nye tekstar ikkje må flyttast og omsetjast to gonger. Full språkstøtte er eit krav for 0.18 og betautgåva, ikkje eit valfritt tillegg etter 1.0.

- [ ] Samle alle app-eigde brukartekstar i Android-språkressursar med stabile nøklar: skjermar, innlogging, feil, tomtilstandar, spelar, varsel, widgetar og skjermlesartekstar. Bruk fleirtalsformer og formateringsparameter, ikkje samansette tekstfragment.
- [ ] Lever komplette, gjennomgåtte omsetjingar på **nynorsk (`nn`) og engelsk (`en`)**. Engelsk blir komplett reservespråk for språk appen enno ikkje støttar. Behald nynorsk for eksisterande installasjonar som ikkje har gjort eit språkval.
- [ ] Legg til **Språk** i innstillingane: «Følg eininga», «Norsk nynorsk» og «English». Nye installasjonar følgjer eininga; eit eksplisitt val skal bli hugsa. Språkbyte skal ikkje krevje ny innlogging eller miste kontodata.
- [ ] Tilpass datoar, klokkeslett, tal og fleirtal til språk og relevante systemval. Test mellom anna kalender, episodetal, varsel og framdrift.
- [ ] Skil grensesnittspråk frå filmomtalar, titlar og lyd-/undertekstval. Hent omsett metadata der tenesta støttar det, men bruk tilgjengeleg tenar-/originaltekst når omsetjing manglar. Språkbyte skal ikkje endre bibliotekmetadata eller avspelingsval.
- [ ] Lag ei kort omsetjarrettleiing med ordliste, kontekst, døme på parameter og framgangsmåte for å leggje til eit språk. Omsetjingar kan bidra gjennom vanlege GitHub-endringar; ingen ny backend eller betalt omsetjingsteneste er nødvendig.
- [ ] Legg inn automatiske kontrollar av manglande omsetjingar, parameter og fleirtalsressursar. Test kunstig forlenga tekst og høgre-til-venstre-layout som førebuing for fleire språk, utan å hevde at desse språka alt er støtta.

**Språkkravet er oppfylt når:** Heile kjerneflyten er prøvd på nynorsk og engelsk, inkludert varsel og Tilbake i spelaren. Språkval overlever omstart, oppdatering og kontobyte. Ingen app-eigde tekstar fell utilsikta tilbake til nynorsk i engelsk vising, og lange omsetjingar klipper ikkje viktige handlingar. Eit nytt språk kan registrerast gjennom ressursfiler og språklista utan endringar i forretningslogikken.

#### Nettbrett — fullverdig støtte før 1.0

Levert i alpha01:

- [x] Del vindaugsreglar mellom navigasjon, sider og dialogar; ingen modellbasert nettbrettgjetting.
- [x] Utvid Heim/Oppdag på breie vindauge (alpha03: heile mediebreidda med innrykte profilkontrollar); hald lesesider på maksimalt 840 dp.
- [x] Vis eit sentrert 720 dp detaljpanel på breie, høge vindauge og eit botnpanel på telefon.
- [x] Android-regresjonar for sein tekst utan endra panelramme, synleg lukkeknapp med tastatur og bevaring av sidetilstand ved breiddeskifte.

Det finst allereie sidenavigasjon ved breie vindauge og avgrensa innhaldsbreidd. Dette er eit grunnlag, ikkje dokumentasjon på at heile nettbrettopplevinga er ferdig testa.

- [ ] Tilpass Heim, Oppdag, Aktivitet og Innstillingar til faktisk vindaugsbreidd. Bruk plassen til fleire synlege titlar, gode avstandar og lesbar tekst, ikkje strekte mobilkort.
- [ ] Behald tydeleg hovudkolonne og gjennomgå detaljpanel for breie vindauge. Eventuell liste/detalj-vising må ha ei avklart navigasjonsløysing og er ikkje eit krav om å byggje om alle skjermar.
- [ ] Test ståande/liggjande, delt skjerm og endring av vindaugsstorleik medan søk, popup eller spelar er open. Behald søk, vald tittel og relevant navigasjons-/avspelingsstatus.
- [ ] Verifiser skjermtastatur, eksternt tastatur, mus og stor skrift utan skjulte handlingar. Språkkrava gjeld òg på nettbrett.
- [ ] Køyr eigen nettbrettprofil i isolert emulator og prøv endeleg release på minst eitt fysisk nettbrett. Mobiltestane tel ikkje som nettbrettverifisering.

**Nettbrettkravet er oppfylt når:** Kjerneflyten og avspelingskontrollane er prøvde på smalt og breitt vindauge, i begge retningar, utan klipping eller tapt tilstand ved storleiksbyte. Fysisk nettbrettest og eigne skjermbilete er dokumenterte før støtta blir marknadsført som ferdig.

Følg [Android sine råd om tilpassa vindauge](https://developer.android.com/develop/adaptive-apps/guides/adaptive-dos-and-donts) og [kvalitetskrava for adaptive appar](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality).

### 0.19 — Lukka beta og Play-førebuing

Eigaravklaringar og butikkarbeid startar parallelt med 0.16, sidan dei kan ta tid utanom utviklinga.

- [ ] Avklar Play-kontoeigar, kontotype, støtteadresse og kven som tek vare på signeringsnøklane.
- [ ] Planlegg Play App Signing slik at eksisterande GitHub-installasjonar kan oppdaterast. **Opplastingsnøkkel og app-signeringsnøkkel er ulike roller**; same opplastingsnøkkel åleine sikrar ikkje kompatibilitet. Test faktisk oppdatering mellom kanalane før lansering. [Android: app-signering](https://developer.android.com/studio/publish/app-signing).
- [ ] Bygg og valider ein fersk signert AAB: installasjon, R8, mapping, gjeldande mål-API, einingsstøtte og 16 KB-kompatibilitet for endelege avhengnader. Tidlegare kontrollar erstattar ikkje kontroll av lanseringskandidaten.
- [ ] Publiser ei offentleg tilgjengeleg personvernerklæring og fyll ut Data safety etter faktiske nettverksstraumar, inkludert tenestepålogging, bilete, GitHub-katalog, avspeling og tredjepartskode. «Ingen eigen backend» er ikkje i seg sjølv eit svar på alle felta. [Google: Data safety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).
- [ ] Gjer ferdig butikktekst, målgruppe/aldersmerking, ikon, grafikk, oppdaterte skjermbilete med lovleg testinnhald, kjelde-/lisensmerknader og støtteinformasjon.
- [ ] Ta med nynorsk- og engelskspråklege testarar i betaen, og klargjer engelsk butikktekst og skjermbilete i tillegg til den norske presentasjonen.
- [ ] Lag avgrensa, stabile testkontoar og tydelege instruksjonar for butikkvurderinga. Ikkje del private administratorinnloggingar. [Google: tilgang ved appvurdering](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en).
- [ ] Gjennomfør lukka beta med feilrapportering og ny test av retta feil. For personlege utviklarkontoar oppretta etter 13. november 2023 krev Google per kjeldekontrollen minst 12 påmelde testarar samanhengande i 14 dagar før ein kan søkje produksjonstilgang. Kontotypen vår må avklarast; dette er ikkje eit universelt krav for alle kontoar. [Google: testkrav](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en).

**Ferdig når:** Betatilbakemeldingane er vurderte, oppgraderingar bevarer innlogging og innstillingar, butikktilgang er prøvd, og ingen opne alvorlege feil blokkerer kjerneflytane. Kontroller gjeldande Play-krav på nytt ved innsending; godkjenning kan ikkje garanterast av ein lokal testpakke.

### 1.0 — Kontrollert lansering

- [ ] Frys funksjonsomfanget; køyr full testpakke og fysisk røyketest på den faktiske kandidaten.
- [ ] Godkjenn alle obligatoriske krav frå 0.16–0.19, eller avgrens og dokumenter støtta eksplisitt før lansering. Tilgangsbrot og installasjonsfeil kan ikkje godkjennast som kosmetiske unntak.
- [ ] Publiser gradvis der kanalen støttar det, med utgjevingsnotat, kjende avgrensingar og fungerande støttekanal.
- [ ] Ha ei planlagd rettingsutgåve med høgare versjonskode ved alvorlege feil. Ikkje baser beredskapen på at brukarar kan installere ein eldre APK over ein nyare.
- [ ] Følg innkomne feil og Play sine stabilitetsrapportar. Nye analyse- eller krasjtenester krev ei eiga personvernvurdering.

## 1.1 — Spole på Android TV / Google TV

**Planlagt leveranse, ikkje berre ein idé.** Føreslegen rekkefølgje er mobil/nettbrett 1.0 først og eiga TV-beta fram mot 1.1. Kartlegging og prototype kan starte under 0.18. Dette er ikkje ein datoavtale.

### Brukarvald hovudreferanse: Wholphin

[Wholphin](https://github.com/damontecres/Wholphin) skal vere ein sentral referanse for TV-opplevinga. Brukaren framheva appen 9. september 2026. Første gjennomgang gjeld README, utviklardokumentasjon og utvalde UI-komponentar ved revisjon `0b995b5404aba166a7931ade67ab1bc256f8418b`; ikkje testing av Wholphin i køyrande TV-app.

Dokumenterte mønster som er relevante: justerbare mediarader og biletformat, sidemeny med snarvegar, Seerr i oppdaginga, profilvern, D-pad-spoling og trickplay. [Funksjonsoversikt](https://github.com/damontecres/Wholphin/blob/0b995b5404aba166a7931ade67ab1bc256f8418b/README.md). Detaljhovudet samlar tittel/logo, kortmetadata, sjanger, strauminfo og utvidbar omtale; fokus på omtalen tek henne inn i synsfeltet. [MovieDetailsHeader](https://github.com/damontecres/Wholphin/blob/0b995b5404aba166a7931ade67ab1bc256f8418b/app/src/main/java/com/github/damontecres/wholphin/ui/detail/movie/MovieDetailsHeader.kt). Sjølv plasshaldar-/feilrader er fokusbare, slik at dei kan navigerast forbi med fjernkontrollen. [FocusableItemRow](https://github.com/damontecres/Wholphin/blob/0b995b5404aba166a7931ade67ab1bc256f8418b/app/src/main/java/com/github/damontecres/wholphin/ui/components/FocusableItemRow.kt).

For Spole betyr dette følgjande planlagde prioriteringar, ikkje ferdige funksjonar:

- **TV-heim og sidemeny:** store omslag/thumbnails, tydeleg radstruktur og lett tilgang til bibliotek, Oppdag og profil. Bevar separate Jellyfin-/Emby-rader og brukarens val av seksjonar.
- **Fokus før pynt:** synleg fokus med varsam animasjon utan å flytte nabokort. Hugs rad og tittel ved Tilbake; sein metadata eller bilete skal ikkje flytte fokus. Lasting og feil må ikkje bli fokusfeller.
- **Detaljar for sofaavstand:** filmatisk kunst, ryddig metadata og tydelege hovudhandlingar. Sesongval, delvis tilgjenge og personleg Seerr-førespurnad skal fungere utan berøring.
- **TV-spelar:** retningstastar for spoling, gode lyd-/tekstval, rask kontrollvising og føreseieleg Tilbake. Trickplay og kapittel er vidare forbetringar når serveren tilbyr data, ikkje føresetnader for avspeling.
- **Personleg, delt skjerm:** enkel Quick Connect, synleg aktiv profil og vern mot utilsikta kontobyte. Same rettar og eigne førespurnader som på mobil.

Spole skal behalde sitt eige matte uttrykk, logo, fargeval og fleirtenesteflyt. Ingen Wholphin-kode, grafikk eller nye avspelingsmotorar er tekne inn i denne dokumentasjonsendringa; eventuell kodegjenbruk krev eiga lisens-/avhengigheitsvurdering.

- [ ] Før TV-prototypen blir godkjend: samanlikn Heim → detaljar → avspeling → Tilbake i Wholphin og Spole på TV, med fjernkontroll og ekte kontodata. Noter fokus, respons, lesbarheit og tilbakeføring; ikkje godkjenn på statiske skjermbilete åleine.

### Leveranse og verifisering

- [ ] Lag ei TV-tilpassa navigasjonsflate med store mediebilete, lesbar tekst på avstand og synleg fokus. Gjenbruk repository, kontorettar, språkressursar og spelarlogikk der det passar.
- [ ] Gjer heile flyten mogleg med retningstastar, OK, Tilbake og medieknappar: Heim → søk → detaljar → sesongval → avspeling. Hugs fokus når ein kjem tilbake til ei rad, og unngå fokusfeller i dialogar.
- [ ] Tilpass innlogging til sofaen: tydeleg Quick Connect-kode der tenesta støttar det, og eit fungerande skjermtastatur som alternativ. Ikkje føreset at Jellyfin-innlogging automatisk gir Seerr-tilgang. Eventuell QR-kode skal ikkje innehalde passord eller varige tilgangsteikn.
- [ ] La Seerr-brukaren følgje og sende eigne førespurnader også på TV, utan administratornøklar eller obligatorisk telefonapp. Vis den aktive profilen tydeleg på den delte skjermen.
- [ ] Tilpass Jellyfin-spelaren til fjernkontroll, lyd-/tekstmenyar og kvalitet. Test play/pause, spoling, Tilbake, bakgrunn og gjenopning. Emby-bibliotek er ikkje det same som Emby-avspeling; ikkje vis ei spelehandling som ikkje er støtta.
- [ ] Legg til TV-startpunkt, banner og korrekte maskinvarekrav først når TV-grensesnittet faktisk fungerer. Ingen krav om berøringsskjerm; TV må ikkje berre få den eksisterande mobilstartaren.
- [ ] Køyr ei eiga TV-testpakke og beta på TV-emulator og minst éi fysisk Android TV-/Google TV-eining. Test med ekte fjernkontroll, ikkje berre museklikk. Verifiser eigne TV-butikkressursar og gjeldande kvalitetskrav før TV-publisering.

**Ferdig når:** Ein ny brukar kan logge inn, finne ein tittel, sende/følgje ei tillaten førespurnad og spele ein tilgjengeleg Jellyfin-tittel utan berøring eller mus. Ingen fokusfeller, feil profil eller tapt Tilbake-navigasjon. Alle TV-kjerneflytar er dokumenterte på fysisk eining. Casting til TV er eit anna produktområde og erstattar ikkje denne støtta.

Grunnlag: [TV-navigasjon](https://developer.android.com/training/tv/get-started/navigation) og [Android TV-kvalitetskrav](https://developer.android.com/docs/quality-guidelines/tv-app-quality).

## Valfritt sidespor: trygg delt kalender

**Status: planlagt, ikkje implementert og ikkje eit obligatorisk 1.0-krav.**

Målet er kalender for vanlege Seerr-brukarar utan å distribuere administratornøklar til Radarr/Sonarr. Først må ein prototype dokumentere korleis personleg innlogging, tilbakekalla tilgang og innhaldsavgrensingar kan handhevast på tenaren. Vi skal ikkje føresetje at Seerr gir alle nødvendige bibliotekrettar.

Ei eventuell løysing blir ein liten, sjølvhosta, lesebeskytta adapter hos tenareigaren: faste datakjelder, minimalt med metadata, tidsavgrensa mellomlager og ingen brukarstyrte vilkårlege oppstraumsadresser. Ho krev eiga godkjenning før utrulling. Spole skal fungere utan henne, og private kalenderdata skal ikkje leggjast i ein offentleg GitHub-fil.

Sjå [eigen plan for delt kalender](docs/SHARED_CALENDAR_PLAN.md).

## Andre utvidingar etter 1.0 — prioritert kandidatliste

Dette er kandidatar, ikkje vedtekne leveransar. Prioriter etter faktisk bruk og betatilbakemeldingar, utan å skyve den avtalte TV-milepålen umerkeleg ut av planen.

1. **Betre seriemaraton:** Neste episode, episodemeny og hugsa lyd-/tekstval per brukar. Ingen automatisk hopping utan tydeleg brukarstyring.
2. **Bilete-i-bilete:** Vidare Jellyfin-avspeling medan ein brukar andre appar, med korrekt pause, livssyklus og systemkontrollar.
3. **Emby-avspeling:** Same grensesnitt, men eiga forhandling av avspeling, tilgang og framdrift — ikkje berre byte av tenarnamn.
4. **Casting:** Eiga vurdering av mottakarstøtte, autentisering, undertekst og kva som skjer når telefonen forlèt nettet.
5. **Fleire språk og større skjermar:** Utvid frå nynorsk og engelsk med kvalitetssikra omsetjingar etter etterspurnad, til dømes bokmål, svensk, dansk og tysk. Vidare nettbrettforbetringar blir prioriterte etter bruk.
6. **Offline:** Seinare moglegheit med nedlastingsval, lagringsgrenser, kontoisolasjon og opprydding. For stort til å vere eit lite tillegg før 1.0.

Ikkje prioritert: obligatorisk sosial backend, AI-genererte tilrådingar, full Radarr-/Sonarr-administrasjon, eit eige køkontrollpanel eller ny namne-/logorunde.

## Neste arbeidsrunde — konkret startliste

| Prioritet | Oppgåve | Leveranse |
| --- | --- | --- |
| Ferdig · QA-01 | Opprett éi testmatrise for release, ekte tenester og fysiske einingar. | [Testmatrise](docs/QA_MATRIX.md) levert; konkrete testscenario blir følgde opp separat. |
| P0 · AUTH-01 | Verifiser vanleg brukar mot administrator, utlogging og tilbakekalla tilgang. | Ingen datalekkasje mellom kontoar; regresjonstestar for funn. |
| P0 · PLAYER-01 | Langtest av spelar, OSD, Tilbake og nettbrot. | Målingar og feilrettingar frå fysisk telefon, ikkje berre emulator. |
| P1 · REQUEST-01 | Gjennomfør godkjend ekte førespurnad og følg varslet heilt fram. | Dokumentert overgangskjede, sesongstatus og varseltid med avgrensingar. |
| P1 · MOTION-01 | Mål første popup-opning og scrolling med sein metadata. | Før/etter-spor og kontrollert stabil geometri. |
| P1 · LANG-01 | Kartlegg brukartekstar og lag språkgrunnlag før vidare UI-utvidingar. | Ressursstruktur, ordliste, nynorsk/engelsk og dokumentert språkval/reservespråk. |
| P1 · TABLET-01 | Lag dedikert testdekning for breie vindauge, rotasjon og delt skjerm. | Testa nettbrettflyt før 1.0; ikkje godkjend berre fordi appen startar. |
| Planlagt · TV-01 | Kartlegg TV-startpunkt, fokus, fjernkontroll og innlogging. | Eiga TV-prototype frå 0.18 og TV-beta mot 1.1. |
| Parallelt · STORE-01 | Avklar Play-eigar, signering, testarar og offentleg personvernside. | Avgjerdsnotat; ingen nøkkelbyte eller publisering før avklaring. |

## Slik held vi planen oppdatert

Ved kvar release: oppdater utgangspunktet, kryss berre av dokumentert fullførte punkt, lenk til verifiseringa og flytt nye alvorlege feil framfor funksjonsarbeid. Emulator med ekte kontoar blir brukt til manuell kontroll; automatiske testar held fram på isolerte kontoar. Private skjermbilete, innloggingar og tenarnøklar skal ikkje følgje dokumentasjonen.

Teknisk bakgrunn: [arbeidsrettleiing](docs/AI_INSTRUCTIONS.md), [Jellyfin-spelar](docs/JELLYFIN_PLAYER.md), [førespurnadsflyt](docs/REQUEST_FLOW.md) og [tilgangsmodell](docs/VIEWER_ACCESS.md). [Tidlegare produktplan](docs/PRODUCT_PLAN.md) og [tidlegare publiseringsvurdering](docs/PUBLISHING_READINESS.md) er historisk bakgrunn, ikkje fasit på kva som står att i dagens versjon.
