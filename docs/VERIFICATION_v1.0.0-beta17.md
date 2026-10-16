# Verifisering av Spole 1.0.0-beta17

16. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 154.

## Omfang

Tilbakemeldingar på beta16 frå ekte TV:

- Heroen på framsida stoppa i ei line der han sluttar, og biletet vart kutta rart. På TV er
  biletet no 60 % høgare enn heroen, ligg under første rad, og tonar ut med ei alfamaske i
  staden for eit lag i mørk farge. Same mønster på smarthylle-sida, der bileta òg byter seg kvart
  åttande sekund.
- Jellyfin-logoen og namnet ved radene på framsida er borte. `HomeLayout.sourceNeeded` viser
  namnet berre når ein annan tilkopla tenar har same rad synleg.
- Undersidene til «Dine førespurnader» hadde ikkje bakgrunnsbilete. Lista og «Alle»-sidene viser
  no biletet til det som har fokus, som Oppdag.
- Bibliotek-knappen på TV opna den siste undersida, til dømes ei smarthylle. Menyen opnar no alltid
  biblioteksida på TV; på telefon går eit nytt trykk på Bibliotek til sida.
- Smarthylle-sida: «Overrask meg» er fjerna og «Endre» flytta til rada med val.

Ønskt i same runde: eit nytt «Neste episode» i avspelaren. Fem forslag vart teikna; eigaren valde
eit hjørnekort utan bilete, som Emby, med ei roleg nedteljing i knappen. Kortet har namn, sesong og
episode, «Spel neste» og «Skjul» («Avbryt» når episoden er slutt). «Spel neste» startar i dempa
aksentfarge og fylls frå venstre medan sekunda går, utan tal; talet blir lese opp for TalkBack.

Funne under gjennomgangen på TV-profilen og i koden:

- Tilbake frå eit bibliotek gav fokus til heroen; no til flisa eller «Bla i alt», med sida der ho var.
- Søk på TV: pil ned kom ikkje ut av søkjefeltet, og fokus forsvann når resultata rulla, fordi
  søket tømde fokus ved rulling. Treffa i biblioteka hadde eit overflødig merke, og titlar å be om
  hadde ikkje namn.
- Oppdag: Tilbake frå «Alle», sjangrar og «Dine førespurnader» starta på første plakat; pil høgre
  på «Alle» sist i ei rad hoppa til «Alle 114» i rada over.
- Tilbake frå ein tittel i «Liknande titlar» lukka begge titlane.
- «ferdig 21:25» stod på titlar frå Seerr som ikkje kan spelast.
- «Les meir» på personsida forsvann med fokuset; «Kalender» på Heim mangla ring; lista med
  førespurnader rulla slik at tittelen vart kutt.

## Bygg og testar

- 1136 einingstestar: alle bestått utanom éin valfri nettverkstest som vart hoppa over. Nye
  testar: biletet i heroen og på smarthylla når minst 40 % av høgda under; namn ved radene berre
  ved to tenarar; Bibliotek-menyen på TV; fokus attende til flis og «Bla i alt»; bakgrunn på
  «Dine førespurnader» og «Alle»; smarthylla startar på første tittel og har «Endre» blant vala;
  søket når resultata og held fokus ved rulling; namn under plakatar; Tilbake-fokus i Oppdag; pil
  høgre sist i ei rad; «ferdig» berre på spelbare titlar; «Les meir» som veksel; ring på «Kalender»;
  hjørnekortet utan bilete og med fylling som svarar til tida som er gått.
  Alle feila med beta16-koden.
- Isolert TV 5566, klasse for klasse: SmartShelvesTest 30/30, LibraryBrowserUiTest 3/3,
  LibraryServiceDisplayTest 6/6, HomeMediaRowsTest 6/6, HomeLayoutUiTest 3/3, DesignRefreshUiTest
  22/22, SetupAndDiscoverTest 4/4, TvNavigationIntegrationTest 2/2, RequestFlowUiTest 7/7,
  TvControlsRegressionTest 6/6, CompactNavigationTest 4/4, WideNavigationSettingsTest 7/7,
  CalendarAndSheetTest 7/7, SynopsisLayoutTest 3/3, FocusOutlineTest 3/3, EnglishRequestFlowTest 3/3,
  UiConsistencyTest 1/1, TvPlaybackControlsTest 14/14 (med det nye kortet). Kjende feil som i
  beta16: `LibraryPresentationUiTest.libraryOverviewHasOneNextRowIndependentOfHomeSwitches`,
  `TvRefinementUiTest.libraryEditorGivesARemoteItsFirstFocus` og fire i `TabletFeatureTest`
  (`hero-episode-number` finst ikkje i TV-oppsettet).

## Ekte profil

Lagra TV-profil 5564, signert bygg installert med `-r`:

- Heim: biletet i heroen held fram under «Sjå vidare» og tonar ut; radene viser ikkje logo. På
  denne profilen har både Jellyfin og Emby «Sjå vidare», så namnet står berre der.
- Bibliotek: etter «Filmar» i menyen opnar Bibliotek biblioteksida med fokus på heroen. Tilbake
  frå «Seriar» gav fokus til «Seriar»-flisa, med sida på same stad.
- Oppdag: Tilbake frå «Dine førespurnader» gav fokus til «Alle 114». Pil høgre på «Alle» sist i
  «Populært no» blir verande. Lista med førespurnader viser biletet til lina med fokus.
- Søk: «troll» gav treff utan «I biblioteket»-merke; Tilbake frå søket gav fokus til søkjeknappen.
- Tittelsida for «Masterplan»: «Liknande titlar» har namn og år under plakatane, og Tilbake frå
  «Play Dirty» gjekk til «Masterplan».
- «Neste episode» vart ikkje prøvd i ekte avspeling på denne profilen. Kortet vart teikna i
  testmiljøet og sendt til eigaren, og Android-testane for avspelaren gjekk gjennom på 5566.

Android TV si «Set up Device Lock»-rute kom opp ein gong og vart lukka utan å setje opp lås.

Ingen førespurnad vart send, og ingen avspeling starta. Testpakka brukte berre isolert 5566.

## Publisering

- Kjelde og tag: `35102366b07a772dda8fd9bd89dffb8c101910ae`, `v1.0.0-beta17`, i `SOURCE_COMMIT.txt`. APK-en vart bygd frå
  releasecommiten og installert på 5564 med `-r`; han ligg som `tested-runtime.apk` i arkivet.
- APK: 11 860 794 byte, SHA-256 `9b0bff6e34496a8894144131666051a7ee55712d20b403f9a3a6dea041dcfefc`,
  signert med det faste sertifikatet (SHA-256 `36fa94f0…1a10`), pakke `app.reelstack`, kode 154,
  ikkje debuggable. FFmpeg-biblioteka er identiske med beta16, så kjelde-/relenkingsarkivet er det
  same (`2b7eeba9…5b21`).
- [Beta17](https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta17) er publisert som
  prerelease med éin universal-APK og fem assets; alle fem GitHub-digestar vart kontrollerte før
  publisering. Offentleg nedlasting har same SHA-256.

## Faktisk oppdatering gjennom appen

Isolert TV 5566 med offentleg beta16 (kode 153): Innstillingar → Varsel og oppdatering →
Appoppdateringar → Sjekk no fann beta17. «Last ned oppdatering» vart «Installer oppdatering», og
Android «Update» fullførte installasjonen. Etterpå: kode 154 og `1.0.0-beta17`, sist oppdatert
16. oktober 2026 kl. 22:44:44.

GitHub [Bygg og test](https://github.com/oyvhov/spole-android/actions/runs/38964109803) for
releasecommiten er fullført med `success`; `instrumentation-tests` vart hoppa over som før.
