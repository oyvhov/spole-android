# Verifisering av Spole 1.0.0-beta30

1. november 2026. Produksjonspakke `app.reelstack`, versjonskode 167. Bygd oppå beta29 (`0a308a8`).

## Omfang

- Rader for Stova (`content://app.reelstack.launcher/rows?kind=&user=`): «Hald fram», nye filmar og nye
  seriar for ein Spole-person, henta av Spole med si innlogging (`MediaSyncRepository.launcherRows`,
  `launcher/LauncherRows.kt`). Kolonnen `official` har aldersgrensa slik tenaren gir ho. Nye episodar av
  same serie er eitt kort.
- Handlingar for Stova (`call("item_action")`): `watched` og `remove_resume`; `spole://open?...&start=0`
  spelar frå starten.
- Spelestatus til Stova (`app.reelstack.action.PLAYBACK_STATE`, `state` = playing / paused / stopped),
  berre ved endring (`launcher/LauncherPlayback.kt`).
- Emby: `OfficialRating` blir bede om saman med dei andre ratingane (Emby utelèt han elles).
- Norske aldersgrenser: «NO-15» blir «15 år», «NO-A» blir «Alle».
- TV-hyller: marg ved høgre kant for kortet med fokus, og høgre-pila stoppar ved enden av hylla.
- Kortstil som i Stova: resten av hylla blir ikkje lenger dempa under «Løft» (`LocalShelfFocused` er borte).

## Bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`: BUILD SUCCESSFUL.
  1376 einingstestar, 0 feil, éin valfri nettverkstest hoppa over. Lint: 0 feil. Release-data: OK
  (`check-release-consistency.py` i WSL).
- Isolert TV 5566 (`SpoleTestRunner`): `TvRefinementUiTest`, `TvNavigationIntegrationTest`,
  `TvSettingsRedesignTest`, `PersonalizationTest`, `TabletFeatureTest`, `DesignRefreshUiTest`,
  `HomeVisualPolishTest`, `LauncherLibraryIntegrationTest`, `AccountPanelsTest`, `LanguageResourcesTest`,
  `LinkedLoginTest`, `FocusOutlineTest` og `HomeMediaRowsTest`: 119 køyrde, 5 feil. Dei 5 er dei same
  nettbrett-testane som feila med beta28 og beta29 på same emulator
  (`PersonalizationTest.wideMediaReachesTheEdgeButProfileKeepsItsInset` og fire i `TabletFeatureTest`).
- Native bibliotek: dei 20 `.so`-filene er byte for byte like beta29, så kjeldearkiva for FFmpeg og dei
  native undertekstbiblioteka er dei same som ved beta29 (same digest). `check-native-subtitles.py`: OK
  (fire ABI-ar, 16 KB-justering på 64 bit, åtte lisensar).
- APK: 28084292 byte, SHA-256 `f934528236ae5ff87c4df3eaba5da8612e3837aa46e0e2b7dbd6302fd82f58bd`,
  sertifikat `36fa94f0…1a10`, pakke `app.reelstack`, versjon `1.0.0-beta30`/167, ikkje feilsøkbar.
  Mappinga har same `pg_map_id` som APK-et (`a50df5e1…3d6a`). Ei tom line til slutt i `FocusOutline.kt`
  vart fjerna etter det første bygget (`git diff --check`), så `assembleRelease` vart køyrt på nytt frå
  akkurat den committa kjelda. R8 gav då ei anna mapping, så det er dette andre bygget som er publisert,
  og sjekkane over er gjort på det.

## På eining

- Shield: den signerte beta30-APK-en (første bygget, same kjelde utan den tomme lina) lagd inn med `adb install -r` over det lokale bygget før publisering.
  Versjonskode 167. Kontoar, profilen «Eilev» og biblioteka var der. På «Favorittar» voks kortet med
  fokus utan at dei andre vart dempa.
- Shield og Loftet (lokalt bygg med same kjelde før versjonsløftet), 1. november: Stova-profilen «Eilev»
  (følgjer Emby-personen Eilev, ingen konto i Stova) fekk «Hald fram», «Nyleg lagt til · Filmar» og
  «Nyleg lagt til · Seriar» frå Spole, og heroen fekk bilete og «6 år». «Nyleg lagt til · Seriar» viste
  éitt TMNT-kort i staden for fire. På Spole sin heim stod kortet ytst til høgre heilt inne med marg, og
  høgre-pila på «Pachinko» (siste i «Sjå vidare») vart verande der. Ei ekte avspeling i Spole fekk Stova
  (i bakgrunnen) til å kople seg til Home Assistant for kino-modus.

## Publisering og oppdatering

(fyllest ut)
