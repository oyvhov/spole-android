# Verifisering av Spole 1.0.0-beta14

16. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 151.

## Omfang

Oppdag og Aktivitet er slått saman. Aktivitet er ikkje lenger ei fane; «Dine førespurnader»
ligg øvst i Oppdag med fanene Alle, På veg, Klare og (for administratorar) Tenaren. Oppdag har
Seerr-rader som lastar kvar for seg (Populært no, Filmar og seriar som kjem, Folk, Sjangrar),
plakatar utan tekst og knappar, eit fast toppfelt for tittelen med fokus på TV, søk med folk,
ei side per person, tekst-handlingar på tittelsida og val av utgåve, kvalitet og lagringsstad i
Be om-arket for kontoar som har lov i Seerr. Spesifikasjon: `OPPDAG_SAMLA_2026-10-16.md`.

## Bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`,
  `--max-workers=2`: `BUILD SUCCESSFUL`, kode 0.
- 1091 einingstestar i 164 suitar: 1090 bestått, éin valfri nettverkstest hoppa over, null feil.
  Nye testar dekkjer lastaren for radene (feil i éi rad, seine svar frå ein annan profil),
  Seerr-klienten (rader, sjangrar, folk i søk, person og kreditt, trailer, profilar og rotmapper,
  4K-sesongar, POST med berre viste val), tilbake-vegen mellom tittel og person, gjentekne titlar
  i «Alle», tilbaketrekking av rett førespurnad, søkjefeltet som held fokus, Tilbake inne i Oppdag,
  status etter sending, fanene på éi line, store skrift i Be om-arket og startfokus på TV.
- Lint: null feil, 198 åtvaringar og eitt hint. Språkkontrollen passerer med 1644 standardnøklar
  og éin tillaten standardnøkkel. Release-metadata samsvarer.
- Isolert TV 5566, systemskrift 1.0, klasse for klasse med `pm clear`: SmartShelvesTest 30/30,
  DesignRefreshUiTest 22/22, RequestFlowUiTest 7/7, WideNavigationSettingsTest 7/7,
  TvControlsRegressionTest 6/6, RequestRulesUiTest 4/4, SetupAndDiscoverTest 4/4,
  CompactNavigationTest 4/4, HomeLayoutUiTest 3/3, FocusOutlineTest 3/3,
  TvNavigationIntegrationTest 2/2, UiConsistencyTest 1/1, ViewerExperienceTest 1/1.
  Same køyring tidlegare i dag: HomeMediaRowsTest 6/6, TvSettingsRedesignTest 6/6.
- Kjende feil, ikkje nye: `Alpha12UiTest.updatePanelOpensFromSettingsAndClosesWithoutChangingAccountState`
  («Failed to inject touch input») feila på same måte med beta13 sine APK-ar på 5566;
  `PersonalizationTest.wideMediaReachesTheEdgeButProfileKeepsItsInset`,
  `TvRefinementUiTest.libraryEditorGivesARemoteItsFirstFocus`,
  `LibraryPresentationUiTest.libraryOverviewHasOneNextRowIndependentOfHomeSwitches` og
  `HomeSearchNavigationTest` 4/4 (`@Before` når ikkje Heim på TV) er kjende frå før.
- `CompactNavigationTest` feila først fordi fire faner får plass i full lengd ved 360 dp; testen
  held no same trange breidd som før (52 dp per namn) og passerer. `ReelstackSmokeTest` er
  skriven om til plakatar og tittelside, men er ein telefonklasse og vart ikkje køyrd her.

## Gjennomgang

Ein uavhengig gjennomgang av heile greina fann fire kritiske og fem viktige feil, mellom anna
krasj i «Alle» ved gjentekne titlar, tilbaketrekking som kunne treffe feil førespurnad,
søkjefelt som miste fokus på telefon og Tilbake som gjekk til Heim. Alle er retta med ein test
som feila først. Mindre funn er lista i rettleiinga for neste runde og ikkje retta i beta14.

## Ekte profil og visuell kontroll

Den signerte produksjons-APK-en vart installert med `-r` over beta13 på den lagra TV-profilen
5564. Kode 151 og `1.0.0-beta14` er stadfesta; første installasjonstid er framleis
9. september 2026 kl. 20:50:58. Ekte kontoar og innhald kom tilbake.

- Oppdag: startfokus på første førespurnad, toppfeltet namngir han; radene, Folk med bilete og
  Sjangrar viser seg. «Alle 114» opnar «Dine førespurnader» med fanene på éi line og startfokus
  på «Alle»; Tilbake går til Oppdag med fokus att på same plakat.
- Tittelside (Crystal Lake): bakgrunnsbilete, «Be om sesongar» med fokusring, «Følg i kalenderen»
  og «Trailer» i tekstfarge, rolleliste. Ein person derifrå opnar personsida, og Tilbake går til
  tittelen.
- Personside (Linda Cardellini): bilete, «Skodespelar · fødd 25. juni 1975», biografi (engelsk
  reserve når TMDB ikkje har norsk) med «Les meir», «I biblioteket ditt» og «Meir med …».
- Be om-arket for ein serie som administrator: Kvalitet «HD - 720p/1080p · vanleg val» og
  Lagringsstad «Seriar 10 · 4,8 TB ledig» med stien dempa; startfokus på Kvalitet; lista opnar
  seg i same ark. Arket vart lukka utan å sende noko.

Ingen førespurnad vart send og ingen vart trekt tilbake på den ekte kontoen. Ein systemdialog
frå Android TV («Set up Device Lock») vart avvist med «Not now». Testpakka brukte berre isolert
5566; ingen instrumentering, utlogging eller nullstilling på 5560/5564.

## Publisert produksjonsartefakt

- Universal-APK: 11 828 526 byte, SHA-256
  `40b032a8820f3310699fbd3558f0cc0be0c200f8e939341a53c19132f6feb960`.
- R8-mapping: 97 378 797 byte, SHA-256
  `d335c730340ce6c914650ba34b78d2d8b0659d58d526c61ad5078bc83cb42a9f`.
- Produksjonspakke `app.reelstack`, kode 151, minSdk 26, targetSdk 36, fire ABI-ar og ikkje
  debuggable. Fast sertifikat SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Fire FFmpeg-JNI-bibliotek og begge lisensane er byteidentiske med beta13. Kjelde-/relenkingsarkiv
  gjenbrukt: 30 790 595 byte, SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Kjelde og tag: `4412eff1f86f2f3f071103e222ecae39f250fa05`, `v1.0.0-beta14`, i `SOURCE_COMMIT.txt`.
  Bygget er laga i ei git-arbeidsmappe, så APK-en har ingen innebygd VCS-commit
  (`NO_VALID_GIT_FOUND`), som beta10 frå same mappe.
- Etter commit: `assembleRelease`, `BUILD SUCCESSFUL`, kode 0. Alle oppføringar i APK-en og
  R8-mappinga er byteidentiske med det testa bygget.
- [Beta14](https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta14) er publisert som
  prerelease, ikkje draft, med éin universal-APK og fem assets. Tag, filstorleikar, uploaded-status
  og alle fem GitHub-digestar vart kontrollerte før publisering. Stabil latest er ikkje endra.
- Uautentisert release-liste og offentleg APK-nedlasting er verifiserte; den nedlasta APK-en har
  same byte-storleik og SHA-256 som arkivfila.

## Faktisk oppdatering gjennom appen

Den lagra TV-profilen 5564 hadde alt fått beta14 med `-r` for den visuelle kontrollen, så den ekte
oppdateringa vart prøvd på den isolerte TV-profilen 5566. Der vart offentleg beta13 (kode 150,
SHA-256 `7430346d…5e72`, lasta ned frå GitHub) installert over ein eldre produksjonsbeta.
Ved opning viste appen sjølv «Spole v1.0.0-beta14 er klar». «Sjå oppdatering» opna panelet med
notata og «Nedlasting · 11,3 MB»; «Last ned oppdatering» vart «Installer oppdatering» etter
appen sine kontrollar. Android «Do you want to update this app?» → «Update» fullførte
installasjonen, og «Open» opna appen att.

Etter oppdateringa viser pakkekontrollen kode 151 og `1.0.0-beta14`, sist oppdatert
16. oktober 2026 kl. 11:01:04. Første installasjonstid er framleis 19. september 2026 kl. 14:51:26.
Dette er ei faktisk offentleg nedlasting og Android-oppdatering, ikkje ein `adb install -r`-test.
5566 har ingen innlogga tenester; appen opna på førehandsvisinga.

GitHub [Bygg og test](https://github.com/oyvhov/spole-android/actions/runs/38877132022) for
releasecommiten `4412eff` er fullført med `success`: jobben `build` bestod, og
`instrumentation-tests` vart hoppa over som i tidlegare køyringar.
