# Ytelsesplan for Spole – 22. september 2026

Grunnlag: kodegjennomgang av `main` på `418d872` (0.18.0-beta6), med vekt på det som gjer appen treg
eller svak i bruk. Ingenting her er målt på emulator enno. Tala for rundtur og ventetid er rekna ut
frå koden og må stadfestast i fase 0 før og etter kvar endring.

Dette er ei utfylling av `REVIEW_2026-09-22.md`, som tok for seg struktur, tryggleik og design.
Punkta der blir ikkje gjentekne her.

## Status 23. september 2026 (0.18.0-beta7)

| ID | Status | Kva vart gjort |
|---|---|---|
| Nedlasting | ✅ | Forhandlar med eininga sine kapabilitetar, utan bilettekst og utan straumingsgrense (`offline/OfflineNegotiation.kt`); offline-spelaren har FFmpeg-lyd; jobbar følgjer kontoen til den andre adressa; tenesta blir ikkje starta frå `Application.onCreate` |
| Y-0 | ✅ delvis | `SpolePerf`-logg i debug: kvart HTTP-kall, første/alle heimerader, detaljside, avspeling klar. Referansemåling med ekte data står att |
| Y-1 | ✅ | Dekrypterte verdiar blir cacha på chifferteksten i `EncryptedTokenStore`; ingen invalidering trengst |
| Y-2 | ✅ | `feed()` hentar rader og bibliotek parallelt, maks 6 kall per tenar (`ParallelRequests.kt`). Falske transportar får eitt kall om gongen |
| Y-3 | ⏳ | Radene kjem framleis per teneste, ikkje per rad |
| Y-4 | ✅ | Avspelingsløkka er nøkla på profil og startar ved kald start; sesjonskanalen blir halden open |
| Y-5 | ✅ | Identitetar frå siste fulle oppfrisking blir gjenbrukte i 5 min |
| Y-6 | ✅ | Kontosjekk, profilar og tittel parallelt; kapabilitetsrapporten ventar ingen på |
| Y-7 | ✅ | Rask banking på adressene før profil og straum; ingen ny freistnad ved tidsavbrot på tilkopling |
| Y-8a | ✅ | Rota lyttar berre på `kidShell` |
| Y-9 | ✅ | Nedlastingsfana ventar på signal frå `DownloadManager` når ingenting overfører |
| Y-10 | ✅ | «Neste episode» blir henta samstundes med sesongane |
| Y-11 | ✅ | Breie kort ber om 1080 px; førehandslasting og oppstartsvarming i kortstorleik og med rett URL |
| Y-12 | ✅ delvis | `profileinstaller` og biblioteksprofilar er alt med i release-bygget; eigen profil for Spole-koden står att |
| Y-14 | ✅ delvis | Biblioteka og tenarane blir henta parallelt, og visningslista éin gong; cache i Room står att |
| Y-15 | ✅ | Tilrådingar startar med ein gong; detaljoppslag for førespurnader parallelt |
| Y-16 | ➖ | Ikkje endra: bakgrunnsjobben fyller snapshotet heimeskjermen startar frå, også tilrådingane |

## Oppfølging 26. september 2026: framsida skal ikkje hoppe

Brukaren melde at innlastinga på framsida framleis ikkje var god. Gjennomgangen fann at mykje av
det som kjennest tregt, er at sida bygst om ved kvar oppfrisking, ikkje at kalla er trege:

| Endring | Kvifor |
|---|---|
| `loadedSources`: berre ei rad som aldri har lasta, får skjelett | Tomme rader fekk skjelett ved kvar oppfrisking og mista det att, og sida hoppa to gonger |
| «Kjem snart» får berre skjelett når Radarr/Sonarr er kopla til | Utan dei kollapsa eit 226 dp skjelett til éi linje |
| Grenser per tenar (`HOME_RESUME_PER_SERVER`, `HOME_ROW_PER_SERVER`) | Grensa på den samanslåtte lista gjorde «Hald fram» 6 → 12 → 6 kort i éi oppfrisking |
| Fast rekkjefølgje (`mergeHomeRows`) i kvar tenaroppdatering | Rekkjefølgja snudde seg per oppdatering, og helten bytte tittel medan han var synleg |
| Helten følgjer ein tittel, ikkje ein plass | Same grunn, frå sida til nettbrett/TV |
| Nytt forsøk ved feil: 1 → 2 → 4 … 30 min, åtvaringar frå 5 min | Ein tenar som låg nede, eller ei åtvaring som eigentleg var ein rett, gav full oppfrisking kvart minutt |
| Radene kjem på skjermen før Room-lagringa | Snapshot-skrivinga låg mellom siste svar og skjermen |
| Biblioteksval i Bibliotek-fana tømmer ikkje framsida | Framsida brukar sitt eige val (sjå `HOME_LAYOUT.md`) |

Står att frå same gjennomgang: sesjonar («Spelar no») kjem framleis med heile oppfriskinga, Seerr-
og kalenderradene ventar på katalogoppslaga, og kvar retur til appen gjer ei full oppfrisking.

## Samandrag

Appen er ikkje treg fordi Compose eller biletlastinga er dårleg. Han er treg fordi nettverkskalla
går **etter kvarandre** der dei kunne gått samstundes, og fordi nokre få ting blir gjort om att langt
oftare enn nødvendig. Dei fem største funna:

| # | Funn | Kvar | Verknad |
|---|---|---|---|
| 1 | Heimestraumen frå Jellyfin/Emby hentar alt i serie, bibliotek for bibliotek | `ServiceClients.kt:342` | Rundt 20 kall etter kvarandre med fire bibliotek. Den største årsaka til treg heimeskjerm |
| 2 | Når heimeadressa ikkje svarar, går det lang tid før appen prøver den alternative adressa | `MediaSyncRepository.kt:463`, `HttpTransport.kt:59` | Anslått 30–45 s med tom heimeskjerm når du går ut av heimenettet |
| 3 | Kvart plakatbilete dekrypterer tokenet via Android Keystore på hovudtråden | `MediaAuthHeaders.kt:34` → `ConnectionRepository.kt:60` | Hakking når du blar i rader, særleg på TV |
| 4 | Avspelingsløkka startar på nytt kvar gong ein popup opnar eller fana skiftar | `ReelstackApp.kt:180` | Ny WebSocket og 4–5 ekstra kall for kvar detaljside du opnar |
| 5 | Avspelingsstart har seks kall etter kvarandre, to av dei dublettar | `JellyfinPlayerModel.kt:486`, `JellyfinPlayback.kt:261` | Lengre tid frå «Spel av» til bilete |

## Fase 0 – Mål først (½ dag)

Utan tal veit vi ikkje kva som hjelpte. Tre små tiltak:

- **Y-0a Tidslogg per kall.** Logg metode, sti utan spørjestreng, status og millisekund i
  `HttpTransport.request` under taggen `SpolePerf`, berre i debug-bygg. Aldri token, vert eller full URL.
- **Y-0b Milepælar.** Logg tida frå `HomeFeedCoordinator.refresh` til første ekte rad, til alle rader,
  frå trykk på kort til ferdig detaljside, og frå «Spel av» til `STATE_READY`.
- **Y-0c Referansemåling.** Køyr på `emulator-5560` (mobil) og `emulator-5564` (TV) med ekte data:
  kald start ×3, oppfrisking av heimeskjermen ×3, opning av detaljside for ein serie ×3, og avspelingsstart ×3.
  Hugs: ikkje instrumentering på desse profilane, berre `install -r` og logcat.
  Skriv tala inn nedst i dette dokumentet.

## Fase 1 – Raske, trygge gevinstar (1–2 dagar)

### Y-1 Keystore-dekryptering på hovudtråden 🔴
**Problem:** `ConnectionRepository.get()` dekrypterer tokenet med AES-GCM via Android Keystore
(IPC til keystore-tenesta) kvar gong han blir kalla. `MediaAuthHeaders.forUrl()` kallar han for kvart
nytt bilete i `MediaArtwork` under komposisjon. `initialState()` kallar `list()` éin gong for den
aktive profilen og éin gong for kvar profil (`ReelstackViewModel.kt:2929–2942`), altså 5 × (1 + tal
profilar) dekrypteringar før første bilete. `WatchNextSync`, `NowPlayingWidget` og `mediaFingerprint`
gjer det same.
**Tiltak:** Hald dekrypterte token i minnet i `ConnectionRepository`, nøkla på prefiks, og tøm
eller oppdater cachen ved `save`/`delete`/`signOut`/profilbyte. Lagringa på disk blir som før.
**Akseptanse:** ingen `Cipher.doFinal` på hovudtråden etter første oppslag (sjå med StrictMode eller
tidslogg); eksisterande tester for utlogging og profilbyte er framleis grøne; ny test: token blir borte
frå cachen ved utlogging.

### Y-4 Avspelingsløkka startar på nytt ved kvar popup 🔴
**Problem:** `LaunchedEffect(lifecycleOwner, activeProfileId, selectedTab, activeSheet)` i
`ReelstackApp.kt:180`. Kvar gong ein detaljpopup opnar eller lukkar seg, blir effekten avbroten:
`closeSessionChannel()` og så `openSessionChannel()` (ny WebSocket), og `retryIncompleteHomeFeed()` +
`refreshPlayback()` køyrer straks.
**Tiltak:** Nøkkel berre på `lifecycleOwner` og `activeProfileId`. Les fane og popup inne i løkka
(slik sporinga av førespurnader alt gjer rett under), og hald kanalen open så lenge appen er i
`RESUMED`.
**Akseptanse:** å opne og lukke ti detaljsider gir null nye WebSocket-tilkoplingar og null ekstra
`Sessions`-kall i `SpolePerf`-loggen.

### Y-5 Avspelingssjekken hentar identitet på nytt kvart 5. sekund 🟡
**Problem:** `MediaSyncRepository.refreshPlayback()` hentar kontoprofilen for Seerr, Jellyfin og Emby
før kvar sesjonssjekk. Når noko spelar, er intervallet 5 s: tre profilkall og to sesjonskall kvart
5. sekund, heile tida.
**Tiltak:** Gjenbruk `ViewerAccess` frå siste fulle oppfrisking (nøkla på `mediaFingerprint`) i
inntil 10 minutt, eller til eit kall svarar 401/403. Sjølve tilgangsregelen er uendra.
**Akseptanse:** under avspeling berre `Sessions`-kall i loggen mellom fulle oppfriskingar;
eksisterande `ViewerAccess`-testar er grøne; ny test: ein vanleg brukar ser framleis berre eigne økter.

### Y-8a Heile appen blir teikna på nytt ved kvar tilstandsendring 🟡
**Problem:** `MainActivity.kt:49` samlar heile `uiState` berre for å lese `isKidMode`. Dermed blir
rota rekomponert ved kvar endring, også nedlastingsframdrift, snackbar og søk.
**Tiltak:** `uiState.map { it.isKidMode }.distinctUntilChanged()` og samle den.
**Akseptanse:** Layout Inspector viser at rotinnhaldet ikkje blir rekomponert når nedlastingsframdrifta endrar seg.

### Y-9 Nedlastingsfana spør databasen kvart sekund 🟡
**Problem:** `ReelstackViewModel.kt:459` les heile nedlastingsoversikta frå Media3-databasen kvart
sekund og skriv ho inn i den globale tilstanden.
**Tiltak:** Lytt på `DownloadManager.Listener` (endringar) og hent framdrift i det tempoet UI-et
treng (1 s) berre når minst éi nedlasting er aktiv. Legg nedlastingstilstanden i eigen `StateFlow`,
ikkje i `ReelstackUiState`.
**Akseptanse:** ingen databasespørjingar når ingenting lastar ned; framdriftslinja oppdaterer seg som før.

## Fase 2 – Dei store: nettverket (3–4 dagar)

### Y-2 Parallell heimestraum frå Jellyfin/Emby 🔴
**Problem:** `MediaServerClient.feed()` gjer desse i serie: økter → bibliotekvisningar → nye filmar
(éin gong per bibliotek) → nye episodar (per bibliotek) → hald fram (per bibliotek) → nye utgjevingar
(per bibliotek og type) → neste episode (per bibliotek) → favorittar (per bibliotek). Med fire
bibliotek blir det rundt 20 kall etter kvarandre. Over mobilnett med 100–150 ms rundtur går 2–3 s berre
til venting, før tenaren har gjort noko arbeid. `onLibraryReady` fyrer først når *alt* er ferdig, så
radene kjem ikkje éi og éi.
**Tiltak:**
1. Hent `sessions` og `libraryViews` samstundes.
2. Når visningane er klare, køyr dei seks radtypane samstundes, og bibliotek-kalla inne i kvar
   type samstundes, med eit tak på om lag 6 samstundes kall per tenar, slik at ein liten
   heimetenar ikkje blir overkøyrd. Bruk `Dispatchers.IO.limitedParallelism(6)` eller ein `Semaphore`.
3. Send kvar rad til UI-et så snart ho er klar (`onLibraryReady` per rad, ikkje per teneste).
4. Hald rekkefølgja og interleave-logikken uendra. Berre tidspunktet skal endre seg.
**Merk:** `FakeTransport` i testane må vere trådsikker. Ein test må sikre at eit ekskludert
barnebibliotek framleis aldri blir med, også når kalla går parallelt.
**Akseptanse:** i `SpolePerf` er djupna på kallkjeda for éi teneste 3 (økter/visningar → rader → neste
episode-datoar), ikkje ~20. Tid til alle rader går ned med minst 50 % over mobilnett på 5560.

### Y-7 Treg overgang til alternativ adresse 🔴
**Problem:** `fetchWithFailover()` prøver den alternative adressa først når *heile* straumen har
feila mot den første. Mot ei lokal adresse som ikkje svarar (du er ute og brukar mobilnett), ventar
kvart GET-kall 7 s på tilkopling, og `HttpTransport.get` prøver ein gong til. Økter, visningar og
`verifyConnection` feilar kvar for seg før appen gir opp, og det same skjer for Seerr, Radarr og
Sonarr. Anslag: 30–45 s før heimeskjermen får innhald. Adressa blir flytta fram etterpå, så det skjer
éin gong per nettverksbyte, men det er akkurat då brukaren opnar appen.
**Tiltak:**
1. Rask sjekk før straumen: `System/Info/Public` (Jellyfin/Emby) eller `api/v1/status` (Seerr) mot
   begge adressene samstundes med 2 s tidsavbrot. Bruk den som svarar først, og prioriter den lagra
   adressa om begge svarar innan kort tid.
2. Lytt på `ConnectivityManager.NetworkCallback`: når nettverket skiftar, nullstill valet slik at
   sjekken køyrer på nytt.
3. Ikkje prøv eit GET-kall på nytt ved `ConnectException`/`SocketTimeoutException` på tilkoplinga.
   Det hjelper berre ved brotne tilkoplingar.
**Akseptanse:** med Wi-Fi av på 5560 (mobilnett eller ekstern adresse) kjem heimeskjermen med ekte
data innan 5 s når den lokale adressa står først; ny einingstest med ein transport som heng på den
første adressa.

### Y-6 Kortare avspelingsstart 🟡
**Problem:** `loadRoot()` gjer i serie: `verify` (`Users/Me`) → `announceCapabilities` (POST, blokkerer)
→ `accountProfileClient.load` (`Users/Me` ein gong til) → Seerr `auth/me` → `item` → `PlaybackInfo`.
**Tiltak:** Bygg `ServiceAccount` frå same `Users/Me`-svar som `verify` les. Send `Sessions/Capabilities`
utan å vente på svaret. Hent Seerr-identiteten og `item` samstundes med `verify`. Kontrollen av
at det er brukaren sin eigen konto, skal vere uendra, og avspelinga skal ikkje starte før han er
gjennomført.
**Akseptanse:** 3 kall i serie før `PlaybackInfo` blir til 1; tida frå «Spel av» til `STATE_READY` går
ned med minst éin rundtur; testen for «byta konto» feilar framleis rett.

### Y-15 Seerr og tilrådingar ventar på alt anna 🟢
**Problem:** Tilrådingslista (`MediaSyncRepository.kt:196`) og utgjevingskatalogen i Seerr blir henta
*etter* at alle tenestene har svara. Detaljoppslaga for førespurnader (`ServiceClients.kt:1263`) går
éin og éin.
**Tiltak:** Start tilrådingslista med ein gong. Køyr detaljoppslaga for førespurnader parallelt, med
eit tak på 4. Katalogen treng filmkandidatane frå Jellyfin/Emby og må framleis vente på dei.
**Akseptanse:** Seerr-radene kjem ikkje seinare enn den tregaste tenesta.

## Fase 3 – Flater som kjennest trege (2–3 dagar)

### Y-10 Detaljside for serie: fire steg i serie 🟡
**Problem:** sesongar → `seriesNextUp` (inntil 2 kall) → episodar → komande episodar
(`ReelstackViewModel.kt:1695`). Episodelista ventar på «neste episode» før ho i det heile tatt blir henta.
**Tiltak:** For ei episodeside veit vi alt kva sesong det gjeld, så hent episodane samstundes med
sesongane. For ein serie: hent `seriesNextUp` samstundes med sesongane, og vel sesong når begge er
klare.
**Akseptanse:** episodelista kjem éin til to rundturar tidlegare; popupen hoppar ikkje
(StableSheetDialog-krava i `AI_INSTRUCTIONS.md` gjeld).

### Y-11 Førehandslasta bilete blir dekoda i full storleik 🟡
**Problem:** `PrefetchRailArtwork.kt:45` lagar `ImageRequest` utan `size()`, så Coil dekodar i
original storleik (opptil 1920 px for backdrops) og legg bitmapen i minnecachen. `StartupReveal`
varmar med 640×360, som ikkje treng å stemme med korta.
**Tiltak:** Set same storleik som kortet brukar, eller bruk `memoryCachePolicy(DISABLED)` slik at
berre diskcachen blir varma. Hent storleiken frå éin felles funksjon som `MediaArtwork` òg brukar.
**Akseptanse:** minneprofilen under rask TV-navigering viser ingen bitmapar over kortstorleik frå
førehandslastinga.

### Y-12 Kald start 🟡
**Problem:** Det finst ingen baseline-profil, så Compose-koden blir JIT-kompilert ved første køyring
etter kvar installasjon. `StartupCover` viser alltid minst ~1,2 s (900 ms animasjon + 300 ms) sjølv
når cachen er klar på 100 ms.
**Tiltak:**
1. Legg til `androidx.profileinstaller` og ein baseline-profil for oppstart, heimeskjerm, rulling i
   rader og opning av detaljside. Lag han med Macrobenchmark på den isolerte emulatoren 5562.
2. Når den bufra heimeskjermen er klar før animasjonen er ferdig, kort ned til ein rask versjon. Dette
   er eit designval, så spør brukaren før du endrar sjølve uttrykket.
**Akseptanse:** tida til første bilete (`am start -W`, TotalTime) går ned målbart på 5564; ingen
endring i utsjånaden til oppstarten utan godkjenning.

### Y-14 Barnemodus lastar heile biblioteket før noko blir vist 🟡
**Problem:** `accountLibrary()` blar 60 om gongen, opptil 400 per bibliotek, bibliotek for bibliotek
og tenar for tenar, og ingenting blir vist før alt er ferdig. `browseLibraries` blir henta to gonger.
Det finst ingen cache, så kvar oppstart i barnemodus startar frå null.
**Tiltak:** Hent biblioteka parallelt, vis første side per bibliotek med ein gong, og hent resten i
bakgrunnen. Gjenbruk visningslista. Lagre resultatet i Room på same måte som heimesnapshotet, nøkla
på barneprofilen.
**Akseptanse:** barnemodus viser hyller frå cache med ein gong ved kald start; les `BARNEMODUS_PLAN.md`
først, og ingen endring i kva biblioteket barnet får sjå.

### Y-16 Bakgrunnsjobben gjer ei full oppfrisking kvar halvtime 🟢
**Problem:** `MediaRefreshWorker` køyrer heile `refresh()` kvart 30. minutt, inkludert tilrådingar,
utgjevingskatalog og alle rader.
**Tiltak:** Avgrens jobben til det varsla og Watch Next treng: nye element og hald fram. Hopp over
tilrådingar og katalog.
**Akseptanse:** varsla om nytt i biblioteket kjem som før; talet på kall per køyring er under halvparten.

## Rekkjefølgje

| Steg | Innhald | Kvifor først |
|---|---|---|
| 1 | Y-0 målingar | Utan referanse kan vi ikkje vise at noko vart betre |
| 2 | Y-1, Y-4, Y-5, Y-8a | Små endringar med tydeleg effekt og låg risiko |
| 3 | Y-2, Y-7 | Den største effekten, men krev nøye testing av tilgangsreglane |
| 4 | Y-6, Y-15, Y-10 | Avspeling og detaljside, som brukaren merkar kvar dag |
| 5 | Y-9, Y-11, Y-14, Y-16 | Opprydding som gjer appen jamnare |
| 6 | Y-12 | Baseline-profil til slutt, når kodestiane har sett seg |

Kvar endring skal ha einingstest der det er logikk, full `testDebugUnitTest` + `lintDebug`,
instrumentering berre på 5562, og visuell kontroll med `install -r` på 5560/5564.

## Utanfor ytelse, men sett under gjennomgangen

- Emby-bilete får tokenet som `api_key` i URL-en (`ServiceClients.kt:1079`), sjølv om `X-Emby-Token` òg
  blir sendt som header. Tokenet endar då i nøklane til diskcachen og i tilgangsloggane på tenaren.
  Undersøk om noko (widget, Watch Next) treng URL-varianten, og fjern han elles.
- Seerr «Oppdag» blir henta med `language=en` (`ServiceClients.kt:1237`). Bruk appspråket, i tråd med
  omsetjingspunktet i `REVIEW_2026-09-22.md`.
- `runCatching` sluker feil mange stader i hentinga. Med tidsloggen frå Y-0a blir det lettare å sjå
  kva rad som faktisk feilar eller er treg.

## Måleresultat

| Måling | Før | Etter | Eining |
|---|---|---|---|
| Kald start, mobil (5560) | | | ms |
| Kald start, TV (5564) | | | ms |
| Heim: første ekte rad | | | ms |
| Heim: alle rader | | | ms |
| Heim: utanfor heimenettet | | | ms |
| Detaljside serie: episodeliste | | | ms |
| Avspelingsstart til `STATE_READY` | | | ms |
