# Samla Oppdag: spesifikasjon · 16. oktober 2026

Oppdag og Aktivitet blir éin stad. Godkjent konsept:
https://claude.ai/artifact/9JRrmTt4mkBsHfr5z3Jms5 (fire TV- og fire telefonskjermar). Bakgrunnen er
samanlikninga med Wholphin i `C:/JellyBin/reports/Spole mot Jellyfin Emby og Wholphin.md`.

## Mål

- Ein stad for alt som kjem frå Seerr: det du har bede om, det som er populært, det som kjem, folk.
- Reine linjer: ingen knappar eller tekst under plakatane, ingen farga flater på knappar.
- Seerr skal kjennast like raskt som i Wholphin: rader lastar kvar for seg og parallelt.
- Folk med bilete og éi personside.
- Førespurnaden får **Utgåve**, **Kvalitet** og **Lagringsstad** når Seerr gir brukaren lov.

## Avgjerder frå godkjenninga (16. oktober)

- Aktivitet er ikkje lenger ei fane. Tenaraktiviteten for administratorar blir fana **Tenaren** i
  «Dine førespurnader», synleg berre for administratorar.
- Folk har namnet under biletet; det er den einaste teksten under ein illustrasjon.
- «Send førespurnad» er lime tekst med pil, utan flate.

## Visuelle reglar for heile Oppdag

- Plakatar i rader har berre bilete. Tittel, år, status og samandrag står i toppfeltet (TV) eller på
  tittelsida (telefon).
- Handlingar er tekst med ikon. Fokus er den vanlege kvite ringen (`focusOutline`).
- Lime berre på: valt fane i menyen, «Send førespurnad». Framdrift er ei 2–3 dp tynn line i
  tekstfarge; status er tekst. Ingen typemerke, statusikon eller fargebrikker på korta.
- Skilje mellom listeelement er ei 1 dp line (tekstfarge 8 %). Ingen kort, ingen kantar.
- Statusordlista er uendra: Førespurd → Lastar ned → Blir lagt i biblioteket → I biblioteket, og
  Avvist, Treng tilsyn, Status ukjend.

## Navigasjon

- `AppTab.ACTIVITY` blir fjerna. Menyen blir `HOME, LIBRARY, DISCOVER, SETTINGS` (pluss Nedlastingar
  som før). Lagra `menu_order`/`menu_hidden` med «ACTIVITY» blir reinska av `visibleMenu`, som alt
  dropper ukjende id-ar. `tabs`, `TvMenuSettings.names` og `DEFAULT_MENU` må vere like.
- Varsel med `open_requests` opnar Oppdag med «Dine førespurnader» open.
- Etter sending blir brukaren verande på tittelsida; statuslina der blir «Førespurd» med ein gong.
  `REQUEST_FLOW.md` og `LAYOUT.md` blir oppdaterte.
- Sporinga av førespurnader går kvart 30. sekund når Oppdag eller «Dine førespurnader» er open,
  elles kvart 300. sekund som før.

## Oppdag-sida

Rekkjefølgje, ovanfrå:

1. **Søk.** Telefon: søkjefelt øvst. TV: eit søkjeikon øvst til høgre i toppfeltet, nådd med opp;
   det opnar det globale søket.
2. **Dine førespurnader.** Berre eigne. Dei som er på veg først, så dei sist ferdige; høgst tolv.
   Telefon: tre liner (lite omslag, tittel, status, framdriftsline) og «Alle N». TV: plakatrad med
   framdriftsline nedst i plakaten og «Alle N» som tekst til slutt. Rada er borte når lista er tom.
3. **Populært no** (`discover/trending`).
4. **Filmar som kjem** (`discover/movies/upcoming`).
5. **Seriar som kjem** (`discover/tv/upcoming`).
6. **Folk.** Dei to første rollene frå kvar av dei seks første titlane i «Populært no», utan
   duplikat, høgst tolv. Runde bilete med namn under.
7. **Sjangrar** (`discover/genreslider/movie` og `/tv`). Bildekort med sjangernamnet inni.

Kvar rad lastar sjølvstendig og parallelt med dei andre, med skjelett i same storleik til ho kjem.
Ei rad som feilar, blir borte; éi roleg line nedst seier at Seerr ikkje svarte og gir «Prøv igjen».
Siste element i ei rad er «Alle», som opnar eit sidedelt rutenett for rada (utan filter i denne
omgangen).

**TV-toppfeltet** viser tittelen som har fokus: namn, «år · lengd · sjanger · vurdering», statuslina
og to liner samandrag, med bakgrunnsbiletet bak. Detaljar blir henta når kortet får fokus og lagra i
ein minnecache med 100 element. Utan fokus på ein tittel (til dømes på Folk) står førre tittel att.

**Telefon** har ingen fokus. Eit trykk på ein plakat opnar tittelsida.

## Dine førespurnader

Erstattar Aktivitet-skjermen og `RequestHistoryScreen`.

- Faner som tekst med strek under vald fane: **Alle N · På veg N · Klare N**, og **Tenaren** for
  administratorar (ADMIN-biten frå fersk `/auth/me`, som i `VIEWER_ACCESS.md`).
- Kvar line: omslag (48 × 72), tittel, statusline, framdriftsline for det som er på veg. Trykk opnar
  tittelsida, der «Trekk tilbake» og varsel per sesong ligg. Ingen knappar i lista.
- «Alle» hentar eldre førespurnader side for side når lista nærmar seg slutten (erstattar historikken).
- Manglande varselløyve og sporingsfeil står som éi roleg line øvst.
- **Tenaren:** tenaraktiviteten gruppert etter dag, som liner: tid · tittel · hending · teneste.
  «Treng tilsyn» (uløyste importar) står òg her.

## Tittelsida for Seerr-titlar

- Handlingslina: **Be om** (eller **Spel av** når tittelen er i biblioteket med ekte element-ID),
  **Be om fleire sesongar**, **Trekk tilbake** for eigen ventande førespurnad, **Trailer**. Tekst med
  ikon; den første har startfokus på TV.
- Statusline under metadata, som tekst.
- **Folk:** rolleliste frå Seerr med person-ID og bilete. Trykk opnar personsida.
- **Liknande titlar** (`{movie|tv}/{id}/similar`) og **Tilrådd ut frå denne**
  (`{movie|tv}/{id}/recommendations`) som plakatrader.

## Personsida

- `person/{id}`: bilete, namn, «avdeling · fødd [dato]», biografi i to liner med «Les meir».
- `person/{id}/combined_credits`: skodespel og stab slått saman, utan duplikat, nyaste først.
  **I biblioteket ditt** er dei med `mediaInfo`-status tilgjengeleg eller delvis; **Meir med [namn]**
  er resten.
- Same side frå tittelsida, søket og Folk-rada. På TV viser rad-overskrifta tittelen som har fokus.

## Søk

Seerr-søket tek med personar (`mediaType == "person"`) med `profilePath`. Folk står øvst som liner med
rundt bilete, «Skodespelar · N i biblioteket ditt» når talet er kjent, og så titlane som rutenett utan
tekst. Gjeld både Oppdag på telefon og det globale søket.

## Be om-arket

Rader skilde med liner, i denne rekkjefølgja:

- **Utgåve: Standard · 4K.** Berre når brukaren har `REQUEST_4K` (1024), `REQUEST_4K_MOVIE` (2048)
  eller `REQUEST_4K_TV` (4096) for typen, og Seerr har ein 4K-tenar for typen.
- **Kvalitet** og **Lagringsstad.** Berre når brukaren har `REQUEST_ADVANCED` (8192),
  `MANAGE_REQUESTS` (16) eller `ADMIN` (2). Data frå `service/{radarr|sonarr}` (standardtenaren for
  valt utgåve) og `service/{radarr|sonarr}/{id}`: profilar og rotmapper med ledig plass.
  - Kvalitet viser profilnamnet; tenaren sitt `activeProfileId` får «vanleg val».
  - Lagringsstad viser siste mappenamn stort, «4,8 TB ledig» (SI-einingar, éin desimal) og full sti
    dempa; `activeDirectory` får «vanleg val».
  - Eit trykk opnar ei vallist i same ark.
- **Sesongar** for seriar, som i dag (ingen førehandsval, låste sesongar).
- **Varsle meg når han er klar**, på som standard.
- **Send førespurnad** som lime tekst med pil. **Avbryt** på TV som dempa tekst.

POST `request` får `is4k`, `serverId`, `profileId` og `rootFolder` berre når arket viste dei. Utan
avanserte rettar og utan 4K er arket som i dag. Byte av utgåve byter lister.

## Data og fart

- Ny klasse `SeerrDiscoverClient` i `data/network` med: trending, upcoming movies/tv, genreslider,
  genre-rutenett, similar, recommendations, person, combined_credits, service-lister. Han brukar same
  transport og autentiseringshovud som `SeerrServiceClient`, og éin instans ligg i `AppContainer`, så
  cachen er éin.
- `CastMember` får `seerrPersonId`; parseren tek han med. Søkjeparseren tek med personar.
- Bilete: plakatar `w342`, bakgrunnar `w1280`, folk `w185`.
- Kvar rad er ein eigen tilstand (laster / klar / feil) i `DiscoverUiState`. Nye Seerr-titlar må med i
  `knownDiscoverMedia`, elles verkar ikkje detaljar og førespurnader.
- `openActivityDetails` skal ikkje leggje titlar inn i `discover`-lista lenger.

## Utanfor denne omgangen

- Filter og sortering i «Alle»-rutenetta, «Request as», taggar, val av ikkje-standard tenar.
- Personside for Jellyfin-/Emby-folk utan TMDB-ID (dagens bibliotekutfalding blir verande).
- Admin-handlingar på andre sine førespurnader.

## Gjennomføring i fire steg

1. **Navigasjon og Dine førespurnader:** ACTIVITY ut, varsel og sending, ny liste med faner og
   Tenaren, polling.
2. **Oppdag-radene:** klient, parallelle rader, TV-toppfelt, telefonoppsett, søk med folk.
3. **Tittelside og personside:** handlingsline, Folk med Seerr-ID, Liknande, Tilrådd, personside.
4. **Be om-arket:** Utgåve, Kvalitet, Lagringsstad, rettar, POST-felt.

Kvart steg er eigne commits med testar; éi utgåve når alle fire er ferdige.

## Testing

- Einingstestar: parsing av person, combined_credits, service-lister med ledig plass, søk med personar;
  rettsstyring av Utgåve/Kvalitet/Lagringsstad; POST-kropp; formatering av ledig plass; menyreinsing
  av «ACTIVITY»; djuplenkje frå varsel; at kvar rad lastar sjølvstendig og at ei feilande rad ikkje
  stoppar dei andre.
- Android-testar på isolert TV 5566: oppdaterte navigasjons-, Discover-, Activity- og
  førespurnadstestar, og nye for faner, Tenaren, personside og arket. Testar som peikar på
  `wide-tab-ACTIVITY`, `menu-*-ACTIVITY`, `activity-*` og `history-*` blir skrivne om eller fjerna.
- Skriftstorleik 2.0 på telefon og TV. Ekte data på TV-profil 5564 (ingen instrumentering der).

## Avvik under bygginga

- **Søk:** folk står øvst, men høgst tre liner, så titlane under er synlege. Lina viser kva personen er kjend
  for på lesaren sitt språk («Skodespelar», «Regissør» …); talet «N i biblioteket ditt» er ikkje med, fordi
  søket ikkje gir det utan eit ekstra kall per person.
- **TV-toppfeltet** står fast over radene i staden for å vere første element i lista, elles rulla det bort ved
  tredje rad. Lengd og sjangrar blir henta når ein plakat har hatt fokus i 0,3 sekund, éin gong per tittel.
- **Tilbake** mellom tittelside og personside går eitt steg kvar veg; noko som blir opna frå andre stader,
  startar på nytt.
- **4K for ein film som alt finst i standard** kan ikkje bestillast frå appen enno: inngangen til arket krev at
  standardutgåva kan bestillast. Logikken i arket er klar for det.
- **Personsida** formaterer fødselsdatoen som lang dato på lesaren sitt språk.
