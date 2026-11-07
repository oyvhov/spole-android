# Verifisering av Spole 1.0.0-beta40

7. november 2026. Produksjonspakke `app.reelstack`, versjonskode 177. Bygd oppå beta39 (`3e1c5da`).

## Bakgrunn

Etter beta39 sa brukaren «Jobb med Shield TV» og «Lag Spole perfekt». Arbeidet er gjort på Shield (Android 11, Button
Mapper, QuickBars og Stova Heim-knapp slått på) og kontrollert på Loftet (Google TV Streamer, bokmål).

## Retting av målinga i beta38 og beta39

Verifiseringane for beta38 og beta39 seier at om lag kvart femte bilete kom for seint. Det talet er feil. Det kom frå
avstanden mellom tidspunkta der bileta var *ferdige* (`FrameCompleted` i `framestats`). Den avstanden varierer med kor lang
tid kvart bilete brukar, også når ingen bilete fell bort.

Rett mål er avstanden mellom bileta sine planlagde vsync (`IntendedVsync`): eit hol på to vsync eller meir betyr at
appen ikkje laga noko bilete til det vsync-et. Målt slik på Shield, nedover heile Heim:

| | Vsync utan bilete | Bilete ferdige meir enn to vsync etter start |
| --- | --- | --- |
| beta38 | 3–10 % | 1–5 av 120 |
| beta39 | 1–10 % | 0–4 av 120 |
| beta39, kompilert med profilen slik Android gjer om natta | 1–2 % | 0 av 120 |
| beta40 | 2–3 % | 1–3 av 120 |

Eit tidleg trykk etter installasjon kører delvis tolka kode. Det forklarer dei høgaste tala i kvar rad.

## Det som var ekte: ombygging ved kvart trykk

Eit systemspor (`atrace` med spormerke i ei prøveutgåve) synte kva det første biletet etter kvart trykk gjorde på Shield:

- 14–20 ms i `Recomposer:recompose`
- inni den bygde Compose om seks filmkort på rada som nettopp hadde kome til syne

Årsaka var `appeared`-tilstanden i korta:

```kotlin
var appeared by rememberSaveable { mutableStateOf(false) }
LaunchedEffect(media.id) { appeared = true }
```

På TV er innfaringa slått av, så verdien er alltid 1. Men skrivinga av `true` eit bilete etter at kortet var bygt, fekk
Compose til å byggje kortet ein gong til. `rememberCardAppeared` skriv ingenting når det ikkje er noka innfaring.

Etter rettinga tek ombygginga i det første biletet 0,2–1 ms. Unntaket er første trykk frå toppbiletet, som framleis tek
om lag 12 ms.

## Rettingar

- **Ombygginga ved kvart trykk:** `CardAppeared.kt`, brukt i ResumeCard, LibraryCard, UpcomingCard og HomeHeader.
- **Færre nodar i tilgjengelegheitstreet.** Compose 1.11 sorterer kvar node etter plassering omtrent kvart 100. ms medan
  sida rører seg. Sorteringa går òg gjennom nodar som er fjerna med `clearAndSetSemantics`, og
  `Modifier.background` og `graphicsLayer` er kvar sin node. Endra:
  - Indre test-merke i korta er berre med i debug (`devTag`). Instrumenteringstestane køyrer på debug-bygget.
  - Framdriftslinja og overlegga på korta er teikna (`drawnBackground`), ikkje lagde med `background`.
  - Reserven til teksten under korta er målt med `TextMeasurer` i staden for to skjulte tekstar.
  - På TV er `ComposeUiFlags.isGraphicsLayerShapeSemanticsEnabled` av, så lag-blokkene ikkje blir køyrde på nytt for å
    finne forma i kvar runde.
- **Radkantar:** `bleedToEnd` lèt «Neste» på bibliotekssidene, episodane og «Medvirkende» gå under sidemargen på 32 dp
  til skjermkanten og tone ut der. `safeBottomFade` kuttar ikkje lenger innhald på sidene.
- **Fokus bak menyar:** `ring()` teiknar ingen ring i eit vindauge utan fokus. Toppmenyen står når profilmenyen er open
  (`topBarStaysFor`), så avataren held fokus og får det att når menyen lukkar seg.
- **Spornamn:** `readableTrackLabel` gir språket på appspråket ut frå ISO-koden og kortnamn på formata. «Norwegian -
  SUBRIP» blir «Norsk · SRT». Brukt på detaljsidene og i spelaren.
- **Samlingar** har ikkje lenger årstal i undertittelen eller fakta.
- **Kalenderen:** eit 404-svar for ein tittel eller sesong gir ingen datoar, men tel ikkje som ein feil ved henting.

## Ikkje retta

- «Noen utgivelsesdatoer kunne ikke hentes» står framleis under «Kommer snart» på Loftet, også etter ei oppdatering med
  beta40. Årsaka er altså ikkje 404-svar. Feilen blir svelgd utan logg, så tittelen og statuskoden er ikkje kjende.
- «Nye episoder» og «Nylig tilgjengelig» viser delvis dei same episodane.
- På Loftet er det grafikkbrikka som avgrensar når toppbiletet og dei store korta nedst er synlege (sjå beta39).

## Bygg og testar

- Einingstestar: 1 679 køyrde, 0 feil, 1 hoppa over. Lint: 0 feil, 219 åtvaringar. Omsetjingar: 1 695 nøklar OK.
  Release-samsvar OK.
- Desse testane feila av rett grunn før rettinga:
  - `CardAppearedTest.aCardWithoutARevealIsBuiltOnce`: bygd 2 gonger
  - `CardNodeDietTest`: 4 nodar under teksten, og eit klipp-lag på framdriftslinja
  - `LiftFocusIndicationTest.aCardInAWindowWithoutFocusShowsNoEdge`
  - `TrackLabelTest`: alle fem
  - `MediaLineTest.aCollectionCarriesNoYear`
  - `RowBleedTest`: begge
  - `TopBarStaysTest.theMenuLineStaysUnderTheProfileMenu`
  - `PersonalCalendarClientTest.aTitleOrSeasonTheServerDoesNotKnowIsNotAFailure`

## På einingane

- **Shield, med prøvebygg:** ombygginga i det første biletet etter eit trykk gjekk frå 14–20 ms til 0,2–1 ms i systemsporet.
- **Loftet, med prøvebygget av beta40 (177):**
  - «Neste» på Seriar og episodane på «Kongen befaler» går ut til skjermkanten og tonar ut.
  - Spora heiter «Norsk · Dolby Digital+ · Stereo · Standard» og «Norsk · SRT».
  - Med profilmenyen open står toppmenyen, berre «Øyvind» har fokusring, og Tilbake gir fokus til avataren.
  - Samlingar har ingen årstal.

## Hending

Medan eg måla på Shield, starta brukaren noko i Spole sin eigen spelar. Ein bolk med opp og ned gjekk inn i spelaren og
heldt kontrollane framme. Brukaren spurde om det var meg.

Vernet mitt stoppa berre for avspeling i andre appar og sjekka berre ved starten av ein bolk. No stoppar det for all
avspeling og for spelarvindauge, og det sjekkar før kvart trykk. Det vart ikkje trykt OK, og ingenting vart spola eller
endra.
