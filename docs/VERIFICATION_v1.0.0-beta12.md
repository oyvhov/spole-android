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

TV-profilen 5564 er halden på beta11 for den offentlege oppdateringsprøva. Ingen testpakke
blir køyrd på 5560/5564, og konto- eller AVD-data blir ikkje nullstilte.
