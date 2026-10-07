# Verifisering av Spole 1.0.0-beta4

7. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 141.

## Innhald

- Oppstart med tenarar på nettet, adresseprøving, Emby-brukarval, automatisk Seerr på lokalnettet,
  fjernkontrollfokus og animasjonen på velkomstskjermen. Sjå [endringsrapporten](ONBOARDING_DISCOVERY_2026-10-06.md).
- Spelaren: eit nettverksbrot som har spelt vidare i eitt minutt etter nytt forsøk, tel ikkje lenger
  mot neste brot (`networkRecoveriesSpent`). Ein straum som feilar på same stad, gir framleis opp
  etter to forsøk. Endringa kom etter at ein lang episode over tunnel stoppa på SHIELD.
  Feilkoden frå TV-en er ikkje henta, så endringa er ei sannsynleg, ikkje stadfesta, forklaring.

## Bygg, einingstestar og lint

- Fullt bygg (`testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`)
  avslutta med `BUILD SUCCESSFUL`, kode 0, etter 13 minutt.
- 882 einingstestar: 881 bestått, 1 hoppa over (valfri prøve mot ekte nett), 0 feil. Nye i denne
  utgåva: `LanServerDiscoveryTest`, `ServerAddressCandidatesTest`, `ServerProbeTest` og to
  gjenopprettingstestar i `PlaybackRecoveryTest`.
- Lint: 0 feil, 188 åtvaringar og 1 hint, same tal som beta3.

## Android-testar på isolerte profilar

Køyrde klasse for klasse med `pm clear` før kvar klasse. Ekte profilar 5560/5564 er ikkje instrumenterte.

**TV 5566 (`Spole_TV_Instrumentation`):** 41 testar, 40 bestått.
- `CombinedSetupUiTest` 10/10, `TvSetupTest` 4/4, `SetupAndDiscoverTest` 5/5, `LinkedLoginTest` 7/7,
  `AccountOptionsTest` 3/3.
- `LoginExperienceTest` 8/9. `companionLoginIsOptInAndShowsDestinationAndConsent` feila likt på
  uendra beta3-kjelde.
- `JellyfinPlayerTest`: `networkRecoveryKeepsTheServerSessionAndPosition`,
  `interruptedStreamHasWorkingRetry` og `googleTvRemoteControlsRealVideoAndBackReturnsSafely` 3/3.

**Mobil 5562 (`Spole_Instrumentation`):** 40 av 40 i oppsett, innlogging og gjenoppretting.
- `CombinedSetupUiTest` 10/10, `SetupAndDiscoverTest` 5/5, `LinkedLoginTest` 7/7,
  `AccountOptionsTest` 3/3, `LoginExperienceTest` 9/9, `ActivityAdaptiveTest` 4/4 og dei to
  gjenopprettingstestane i `JellyfinPlayerTest` 2/2.
- Profilen hadde tvungen skjerm 1920×1200 og tettleik 240 frå tidlegare arbeid. Då fekk
  `ReelstackSmokeTest` sidemeny i staden for telefonoppsett og feila 8 av 8 i oppstartssteget. Med
  telefonstorleik feila 3 av 8. Innstillingane vart sette tilbake til 1920×1200/240 etter køyringa.
- `ReelstackSmokeTest.jellyfinEditorOffersQuickConnectAndAccountLogin` feila fordi «Hald fram» no
  prøver adressa mot tenaren før innloggingssteget. Testen ventar no på steget, og han bestod i ny køyring.
- Står att i `ReelstackSmokeTest`: `homeScreenShowsCoreMediaState` («Spelar no» ikkje synleg) og
  `upcomingOpensCalendarAgenda` (undertittelen i kalenderen ikkje synleg). Desse gjeld heimesida og
  kalenderen, som denne utgåva ikkje endrar. Dei er ikkje samanlikna med beta3 på mobil.
- `SheetInteractionTest` 9/10. `everyRouteUsesSameTopRightCloseTarget` stoppar på kalenderarket, og
  i ei mellombels omordna køyring på førespurnadsarket. Begge har anna toppkant enn detaljarket på
  telefon. Testen namngir no arket som avvik. Rekkjefølgja er uendra.
- Etter det signerte bygget vart heimenettadresser og tenarnamn i kommentarar og testdata bytte ut
  med nøytrale døme. Linjetala er uendra. Kontrollbygget gav same SHA-256 for APK og mapping.
  Einingstestane (882, 0 feil) og `CombinedSetupUiTest` på TV 5566 (10/10) bestod på nytt med dei
  nye testdata.

## Signert produksjonsartefakt

- Universal-APK: 11 517 334 byte; `app.reelstack`, kode 141, `1.0.0-beta4`, minSdk 26 og
  targetSdk 36; ikkje debuggable.
- SHA-256: `96acc653486538e910c1a7b2151e08b4503a42ffefda0497dae3c05e0ca39f2d`.
- Sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping for akkurat dette bygget: 89 907 928 byte. APK, mapping og FFmpeg-arkiv ligg i
  `app/build/release-v1.0.0-beta4`.
- Alle åtte native bibliotek og begge FFmpeg-lisensfilene er byteidentiske med beta3. Kjelde- og
  relenkingsarkivet er det same: 30 790 595 byte, SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Signert APK er installert med `install -r` over beta3 på lagra TV-profil 5564. Versjonen vart
  `1.0.0-beta4` / 141, sist oppdatert 7. oktober 2026 kl. 09:05:06. Første installasjonstid er
  framleis 9. september 2026 kl. 20:50:58. Appen opna rett på heimesida med den eksisterande kontoen,
  Seerr-profilen og «Sjå vidare» frå Jellyfin, utan nytt oppsett.

## Avgrensingar

- Oppdaging av tenarar er prøvd mot ekte nett frå arbeidsstasjonen og med injiserte tenarar i
  UI-testane. Han er ikkje prøvd på fysisk SHIELD. Emulatorane sit bak QEMU-NAT og får ikkje svar på
  kringkasting.
- Gjenopprettinga etter nettverksbrot er dekt av einingstestar og to syntetiske spelartestar, ikkje
  av eit ekte tunnelbrot.
