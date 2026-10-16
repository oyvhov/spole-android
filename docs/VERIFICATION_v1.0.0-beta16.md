# Verifisering av Spole 1.0.0-beta16

16. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 153.

## Omfang

Tilbakemeldingar på beta15 frå ekte TV:

- Mange knappar fekk ei fokusmarkering som ikkje passa forma. Ein gjennomgang på TV-profilen og
  i koden fann fire typar feil: ringen låg rundt ei kolonne med bilete og namn i staden for på
  det runde biletet (medverkande, personar, profilbilete); ringen låg på den 48 dp høge
  trykkflata rundt ein 40 dp knapp; lenker og faner utan sidepadding fekk ring gjennom teksten;
  og ein del knappar hadde berre Material-tonen eller to ringar i ulik form. Rettingane brukar to
  nye hjelparar: `focusRing` for Material-knappar og `focusBleed`, som gir ringen luft rundt ord
  utan å flytte orda.
- Oppdag på TV: toppfeltet bytte høgd etter innhaldet og viste samandrag og lange fakta. No er
  det to liner med fast linjehøgd: tittel, og år · ★ vurdering · type · status. Titlar hentar
  ikkje lenger detaljar ved fokus; førespurnader hentar bilete, år og vurdering.
- Tittelsida for Seerr-titlar mangla vurdering, og medverkande låg under skjermkanten. TMDB-
  vurderinga kjem frå `voteAverage` (og frå rada med ein gong), Rotten Tomatoes frå Seerr sitt
  `ratings`-endepunkt, og toppen er 40 % av høgda for titlar utan episodar og spor. Seerr gir
  `voteAverage` av 10; Spole lagrar TMDB-vurderingar av 100, som frå Jellyfin, og skalerer difor.
- Under gjennomgangen kom det fram at pil opp frå knapperada på TV-tittelsida berre rulla til
  toppen, så samandraget og «les meir» var utanfor rekkjevidd. No går pil opp til samandraget når
  det har meir tekst.

## Bygg og testar

- 1110 einingstestar: alle bestått utanom éin valfri nettverkstest som vart hoppa over. Nye
  testar teiknar skjermen og les pikslar: ringen ligg på ansiktet for medverkande og personar, på
  pillen for heroknappane og «Trailer», og på varselrada i be om-arket; fanene og «Les meir» har
  minst 10 dp luft rundt orda; «Avbryt» er ein pille. Alle feila med beta15-koden. Toppfeltet i
  Oppdag har same høgd for ein førespurnad og ein ordrik tittel, og medverkande startar over
  80 % av høgda på tittelsida. Pil opp frå kalenderknappen gir fokus til samandraget (feila med
  beta15-koden). TMDB-vurderinga frå Seerr og frå rada er lagra av 100.
- Isolert TV 5566, klasse for klasse: RequestFlowUiTest 7/7, RequestRulesUiTest 4/4,
  TvControlsRegressionTest 6/6, SetupAndDiscoverTest 4/4, FocusOutlineTest 3/3,
  DesignRefreshUiTest 22/22, TvNavigationIntegrationTest 2/2, EnglishRequestFlowTest 3/3,
  CompactNavigationTest 4/4, UiConsistencyTest 1/1, WideNavigationSettingsTest 7/7,
  CalendarAndSheetTest 7/7, CombinedSetupUiTest 10/10, FeedRefreshActionTest 2/2,
  SignOutAllTest 2/2, SynopsisLayoutTest 3/3, TvPlaybackControlsTest 14/14,
  TvSettingsRedesignTest 6/6, AccountOptionsTest 3/3, TvRefinementUiTest 36/37
  (`libraryEditorGivesARemoteItsFirstFocus`, kjend). `SheetInteractionTest` 3/10,
  `SheetKeyboardFlowTest` 1/6 og `TabletFeatureTest` 5/9 feila; med beta13-APK-ane på same
  profil feila dei 10/10, 6/6 og dei same 4 (`sheet-close` og `hero-episode-number` finst ikkje i
  TV-oppsettet).

## Ekte profil

Lagra TV-profil 5564, signert bygg 1.0.0-beta16 installert med `-r`. Før rettinga vart fokus på
Heim, Oppdag, tittelsider, personside, «Dine førespurnader», be om-arket, Bibliotek og Innstillingar
fotografert steg for steg med fjernkontrollen; etter rettinga dei same stega:

- Heim: «Hald fram» og «Sjå meir» har ring på pillen (før: berre ein svak tone).
- Oppdag: toppfeltet er to liner, til dømes «Crystal Lake / 2026 · ★ 7.8 · Serie · Kan leggjast
  til», og førespurnaden «Ølhunden Berit» «2025 · ★ 7.2 · Sesong 1 · Førespurd». «Alle 114» er
  ein pille med luft rundt teksten.
- Tittelsida for «Street Fighter»: Rotten Tomatoes 45 % og TMDB 6.3, medverkande på første
  skjerm, ring rundt ansiktet, pille-ringar på «Følg i kalenderen» og «Trailer». Pil opp går til
  samandraget, og OK opnar heile teksten.
- Personsida: «Les meir» er ein pille som ikkje skjer gjennom teksten.
- «Dine førespurnader»: fanene har luft rundt orda, linene har innrykk inne i ringen, og
  tilbakeknappen har ring på sirkelen.
- Be om-arket for «The Mongoose»: varselrada har avrunda ring som vala over, «Avbryt» er ein pille.
  Arket vart lukka med Tilbake.
- Innstillingar → Sider og meny: brytarane har pille-ring rundt sporet og pilknappane ring på
  sirkelen. Ingen brytar vart endra.

Ingen førespurnad vart send, og ingen avspeling starta. Testpakka brukte berre isolert 5566.
