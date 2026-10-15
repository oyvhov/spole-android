# Verifisering av Spole 1.0.0-beta12

15. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 149.

## Omfang

Rettar 17 funn frå gjennomgangen av ekte mobil-, nettbrett- og TV-skjermar, ekstra plass til
nettbrett-hero med skriftstorleik 2.0, og dynamisk gruppering av tittel og episodeinformasjon.
Skjermbilete med ekte innhald og kontoopplysningar ligg berre lokalt under `.artifacts/`.

## Bygg og testar

- Einingstestar: 1004 i 146 suitar; 1003 bestått, éin valfri nettverkstest hoppa over,
  0 feil. Dei åtte nye korttestane passerer ved normal og dobbel skrift.
- Korttestane måler frå den siste faktisk teikna tekstlinja til episodeinformasjonen,
  og kontrollerer stabil kunst, tekstreserve og avspelingshandling ved kort → lang tittel.
  Reserven bruker faktisk tekstmåling, ikkje berre omrekning frå sp til dp.
- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`,
  `--max-workers=2`: `BUILD SUCCESSFUL`, kode 0, etter 4 minutt 24 sekund.
  Logg: `app/build/release-v1.0.0-beta12/build-final-runtime.log`.
  Siste reine testendring vart bygd med same oppgåver: `BUILD SUCCESSFUL`, kode 0,
  etter 1 minutt 3 sekund; release-APK-en er byteidentisk. Logg: `build.log` i same mappe.
- Lint: 0 feil, 196 åtvaringar og eitt hint. Versjons- og språkkontrollen passerer:
  1660 standardnøklar og éin tillaten standardnøkkel.
- Målretta Android-prøver for mobil-/nettbrett-caption, metadata, sveip og OSD: 20 unike
  testar bestått. Samla køyring gav 19/20; den siste normal-skrift-sveipen gjekk forbi
  enden av metadata-rada og vart avgrensa til faktisk attverande rullelengd. Han bestod
  deretter 1/1. Påstandane om rulling, tittelval, avstand og identiske handlingsgrenser står ved lag.
- Ekstra brei mobilkøyring med 509 oppdaga testar vart ikkje fullført: 102 avslutta,
  98 bestått, fire hoppa over og ingen testfeil. Instrumenteringa mista emulatoren med
  prosesskode 255 under test 103; 407 har ikkje avslutta resultat. Dette er ikkje kalla
  509 beståtte testar. Logg: `android-phone-final.log` og `.json` i release-byggmappa.
  Dei fullførte målretta prøvene og den tidlegare kontrollen av dei 17 UI-rettingane
  er grunnlaget for mobilverifiseringa, saman med ekte skjermbilete.

## Android på Google TV

Isolert profil 5566, endeleg debug- og test-APK med `SpoleTestRunner`:

- 108 testar ved systemskrift 1.0: 105 bestått, éin hoppa over og to kjende feil.
- Den eine font-2-testen vart deretter køyrd med systemskrift 2.0: 1/1 bestått.
- Nye caption-prøver beviser éi faktisk linje for kort tittel, to for lang tittel,
  tett episodeinformasjon, stabil kunst og tekstreserve, og uendra D-pad-/fokusåtferd.
- Sein episodeinformasjon held no hero-handlinga i ro også når serien manglar nummer
  ved første lasting. Avspelingskontrollar og sesongprøvene passerer.

Dei to feila er tidlegare dokumenterte og ligg i uendra produksjonskode:
`libraryEditorGivesARemoteItsFirstFocus` ventar første fokus i den isolerte dialog-fixturen;
`movieOnlyInFavouritesOpensAndLibraryTabLeavesThePinnedLibrary` ventar at fanebyte tømmer
bibliotekstien, medan ViewModel uttrykkeleg bevarer siste plassering.
Sjå historikken i [beta10](VERIFICATION_v1.0.0-beta10.md) og
[0.18.0-beta9](VERIFICATION_v0.18.0-beta9.md). Dei er ikkje kalla beståtte.
Loggar: `android-tv-final.log` og `android-tv-font2-final.log` i release-byggmappa.

## Produksjon og oppdatering

Den signerte universal-APK-en er installert med `-r` over beta11/kode 148 på den lagra
mobilprofilen 5560. Pakke `app.reelstack`, kode 149, `1.0.0-beta12`, minSdk 26, targetSdk 36,
fire ABI-ar og ikkje debuggable. Sertifikat SHA-256 er
`36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.

Ekte framside er kontrollert på mobil og i mellombels nettbrettformat 2560×1600, density 320,
med skrift 1.0/2.0: korte serienamn, «Svartisen», lang episodetittel og film utan episodefelt.
Tittel, episode og metadata har naturleg avstand; kunstfelt og handlingsknappar held forma.
Opphavleg mobilformat 1280×2856, density 480, skrift 1.0 og temaval er sette tilbake.
Det er emulatorbevis, ikkje prøve på fysisk nettbrett.

TV-profilen 5564 vart halden på beta11 fram til den offentlege oppdateringsprøva. Ingen testpakke
vart køyrd på 5560/5564, og konto- eller AVD-data vart ikkje nullstilte.

## Publisert artefakt og faktisk oppdatering

- Kjelde og tag: `84f6d39d1cc82536dd12378e94777b9f38795cc8`, `v1.0.0-beta12`.
  `SOURCE_COMMIT.txt` og APK-en sin innebygde VCS-commit samsvarer.
- Bygg etter commit: `BUILD SUCCESSFUL`, kode 0, etter tre sekund. Alle runtime-oppføringar
  utanom commit-/signeringsmetadata og heile R8-mappinga er byteidentiske med det testa bygget.
- Universal-APK: 11 805 326 byte, SHA-256
  `a84f091b48ce7d867383645cb2d998cb065933e143a496603929cc3fb0903e45`.
- R8-mapping: 96 739 596 byte, SHA-256
  `1544a72b2a92f6311c13656d8c24da9363ac0a9c38ca727e80d9ece35464dfee`.
- Dei fire FFmpeg-JNI-biblioteka og begge lisensane er byteidentiske med offentleg beta9.
  Kjelde-/relenkingsarkiv: 30 790 595 byte, SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- [Beta12](https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta12) er publisert
  som prerelease, ikkje draft, med nøyaktig éin APK og fem assets. Alle storleikar, uploaded-statusar
  og GitHub-digestar vart kontrollerte før publisering. Stabil latest er ikkje endra.
- Den uautentiserte release-lista og offentleg APK-nedlasting er kontrollerte. Nedlasta APK
  har same storleik og SHA-256 som arkivfila. Kontrollkallet brukte ein frisk query-parameter
  etter at mellomlageret først returnerte release-lista frå før publisering.
- TV 5564: Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no fann beta12.
  Last ned → Installer → Android «Update» fullførte. Etter «Open» kom ekte framside tilbake;
  Jellyfin og Emby står framleis som «Tilkopla». Installert kode er 149, oppdatert
  15. oktober 2026 kl. 22:24:37; første installasjon er framleis 9. september kl. 20:50:58.
  Dette er faktisk nedlasting og installasjon gjennom appen, ikkje ADB-installasjon.
- GitHub [Bygg og test](https://github.com/oyvhov/spole-android/actions/runs/38800891977)
  for releasecommit fullførte med `success`.

Smarte hyller er framleis globale i beta12. Manglande val av tenar/bibliotek vart meldt av
brukaren etter publisering og blir handtert som ei eiga retting; den publiserte APK-en blir ikkje bytt ut.
