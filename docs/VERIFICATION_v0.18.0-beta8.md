# Verifisering — Spole 0.18.0-beta8

Dato: 24. september 2026

## Bygg og artefakt

- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease`
  fullførte med **BUILD SUCCESSFUL** på Windows. Den første køyringa stoppa på to lint-feil
  (`RememberInComposition`: `FocusRequester` laga utan `remember` i menyen). Dei vart retta før
  bygget som er publisert.
- Einingstestar: **757 køyrde, 0 feila, 0 feil og 0 hoppa over** (740 i beta7; 17 nye).
- Lint: **0 feil**, 161 åtvaringar og 1 hint. Auken frå 148 er i hovudsak strengar som vart
  til overs då dei gamle raddialogane forsvann.
- Produksjons-APK: `app.reelstack`, `0.18.0-beta8` / versionCode `128`, minSdk `26`, targetSdk `36`,
  universell for fire ABI-ar, ikkje `debuggable`, med FFmpeg-lisensane.
- APK: `Spole-v0.18.0-beta8.apk`, 11 380 758 byte, SHA-256
  `3596fda2da69f7dd5227fade6c9ed1825711123655d7e1c49488a41682172ba5` (bygget etter layoutrettinga; testar og lint: 757/0 og 0 feil).
- Signatur: sertifikat-SHA-256 `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- FFmpeg-kjeldepakken er uendra: SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

- Instrumenteringstestar på den isolerte mobiltestprofilen (5562), heile pakka: **376 køyrde,
  362 bestått, 14 feila**. Alle 14 er blant dei 15 kjende feila som fanst i beta6 og beta7
  (sjå `VERIFICATION_v0.18.0-beta7.md`). Ingen er nye. `UiConsistencyTest` går no gjennom, etter at
  testen vart skriven om til den nye menyen. `HomeLayoutUiTest` (3), `ReelstackSmokeTest` (med
  menyen i den ekte appen), `HomeMediaRowsTest`, `HomeHeaderTest`, `PersonalizationTest`,
  `MediaRefinementTest`, `ReleaseRowsTest` og `TabletLayoutFollowupTest` gjekk gjennom.
- Etter pakka vart ein layoutfeil i menyen retta: «Alle bibliotek» braut over to linjer, fordi
  knappen og pilane delte linja likt. Menyklassane vart køyrde på nytt mot det endelege bygget
  (sjå under).

## Tilpass framsida

Dekt av einingstestar:

- `HomeLayoutTest` (11): overgang frå dei gamle innstillingane (éin brytar blir til éi rad per
  tenar, «Neste episode» frå personaliseringa), rekkjefølgja blir behalden, synlegheit per teneste,
  flytting som hoppar over rader som ikkje er lista, lagring og ukjende rader, biblioteksval per
  rad, overgang frå biblioteksvalet i Bibliotek-fana, og henteplanen (skjulte rader blir ikkje
  henta, hald fram blir alltid henta).
- `HomeLayoutEditorTest` (6, Robolectric, mobil): seriar frå Emby blir verande når alt frå Jellyfin
  blir skjult, ein tenar utan adresse har ingen rader, flytting til toppen og tilbakestilling,
  bibliotek per rad (berre bibliotek som kan fylle rada), alle kryssa av betyr «alle», og
  skriftstorleik 2.0.

## Visuell kontroll

- Mobil (5562, demodata): menyen viser filterbrikker med tenestelogo, rader per teneste med
  brytar, «Alle bibliotek» og pilar. Ein layoutfeil vart retta før det endelege bygget. Med
  skriftstorleik 2.0 blir ingenting klipt; brikker og knappar bryt over fleire linjer.
- `HomeLayoutUiTest`, `UiConsistencyTest`, `ReelstackSmokeTest` og `HomeMediaRowsTest` mot det
  endelege bygget: **OK (20 tests)**.

## Installasjon og oppdatering

Kjelde-commit og publisert tagg: `aa58e8f8abe31e04c8b6b63ccf2b74a25f2a149e`.
Release: <https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta8>

- Fem assets. GitHub-digest `sha256:3596fda2…ba5` er lik den lokale hashen. Den offentlege lista
  viser `draft=false` og `prerelease=true`, og ei offentleg nedlasting utan autorisasjon har same
  SHA-256.
- **Mobil (5560), `adb install -r` før publisering:** 0.18.0-beta7 (127) → 0.18.0-beta8 (128) med
  uendra `firstInstallTime`. Appen startar utan krasj.
- **TV (5564), ekte oppdatering gjennom appen:** 0.18.0-beta7 viste sjølv «Spole v0.18.0-beta8 er
  klar». Sjå oppdatering → Last ned oppdatering (10,9 MB) → Installer oppdatering → Android
  «Update» → «App installed» → Open. versionCode 128, uendra `firstInstallTime`, same konto
  (Øyvind · Seerr) og «Sjå vidare · Jellyfin» med ekte titlar. Ingen krasj.

## Tilpass framsida med ekte data (TV)

- Overgangen tok med det brukaren hadde: «Spelar no» var av, og «Hald fram · Jellyfin» viste
  **4 av 5 bibliotek**, der Barne-TV var av som i biblioteksvalet frå før. Biblioteka (Filmar,
  Seriar, Barnefilmar, Barne-TV, Samlingar) vart henta frå tenaren.
- Jellyfin og Emby hadde kvar sine rader.
- «Hald fram · Jellyfin» slått av: rada vart merkt «Skjult», og framsida viste «Sjå vidare · Emby»
  utan Jellyfin-rada. Slått på att: «Sjå vidare · Jellyfin» kom tilbake med same titlar. Ingen
  andre innstillingar vart endra.
