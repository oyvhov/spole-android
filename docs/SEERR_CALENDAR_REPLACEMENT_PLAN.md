# Plan for å fjerne Sonarr og Radarr frå Spole

Avtalt retning 5. oktober 2026. Implementert i 1.0.0-beta3. Endeleg bygg-, test- og oppgraderingsresultat ligg i [verifiseringsrapporten](VERIFICATION_v1.0.0-beta3.md).

## Implementasjonsstatus for beta3

- Direkte Sonarr-/Radarr-integrasjonar er fjerna frå tenestetypar, klientar, oppsett, filter, ikon og ressursar. Oppgraderingsmigreringa fjernar gamle tilkoplingar og krypterte nøklar før kontolasting.
- Personleg kalender les Seerr-detaljar, sesongar, digitale filmpremierar og alle sider med eigne aktive førespurnader. Frø frå Jellyfin/Emby kjem frå den innlogga brukarens «Neste», «Hald fram» og seriefavorittar.
- Lokale følgje- og skjuleval er avgrensa til profil, stabil Seerr-tenaridentitet og verifisert brukar-ID. Ingen følgjehandling sender førespurnad eller endrar tenarovervaking.
- Datooversikta har 29 dagar inkludert i dag, film-/episodefilter, sesonggrupper og utvidbar episodeoversikt. Heim viser éi neste hending per serie. Titlar utan dato i perioden får eiga liste.
- Kalenderen blir publisert før den større filmkataloggjennomgangen. Metadata har seks timars mellomlager, avgrensa samtidige kall og ingen stille kandidat-/hendingsgrense. Gammal kalendercache blir ikkje vist før ny identitetskontroll.
- Lesetest på den eksisterande Seerr-kontoen gav 57 kalenderhendingar på TV. Følgje/skjuling og ekte episodetaljar er prøvde utan sending av førespurnader. Ordinære kontoar er dekte av syntetiske tilgangs-/sesjonstestar; ingen ekstra ekte ordinær konto var tilgjengeleg i denne arbeidsmappa.

Avgrensingar i denne utgåva: spesialsesong 0 er utelaten; særskild følgje av spesialsesong er ikkje bygd. Manuelle følgjeval blir ikkje synkroniserte mellom einingar. Manglande ekstern ID blir ikkje gjetta. Kalenderen gir ingen nye påminningar og erstattar ikkje Seerr sine tenartenester for automatisk henting. Daterte titlar er synlege i agendaen, medan titlar utan dato i perioden ligg under han; eiga samla følgjeoversikt er eit seinare tillegg.

Spole skal ha Jellyfin, Emby og Seerr som dei einaste tenesteintegrasjonane. Sonarr og Radarr blir fjerna frå oppsett, innstillingar, nettverksklientar, datamodellar og brukargrensesnitt. Personleg kalender, førespurnader og framdrift skal fungere gjennom eksisterande personlege kontoar. Denne planen erstattar [den tidlegare adapterplanen](SHARED_CALENDAR_PLAN.md) og eldre kalenderpunkt i [veikartet](../ROADMAP.md).

## Omfang og tenaravhengigheiter

Fjerninga gjeld Spole på telefon, nettbrett og TV. Ingen Sonarr-/Radarr-adresser eller nøklar skal vere nødvendige, lagrast eller brukast i den ferdige appen. Emby-støtta blir bevart.

Seerr sine metadata om komande episodar og filmutgjevingar kan hentast utan direkte tilgang til Sonarr/Radarr. Seerr brukar derimot desse tenestene på tenarsida for automatisk henting av førespurd innhald og nedlastingsframdrift. Å fjerne dei frå Spole erstattar ikkje denne tenarfunksjonen. Denne planen avinstallerer eller endrar ingen tenartenester. Sjå [Seerr sitt tenesteoppsett](https://docs.seerr.dev/using-seerr/settings/services/) og [nedlastingsovervakinga](https://github.com/seerr-team/seerr/blob/develop/server/lib/downloadtracker.ts).

Det skal ikkje byggjast eit eige kalenderadapter, ein Spole-backend, ein direkte TMDB-integrasjon eller eit nytt innloggingssystem. Kalenderen skal fungere med ein ordinær Seerr-konto. Jellyfin/Emby skal framleis fungere utan Seerr; då fell Seerr-datoar og førespurnader bort med ein konkret forklaring.

## Kva som erstattar dagens funksjonar

| Dagens funksjon | Endeleg løysing |
| --- | --- |
| Sonarr-episodar i «Kjem snart» | Personlege serietitlar frå bibliotek, aktivitet og følgjeval; episode- og sesongdatoar frå Seerr. |
| Radarr-filmar i «Kjem snart» | Eigne aktive filmførespurnader og manuelt følgde filmar; digitale utgjevingsdatoar frå Seerr. |
| Kalender | Same datagrunnlag som «Kjem snart», gruppert etter dato. |
| «Nyleg tilgjengeleg» | Utgjeving siste 28 dagar og faktisk tilgjenge i brukarens Jellyfin-/Emby-bibliotek. Filmar krev digital dato. |
| Direkte administratorkø | Fjernast. Vis status og eventuell rapportert framdrift på brukarens Seerr-førespurnader. |
| Førespurnader og aktivitet | Personleg Seerr-flyt, med serverstyrte rettar og eksisterande kontoavgrensing. |
| Sonarr-/Radarr-oppsett og kjeldemerke | Fjernast frå alle skjermar, filter, hjelpe- og feilmeldingar. |

Den nye kalenderen uttrykkjer brukarens interesser. Han er ikkje ei kopi av tenareigarens overvaka katalog. Titlar som berre finst i Sonarr/Radarr, og verken finst i biblioteket, eigne Seerr-førespurnader eller lokale følgjeval, kjem ikkje automatisk med.

## Personleg utval og følgjeval

Kalenderen tek automatisk med foreldre-seriane til «Neste» og «Hald fram», seriefavorittar og aktive eigne Seerr-førespurnader. Alle bibliotekfrø kjem frå den innlogga brukarens tillatne bibliotek og gjeldande val for framsida. Ein tittel må ha ei konkret kjelde; ein trend- eller tilrådingsliste blir ikkje brukt som kalenderfrø.

Detaljsida for film og serie får «Følg i kalenderen» og «Slutt å følgje i kalenderen». Ei oversikt «Titlar eg følgjer» viser både daterte titlar og «Dato ikkje annonsert». Brukaren kan skjule ein automatisk inkludert tittel; eit slikt val har prioritet over nye automatiske frø. Ein ferdig sett serie kan følgjast manuelt for å fange opp ein seinare sesong.

Kalenderfølgje blir lagra lokalt per Spole-profil, stabil Seerr-tenaridentitet og verifisert Seerr-brukar-ID. Han er skild frå eksisterande sesongbjølle, førespurnadsvarsel og favorittstatus. Følgjeval sender ingen førespurnad og endrar ingen overvaking. Integrasjon med Seerr sin native watchlist er ikkje del av denne leveransen. Manuelle kalenderfølgjar blir ikkje synkroniserte mellom telefon og TV i denne leveransen; automatisk utval blir berekna frå kontoane på kvar eining.

Eksisterande personlege førespurnader og varslingsval blir bevarte. Kalenderfølgje aktiverer ingen nye varsel. Kalenderpåminningar og synkronisering av lokale følgjeval er separate seinare oppgåver.

## Episodar og datoar

Komandevisinga dekkjer frå i dag til og med 28 dagar fram. Ein kort rad på Heim og den fullstendige kalenderen bruker same resultat. Heim viser neste daterte hending per serie; kalenderen viser alle kjende episodar i vindauget. Samtidige episodar frå ein sesong blir samla som éi tydeleg gruppe, med episodeoversikt ved opning.

For kvar serie bruker appen Seerr sine seriedetaljar til å finne neste episode og relevante sesongar, og hentar episodeoversikta for sesongar med kjende datoar i vindauget. «Neste episode» åleine er ikkje nok til ein full kalender. Dersom ei sesonghenting feilar, kan den kjende neste episoden visast, men resultatet skal merkjast som ufullstendig. Beta3 utelèt spesialsesong 0; eksplisitt følgje av spesialar er eit seinare tillegg.

Seerr leverer episodefelt som `airDate`, `seasonNumber` og `episodeNumber`, og ei eiga sesongrute. Dette er kontrollert i [Seerr sin episodemodell](https://github.com/seerr-team/seerr/blob/develop/server/models/Tv.ts) og [sesong-API-et](https://docs.seerr.dev/api/get-season-details-and-episode-list/). Tenestekoden stadfestar moglegheita; faktisk datadekning i brukarens installasjon må prøvast i første gjennomføringssteg.

Dato utan klokkeslett blir lagra som `LocalDate`. Appen skal ikkje lage eit klokkeslett, flytte datoen gjennom UTC eller vise ei sekundnedteljing. «Premiere i dag» fortel kva dato metadata oppgir, utan å påstå at episoden alt er utgjeven eller kjem på denne tenaren i dag. Ukjende datoar står i følgjeoversikta og får ingen kalenderplass. Ei avslutta serie utan nye annonserte episodar skaper ingen tomme kort.

Datoendringar erstattar tidlegare hending med same identitet. Ei utgått premieredato skal ikkje bli ståande som framtidig hending berre fordi episoden manglar i biblioteket. «Premiere», «førespurd» og «kan spelast» er tre ulike opplysningar.

## Film og nyleg tilgjengeleg innhald

For film bruker «Kjem snart» berre utgjevingstype Digital, type 4, frå Seerr sitt `releases`-felt. Kino, festival, fysisk utgjeving og generelt `releaseDate` er ikkje reserveverdiar for digital dato. [TMDB definerer desse utgjevingstypane](https://developer.themoviedb.org/reference/movie-release-dates), og [Seerr vidareformidlar filmdatoane](https://github.com/seerr-team/seerr/blob/develop/server/models/Movie.ts).

Første kjende digitale utgjeving på tvers av regionar er datoen, i tråd med dagens Spole-regel. Kortet blir merkt «Digital premiere»; det lovar ikkje norsk strøymetilgjenge. Region blir behalden i datamodellen og forklart i detaljane. Ingen seinare regional nyutgjeving skal få ein gammal film til å sjå ny ut. Den eksisterande aldersregelen for gjenutgjevingar blir bevart.

«Nyleg tilgjengeleg» beheld 28 dagar bakover og krev stadfesta, tillaten bibliotekskopi. For film må digital dato vere innanfor vindauget. For episodar blir faktisk premieredato brukt. Datoen fila vart lagt til blir berre brukt til «Nyleg lagt til», ikkje som utgjevingsdato. Datooppslag skal ikkje avgrensast til éi første side av biblioteket eller velje kandidatane utelukkande etter kinopremiereår.

Det skal vere mogleg å sjå alle aktuelle titlar gjennom paginering og mellomlager. Ei teknisk grense på samtidige kall skal ikkje stille kaste bort kalenderkandidatar eller relevante bibliotekfilmar.

## Identitet og bibliotektilgjenge

Ein tittel blir identifisert med medietype og TMDB-ID. Ein episode bruker i tillegg sesong- og episodenummer. Namn og årstal er aldri bevis på samsvar. Manglande TMDB-ID kan løysast gjennom eintydige eksterne ID-ar dersom Seerr støttar det i den aktuelle installasjonen; elles blir tittelen merkt med manglande datagrunnlag og halden utanfor datert kalender.

Den nye hendingsmodellen skil mellom metadataidentitet, datokjelde, datotype, region, lokale biblioteksreferansar og førespurnadsstatus. Kjelde skal ikkje brukast som snarveg til medietype. Alle lenkjer til avspeling krev ein ekte, tillaten Jellyfin-/Emby-ID; Seerr-ID blir aldri ein oppdikta avspelings-ID.

Like hendingar blir dedupliserte med eksterne ID-ar. Dersom same tittel finst i både Jellyfin og Emby, får kalenderhendinga eitt kort med dei tillatne bibliotekskopiane i detaljane. Dei ordinære biblioteksradene for Jellyfin og Emby held fram kvar for seg.

Seerr sin globale AVAILABLE-status gir ikkje i seg sjølv rett til å spele frå eit bestemt bibliotek. For ein kalenderpost skal «I biblioteket» og avspelingsknappen byggje på den innlogga brukarens bibliotek. Metadata kan visast for ein tittel brukaren sjølv har følgt eller førespurt når Seerr gir tilgang, sjølv om han enno ikkje finst lokalt.

## Førespurnader, framdrift og varsel

Seerr er den einaste kjelda for sending, godkjenning, avvising, kvote, førespurnadsstatus og rapportert nedlastingsframdrift. Standard- og 4K-status og valde sesongar blir haldne frå kvarandre. «Lastar ned» og prosent blir berre viste når Seerr returnerer matchande nedlastingspostar. Tom kø er ikkje bevis på ferdig innhald eller feil; godkjenning er ikkje bevis på nedlasting.

«Mine førespurnader» erstattar den direkte køflata på Heim/Aktivitet. Ingen generell Sonarr-/Radarr-kø, historie eller køhandling blir ombygd til ein skjult Seerr-proxy. Ved manglande framdrift står den siste stadfesta førespurnadsstatusen, med tidspunkt og eventuell oppdateringsfeil.

Eksisterande personlege klarvarsel held fram etter [førespurnadsreglane](REQUEST_FLOW.md). Desse varsla må skiljast frå nye kalenderhendingar: ei premieredato skal aldri utløyse klarvarsel. Der eit varsel bygger berre på Seerr sin tilgjengestatus, må ordlyden seie kva som er stadfesta; kalenderen kan ikkje omtolke det til stadfesta tilgjenge i brukarens bibliotek.

Biblioteknedlasting til telefon for offline-avspeling er ei anna funksjon enn tenarens nedlastingskø og blir bevart.

## Telefon, nettbrett og TV

«Kjem snart» får ei eiga, flyttbar rad i «Tilpass framsida». Eksisterande synlegheits- og rekkjefølgjeval blir bevarte. Kortet viser tittel, episode eller digital premiere, og kort datotekst. Episodar bruker breitt bilde; filmar bruker cover. Kunst blir lasta i kortets faktiske storleik, med stabil plasshaldar.

Trykk opnar detaljane. «Sjå kalender» opnar datooversikta, og Tilbake frå detaljane går til same dato og same kort. TV må halde fokus ved oppdatering, paginering og endra datoar. Ved fjerna hending går fokus til næraste eksisterande kort. Sein metadata får ikkje flytte kort eller fokus. På mobil skal rad og oversikt fungere med touch og stor tekst.

Tom kalender, ukjent dato, manglande Seerr-innlogging, ufullstendig henting og nettverksfeil har ulike forklaringar. Ingen melding skal be brukaren kople til Sonarr/Radarr. Utan Seerr kan brukaren framleis opne bibliotek og spele.

## Innlasting og kontogrenser

Kalenderen skal publiserast uavhengig av bibliotek, Hero og førespurnadsstatus. Kaldstart og vanleg scrolling må ikkje vente på metadata for alle titlar. Mellomlagra kalender blir berre vist etter stadfesta gjeldande konto- og tilgangsomfang, med tidspunkt for sist vellukka oppdatering.

Seerr-innlogging og metadata blir kontrollerte uavhengig av henting av eigne førespurnader. Dersom ei førespurnadsrute feilar eller kontoen ikkje kan sende førespurnader, skal dette ikkje blokkere lesbare episode-/filmdatoar for bibliotekfrø og lokale følgjar.

Følgjande grenser er utgangspunkt for implementasjonen, og blir målte på ekte TV og telefon:

- Høgst fire samtidige metadataoppslag. Oppdater titlar med kjent nær dato først, deretter manuelle følgjar og andre frø.
- Metadata blir mellomlagra i seks timar. Manuell oppdatering kan fornye dei; vanleg Heim-oppdatering startar ikkje heile oppslagslista på nytt.
- Arbeidslista blir behandla i bolkar og held fram til alle kandidatar er behandla. Svar som ikkje er ferdige blir merkte ufullstendige, ikkje som ein endeleg tom kalender.
- Nye metadataresultat skal ikkje køyre om bilethenting eller bibliotekspørringar unødvendig. Fleire feil i same kjelde gir avgrensa nye forsøk og tilbakehald.

Kontoomfang inkluderer Spole-profil, verifiserte mediebrukarar, stabil tenaridentitet, Seerr-brukar, bibliotekval og dataschemaversjon. Cookie-fornying skal ikkje miste lokale følgjar. Kontobyte eller tilbakekalla tilgang stoppar gamle jobbar og resultat før dei kan bli publiserte. 401/403 utløyser innlogging eller tømming av det aktuelle tilgangen; metadata-cache er ikkje ei omgåing av dette.

Barnemodus får ikkje vaksenprofilens Seerr, kalender, følgjeval eller kø. Den eksisterande separate barneflata blir bevart, i tråd med [barnemodusplanen](BARNEMODUS_PLAN.md).

## Fjerning og oppgraderingsmigrering

Fjerninga skal gjennomførast i same ferdige leveranse som erstatninga. Det blir ikkje publisert ein mellomversjon som krev gamle tilkoplingar for å fylle kalenderen.

1. Køyr ein idempotent migrering før tilkoplingar, bakgrunnsarbeid og cached feed blir lasta. Fjern gamle Sonarr-/Radarr-adresser, namn, brukar-ID-ar, alternativ adresser, identitetar, lagra sist brukte adresser og krypterte nøklar for hovudprofil og registrerte barneprofilar. Migreringa bruker dei gamle lagringsnamna direkte, ikkje nye enum-verdiar.
2. Fjern gamle kalender- og kødata, kjeldefilter og utdatert datascope. Bevar Jellyfin-/Emby-/Seerr-kontoar, tema, språk, bibliotekval, framsiderekkjefølgje, offline-filer, avspelingshistorikk og personlege førespurnader/varsel. Oppgrader kalendercache-versjonen og tilpass nøkkel/fingerprint.
3. Ukjende eller utgåtte kjeldenamn skal droppast ved dekoding. `MediaSnapshotStore.kind()` har i dag Jellyfin som reserveverdi; denne åtferda må endrast før enum-verdiar blir fjerna. Gamle Radarr-/Sonarr-postar skal aldri bli omdøypte til Jellyfin.
4. Bevar `UPCOMING` som framsideval der det er mogleg. Gamle direkte køval blir fjerna; dei skal ikkje aktivere ei ny rad som brukaren ikkje har valt. Tidlegare «Kjem snart» får den nye datakjelda og ei kort eingongsforklaring om det personlege utvalet.
5. Fjern `RADARR` og `SONARR` frå `ServiceKind`, direkte kø-/kalenderklientar og parsarar, health/failover-ruter, autentiseringsgreiner, demoar, kjeldemerke, onboarding, filter, ikon og språkressursar. Gamle backup-/lagringsformat skal ikkje kunne opprette tilkoplingane att.
6. Oppdater gjeldande instruksar, appguide og dokumentasjon. Bevar daterte release- og verifiseringsrapportar som historikk. Berre migrering og testar for gammal lagring kan innehalde gamle kjeldestrengar i køyrbar kode.

Migreringa skal tole avbrot og gjentaking. Kvitteringa blir skriven først etter vellukka opprydding. Ho skal aldri tømme alle appdata eller slette ei delt krypteringsnøkkel for å fjerne to tenestepostar.

## Gjennomføring i fast rekkjefølgje

| Steg | Leveranse | Ferdig når |
| --- | --- | --- |
| 1 | Lesetest av Seerr-kontrakten på eksisterande kontoar. | Seriedetaljar, relevant sesong, filmdatoar og eigne førespurnader er kontrollerte med admin og ordinær konto. Manglande/avviste felt har ein definert feiltilstand. Ingen ekte førespurnad blir sendt som del av lesetesten. |
| 2 | Hendingsmodell, personlege frø, lokalt følgjeval og kalenderrepository. | Episodevindauge, filmdatoar, ekstern identitet, bibliotektilgjenge, deduplisering, paginering og kontoavgrensing har regeltestar. |
| 3 | Integrert Heim, kalender og detaljflyt. | Telefon/TV har fungerande rad, datooversikt, følgjehandling, stabil lasting og Tilbake/fokus. Skjult rad stoppar unødvendige kalenderoppslag. |
| 4 | Personleg førespurnadsstatus erstattar direkte kø. | Status/prosent kjem berre frå matchande Seerr-data; eksisterande varsel og offline-nedlasting er bevarte. |
| 5 | Migrering og full kodefjerning. | Oppgradering frå 1.0.0-beta2 bevarer resten av appen; inga støtte for dei to tenestene står att utanom lagringsmigrering. |
| 6 | Samla verifisering og releasekandidat. | Krava nedanfor er dokumenterte for den endelege kandidaten. Publisering følgjer vanleg releaseflyt når ho er bestilt. |

Ei manglande dato for éin tittel stoppar ikkje arbeidet. Dersom installasjonen ikkje tilbyr eit nødvendig endepunkt, skal mangelen rettast eller dokumenterast som avgrensa støtte før migreringa blir godkjend. Ein vellykka kodegjennomgang av Seerr åleine godkjenner ikkje steg 1.

## Kodeområde som må endrast

Dette er kontrollert mot arbeidsmappa 5. oktober 2026, og er startlista for implementasjonen:

- `data/model/Models.kt`, `HomeLayout.kt`: tenestetypar, hendingsidentitet, datopresisjon, framsideval og gamle format.
- `data/network/ReleaseCatalogue.kt`, `UpcomingSeason.kt`, `ServiceClients.kt`, `ServicePayloadParser.kt`, `AccountProfileClient.kt`: gjenbruk av Seerr-datoar og sesongar, innlastingsgrenser, fjerning av `QueueServiceClient` og tenestegreiner. Dagens `SeerrReleaseClient` hentar berre eit avgrensa utval bibliotekfilmar; han er ikkje den nye personlege kalenderen enno.
- `data/repository/MediaSyncRepository.kt`, `MediaSnapshotStore.kt`, `ConnectionRepository.kt`, `AppPreferencesRepository.kt`, `RequestTrackingRepository.kt` og `data/repository/cache`: integrering, lokal følgjelagring, cache og migrering. Dagens `upcoming` blir fylt frå direkte kalenderklientar; denne ruta må erstattast.
- `ui/HomeFeedCoordinator.kt`, `ReelstackViewModel.kt`, `ReelstackSheets.kt`, `UpcomingCalendarSheet.kt`, `screens/HomeScreen.kt`, `ServicesSettings.kt`, `SecondaryScreens.kt`, `WelcomeScreen.kt`, `TvWelcomeScreen.kt`: framside, kalender, innstillingar, fokus, detaljar og onboarding.
- `ui/components/ServiceLogo.kt`, `ui/DemoContent.kt` og alle språkressursar: fjerning av merking og forklaringar. Exhaustive `ServiceKind`-greiner i spelar, offline, oppdatering og andre klientar må tilpassast utan åtferdsendring.

Eksisterande `ReleaseCatalogueTest`, `ServiceClientsTest`, `ServicePayloadParserTest`, `ViewerAccessTest`, `HomeFeedRecoveryTest`, `CalendarAndSheetTest`, `MediaSnapshotStoreTest`, `ViewerExperienceTest` og `TvSetupTest` skal tilpassast, saman med eigne migrerings- og kalenderregeltestar.

## Obligatorisk verifisering

- [ ] Vanleg Jellyfin + Seerr-brukar får personleg «Kjem snart» utan nøklar eller kall til Sonarr/Radarr. Ny installasjon treng berre dei støtta kontoane.
- [ ] Episode seinare i same sesong, ny sesong, fleire episodar same dag, spesialepisode, ukjent/flytta dato og avslutta serie blir handterte utan duplikat eller oppdikta informasjon.
- [ ] Grenser i dag / 28 dagar fram og 28 dagar tilbake, midnatt og ulike tidssoner endrar ikkje date-only metadata.
- [ ] Film med berre kinodato eller fysisk dato får ingen digital kalenderdato. Sein regional nyutgjeving får ikkje film til å framstå som ny. Metadataåret får ikkje skjule relevante digitale utgjevingar.
- [ ] Same eksterne tittel i Jellyfin og Emby får éi kalenderhending; like namn med ulike ID-ar blir haldne skilde. Manglande ID blir ikkje gjetta.
- [ ] Ingen Play-handling utan eiga tillaten bibliotekskopi. Seerr-status, annan sesong, 4K-status og utgjevingsdato blir ikkje blanda.
- [ ] Fleire sider og meir enn 60 filmkandidatar/30 kalenderhendingar blir ikkje stille avkorta. Feil i éin tittel eller éi kjelde stoppar ikkje dei andre.
- [ ] Kontobyte, utlogging, ny cookie, failover, 401/403 og barneprofil gir ingen gamle hendingar eller følgjeval frå ein annan tilgang.
- [ ] Migrering frå ekte gammalt lagringsformat er testa, også med registrerte profilar, avbrot og gjentaking. Ukjent service blir droppa, aldri mappa til Jellyfin.
- [ ] Nettverkslogg/testtransport stadfestar null direkte Sonarr-/Radarr-kall i både synleg app, oppstart, oppdatering, bakgrunnsarbeid og feiltilstandar. Ingen tidlegare lagra nøklar står att etter migrering.
- [ ] Heim, detaljar, kalender, innstillingar og førespurnader er gjennomgått på telefon og Google TV med touch/fjernkontroll, nynorsk/engelsk og skriftstorleik 2.0. Fokus og tilbakeføring er bevarte gjennom sein metadata.
- [ ] Einingstestar, relevante Android-testar, lint og releasebygg er grøne. Automatiske testar bruker isolert instrumenteringseining; ekte kontoar blir ikkje nullstilte eller brukte til test-POST.
- [ ] Signert oppgradering frå siste publiserte beta er kontrollert på eksisterande mobil-/TV-profilar, med bevarte kontoar og innstillingar. Verifiseringsrapport skil lesetestar, syntetiske testar, emulator og fysisk maskinvare.

Lista over er den opphavlege akseptanselista og skal ikkje tolkast som stadfesting av alle scenario på fysisk maskinvare eller alle kontoar. Beta3-resultat og faktiske avgrensingar er dokumenterte i verifiseringsrapporten. Historiske implementasjonsomtalar lenger opp viser utgangspunktet før kodefjerninga.
