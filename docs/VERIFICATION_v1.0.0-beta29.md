# Verifisering av Spole 1.0.0-beta29

1. november 2026. Produksjonspakke `app.reelstack`, versjonskode 166. Bygd oppå beta28 (`a3261f3`).

## Omfang

- Heim på TV som i Stova: toppbiletet går ned til den første hylla, som blir målt og står heil nedst
  (`tvHeroHeightFor`, `TabletLibraryFeature(televisionHeight)`). Radene tonar ut 28 dp over botnen
  (`tvHomeBottomFade`). Biletet går vidare bak hylla og brukar det reine bakgrunnsbiletet
  (`televisionHeroArtworkUrl`). Logoen er 380 × 88 dp.
- Toppbiletet: ratingar fyrst, så år, lengd eller tid att og aldersgrense; «Episode»/«Film» er borte; to liner
  handling; biletet syner meir (Stova si kurve).
- Kortstil som i Stova som standard: `ArtworkCorners.ROUND` (20 dp) og `FocusStyle.LIFT` (kortet veks 6 %
  utan ring, dei andre på hylla dempast 30 %, framdrift 4 dp i aksentfargen). Knappar og menyar har ring.
  Eingongsflytting frå gammal standard (`card_style_stova_applied`).
- Miniatyrbilete: krossfadinga frå plasshaldaren gav eit 1,26 gonger for stort bilete i «Sjå vidare»;
  `artworkRequest` set no alltid `preferEndFirstIntrinsicSize`.

## Bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`: BUILD SUCCESSFUL.
  1366 einingstestar, 0 feil, éin valfri nettverkstest hoppa over. Lint: 0 feil. Release-data: OK.
- Isolert TV 5566 (`SpoleTestRunner`): `TvRefinementUiTest`, `TvNavigationIntegrationTest`,
  `TvSettingsRedesignTest`, `PersonalizationTest`, `TabletFeatureTest`, `DesignRefreshUiTest`,
  `HomeVisualPolishTest`, `LauncherLibraryIntegrationTest`, `AccountPanelsTest`, `LanguageResourcesTest` og
  `LinkedLoginTest`: 110 køyrde, 104 beståtte, éin hoppa over med vilje (antakelse om dobbel tekst), 5 feil.
  Dei 5 er nettbrett-testar som ikkje held på TV: `PersonalizationTest.wideMediaReachesTheEdgeButProfileKeepsItsInset`
  og fire i `TabletFeatureTest` (episodenummeret blir ikkje vist på TV, og TV-logoen er breiare enn testen
  ventar). Dei same 5 feila på same måte med beta28 bygd frå `v1.0.0-beta28` på same emulator, så dei er ikkje
  nye.
- Native bibliotek: dei 20 `.so`-filene er byte for byte like beta28. Kjeldearkiva for FFmpeg og dei native
  undertekstbiblioteka er difor dei same som ved beta28 (same digest). `check-native-subtitles.py`: OK.
- APK: 28083856 byte, SHA-256 `6fe57ea2fb13fd96e24cb0ca15b1e56607c820d81847666a4fce2dfa320954a6`,
  sertifikat `36fa94f0…1a10`, pakke `app.reelstack`, versjon `1.0.0-beta29`/166, ikkje feilsøkbar.
  Mappinga har same `pg_map_id` som APK-et.

## På eining

- Shield (lokalt bygg med same kjelde før versjonsløftet): Heim med stort toppbilete og «Sjå vidare» nedst,
  neste rad skjult; ratingar fyrst og «10m att»; kort i fokus løfta utan ring og dei andre dempa; «Kongen
  befaler» og «Skal vi danse» heile i korta. Eingongsflyttinga gav runde hjørne og løft på Shield, som hadde
  mjuke hjørne og kvit ring lagra.

## Publisering og oppdatering

Blir fylt inn etter publisering.
