# Verifisering av Spole 1.0.0-beta18

17.–18. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 155.

## Omfang

Avspelaroppgraderinga og ViewModel-oppdelinga som Codex gjorde i arbeidsmappa
([PLAYER_UPGRADE_2026-10-17.md](PLAYER_UPGRADE_2026-10-17.md),
[VIEWMODEL_ARCHITECTURE.md](VIEWMODEL_ARCHITECTURE.md)), testa på nytt frå botnen og retta
før release. Codex sine eigne Android-køyringar var utval på 19–68 testar; heile suiten fann
feilen i heroen nedanfor.

Funne og retta, kvar med ein test som feila før rettinga:

- **Heroen på TV byta ikkje tittel med fokus på knappane.** Codex sette rotasjonen på pause så
  lenge heroen hadde fokus, og «redusert rørsle» stoppa han. På TV står fokus der når Heim opnar.
  beta11-åtferda er tilbake: heroen byter med fokus, kvart fjernkontrolltrykk startar ventetida
  på nytt, og berre lettvektsmodus stoppar rotasjonen. `TvRefinementUiTest.televisionHeroRotates…`
  feila med Codex-bygget og består no; `TvHeroFocusTest` er skriven om etter same krav.
- **ASS-originalar over 2 MB mista teksten.** beta17 henta alltid `Stream.vtt`. No blir
  tenaren sin VTT av same spor brukt når originalen er for stor eller ikkje kan hentast
  (`JellyfinPlayerTest.anAssFileTooLargeForTheRendererFallsBackToTheServersVtt`, 3 MB ASS).
- **Kvar innlogging tømde køa med framdrift som ikkje var send**, for alle profilar, også
  ved å leggje til Seerr. No blir berre kontoen ein loggar ut av rydda; «Logg ut av alt» tømmer
  heile køa saman med nedlastingane (`AccountChangeProgressTest`).
- **Eit sjekkpunkt tenaren avviste, vart ikkje brukt til å halde fram** etter at den lokale
  journalen gjekk ut etter eitt døgn (`OfflinePlaybackProgressTest`).
- **Feil frå Spole sine eigne undertekstlesarar** (SUP, MKS, ASS-dokument) vart rapporterte som
  eit øydelagt filmformat, som kan pakke om eller stoppe filmen. Dei er no
  `SubtitleSourceException` og slår av teksten (`SubtitleFailureClassificationTest`). Feilvegen
  let seg ikkje utløyse med lokale filer på emulatoren: ei MKS med feil kodek gav berre ingen
  tekst, og filmen heldt fram.
- **Låsen i avspelaren:** skriving til disk og Keystore skjedde under ein lås hovudtråden ventar
  på kvart sekund. Avspelaren les no den sist publiserte økta utan å vente
  (`SessionScopeTest.thePlayersTickDoesNotWaitForAGuardedWrite`).
- **Tekst etter videoen:** ei line som varte lenger enn videoen, heldt avspelinga gåande, både
  for ASS og for vanleg VTT gjennom Media3. Dette fanst før Codex. `MediaBoundTextRenderer` reknar
  teksten som ferdig når perioden er slutt (`NativeAssPlaybackTest`, to nye prøver som feila før).
- Mindre: mangel på minne under teikning av ASS mistar teksten i den ruta i staden for appen; den
  overflødige `ass`-AAR-en er fjerna (`ass-kt` har same `libass.so`); ein test passar på at
  Media3-versjonen ikkje blir endra utan ny kontroll av den kopierte `SpoleSubtitleSources`;
  alle punkta i «Avspelingsval» er minst 48 dp (`PlayerOptionsMenuTest`).

Vurdert og ikkje endra: førehandsbilete for neste episode kan ikkje gå tapt, fordi kvart
tittelbyte går gjennom `choose()`, som avbryt den førre hentinga. At onboarding blir fullført
utan å vente på eit lukka ark, og at bibliotekval ikkje lenger tømmer framdriftsjournalen, er
rettingar frå Codex og er behaldne.

## Bygg og testar

- 1220 einingstestar: 1219 bestått, éin valfri nettverkstest hoppa over. Lint: 0 feil (211 åtvaringar, som før). Testane for kvar retting feila med koden før rettinga; to vakttestar (48 dp i valmenyen, Media3-versjonen) held fast på krav som alt var oppfylte.
- Isolert TV 5566, heile suiten klasse for klasse: 88 klassar, 507 testar, 467 bestått. Dei 40 feila er dei same som med beta17 (TV-baseline: AdaptiveDialog, Alpha12, HomeSearchNavigation, LanguagePicker, LibraryPresentation, MobileHeroGesture, Personalization, ReelstackSmoke, ReleaseRows, SheetEscape, SheetInteraction, SheetKeyboardFlow, TabletFeature og TvRefinement.libraryEditor…). JellyfinPlayerTest 44/44, NativeAssPlaybackTest 7/7, TvPlaybackControlsTest 15/15, SmartShelvesTest 30/30.
- Isolert mobil 5562: 11 klassar for avspelar, mobil og nedlastingar, 94 testar, alle bestått (JellyfinPlayerTest 44/44, PlaybackTouchGesturesTest, PlaybackOptionsUiTest, PlaybackPreviewUiTest, NativeAssPlaybackTest, BitmapSidecarPlaybackTest, MobileHeroGestureTest m.fl.).
- `scripts/test-progress-restart.sh emulator-5566`: begge fasane bestått.
- Emulatoren 5566 krasjar på verten (segfault i swiftshader) etter ei stund og alltid i
  `OfflineDetailDownloadUiTest`, også med beta17. Klassane etter eit krasj vart køyrde på nytt
  etter omstart. `OfflinePlaybackIntegrationTest` stoppar på 5562 fordi profilen har under 300 MB
  ledig mot appen sin reserve på 512 MiB; han består på 5566.

## Ekte profil

Lagra TV-profil 5564, signert bygg installert med `adb install -r` over beta17 (154 → 155):

- Kontoane og det ekte innhaldet var der etter oppgraderinga: «Sjå vidare» frå både Jellyfin og
  Emby, med namnet på tenaren berre der begge har rada.
- Heroen på Heim byta frå «Kongen Befaler» til «Trolljegeren» medan fokus stod på «Spel av»,
  med fokusringen synleg. Det var feilen frå Codex-bygget.
- Avspelaren, valmenyen og undertekstane vart ikkje prøvde på den ekte kontoen. Dei er prøvde
  på dei isolerte profilane med syntetiske tenarar og lokale testfiler.

Ingen førespurnad vart send, og ingen avspeling starta på den ekte kontoen. Testpakka brukte berre
isolerte 5562 og 5566.
