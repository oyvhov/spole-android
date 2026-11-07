# Verifisering av Spole 1.0.0-beta38

7. november 2026. Produksjonspakke `app.reelstack`, versjonskode 175. Bygd oppå beta37 (`1f8ae3f`).

## Bakgrunn

Etter beta37 på Shield melde brukaren dette:

- Gløden rundt kortet med fokus var stygg.
- Bortover i «Sjå vidare» hoppa skjermen.
- Ned gjekk ikkje til rett stad: fokus gjekk sidelengs før det gjekk ned, men ikkje på første rad.
- Det var hakkete å gå nedover, og overskriftene var for store.

Brukaren peikte sjølv på teksthøgda under episodane. Etter den første rettinga i Stova-stil sa brukaren at det var vanskeleg å sjå kva kort som hadde fokus.

## Årsaker funne på Shield (beta37)

- **Ned gjekk sidelengs.** Heile kortet med tekst vart skalert inne i det klikkbare elementet, og fjernkontrollen måler frå dei skalerte grensene. Eit nabokort med to linjer tekst nådde lenger ned enn det skalerte kortet og tel difor som «under» det. Opptak: frå «Nytt liv i gamle hus» gjekk Ned til kortet til høgre.
- **Hoppet bortover.** `RowKeylineSpec` let fokus gå til kanten og flytta så rada fire kort på éin gong.
- **Hopp nedover.** Ei rad er like høg som det høgaste kortet som er synleg. Kort med to linjer episodenamn gjorde rada høgare når dei kom til syne, og alt under hoppa.
- **Grafikkbrikka.** Kanttoninga i `tvHomeBottomFade` (og i beta37 `rowEdgeFade` og `safeBottomFade`) teikna heile sida i eit eige lag. I eit atrace-spor ved rulling vart laget på 1760 × 1080 teikna på nytt i 53 av 55 bilete. RenderThread venta 10–20 ms i `eglSwapBuffers`.
- **Rørsla.** Kvart trykk starta ein ny tween frå ro, så eit trykk under ei rørsle stoppa sida og sette henne i gang igjen.

## Rettingar

- `FocusOutline.kt`:
  - LIFT er 1,08 med 2 dp løft og 6 % lys, utan glød. `liftShadow` er fjerna.
  - Biletet med fokus får ein skarp kant på 3 dp på TV.
  - `focusLift` skalerer biletet inne i det klikkbare elementet, ikkje sjølve elementet.
- Alle korta: ResumeCard, LibraryCard, RailSeeAllCard, SmartShelfTile, PosterOnlyCard og SmartShelfCard.
- `RowKeyline.kt`:
  - Fokus som kjem inn i ei rad ovanfrå eller nedanfrå, festar staden det landar på.
  - Venstre og høgre flyttar rada eitt kort per trykk under eit fokus som står i ro.
- `ScrollGlide.kt`: ei fjør utan sprett (stivleik 280, som Stova) som tek med farten frå eit avbrote trykk. Både sida og radene brukar ho.
- `EdgeFade.kt`: kanttoninga går gjennom lag som berre er så store som bandet, som på Stova sin Heim. Brukt i `tvHomeBottomFade`, `rowEdgeFade` og `safeBottomFade`.
- `RailCardCaption.kt`: på TV står episodelinja på éi linje, og plassen hennar blir halden også når ho manglar.
- `rememberTvListState`: Heim og radene på TV har eit buffer (`LazyLayoutCacheWindow`) som byggjer neste rad og neste kort medan sida står i ro.
- Overskriftene på radene på TV brukar `titleMedium` (16 sp).
- Manifestet har `<profileable android:shell="true" />`, slik at CPU og bilettid kan målast med simpleperf og Perfetto. Appen er framleis ikkje debuggable.

## Bygg og testar

- Einingstestar på den endelege kjelda: 1 656 køyrde, 0 feil, 1 hoppa over. Lint: 0 feil, 220 åtvaringar. Omsetjingar: 1 695 nøklar OK. Release-samsvar OK.
- Alle 20 native bibliotek er byte for byte like beta37.
- Desse testane feila av rett grunn før rettinga:
  - `HomeTvDownNavigationTest.theCardTheRemoteMeasuresFromDoesNotGrowWithFocus`: toppen flytta seg 9,25 dp.
  - `everyCardInARowIsTheSameHeightWhateverItsCaption`: 205,5 mot 222,5 dp.
  - `comingDownIntoARowDoesNotSlideItSideways`: filmen flytta seg frå 728 til 48 px.
  - `HomeTvShelfKeylineTest.everyStepLandsTheFocusedCardOnTheKeyline…`: 194 i staden for 24 dp.
  - `LiftFocusIndicationTest`: glød på sida, ingen skarp kant, skala.
  - `HomeSectionTitleTest.aShelfHeadingIsOneQuietLine`.
- `HomeTvDownNavigationTest.downFromACardBesideATallerCaptionGoesToTheRowBelow` passerte også før rettinga, fordi rada under står nærare i testoppsettet enn på Shield. Han står att som vern.

## På Shield

SHIELD Android TV, Android 11, 1920 × 1080, animasjonsfart 0,5×, ekte Jellyfin- og Emby-profil. Signerte bygg lagde inn med `install -r`, med uendra første installasjonsdato.

Sett på eininga:

- Fokuskortet er tydeleg: større, med skarp kvit kant og utan glød.
- Alle episodelinjer står på éi linje.
- Bortover flyttar rada seg eitt kort per trykk.
- Ned frå andre rad gjekk ned og ikkje sidelengs.

At rada står stille når fokus kjem inn ovanfrå, er berre prøvd i einingstesten. Før rettinga glei rada sidelengs på eininga, frå «The Housemaid» ned i «Favorittar · Emby».

Målingane:

| | beta37 | beta38 |
| --- | --- | --- |
| GPU per bilete, median | 7 ms | 4 ms |
| GPU per bilete, 90. persentil | 13 ms | 7–10 ms |
| Heilskjermslag teikna på nytt | 53 av 55 bilete | ingen |
| Bilete som kjem innan éin vsync | om lag 80 % | om lag 77 % |

**Det som står att:** om lag kvart femte bilete hoppar over eitt vsync, både i beta37 og beta38.

Profilen (simpleperf, `profileable`) syner kvifor. Om lag 40 % av tida på hovudtråden går til tilgjengelegheitstreet i Compose (`AndroidComposeViewAccessibilityDelegateCompat.getCurrentSemanticsNodes`, sortering og hendingar). Treet blir bygd fordi tre tilgjengelegheitstenester er slått på:

- Button Mapper, som har lov til å lese vindaugsinnhald
- QuickBars
- Stova Heim-knapp

Compose samanliknar då treet om lag kvart 100. ms medan noko rører seg. Det er neste steg. Bufferet i `rememberTvListState` gjer treet større, og verknaden av det er ikkje målt med tenestene på.

## Hending

Medan eg måla, starta nokon ein film i Emby på Shield. Ein bolk med D-pad-trykk (5 × opp, 6 × ned) som var meint for Spole, gjekk inn i Emby-spelaren og viste kontrollane der. OK vart ikkje trykt, og filmen spelte vidare. Eg slutta å bruke Shield. Ingen førespurnad vart send, og ingen avspeling starta i Spole.
