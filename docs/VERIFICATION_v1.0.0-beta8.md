# Verifisering av Spole 1.0.0-beta8

9. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 145.

## Innhald

Alt under «Etter beta7» i [SMART_SHELVES_2026-10-08.md](SMART_SHELVES_2026-10-08.md):

- Sida for ei hylle får bakgrunnsbilete, ei vifte av plakatar, «Overrask meg», filter, sortering,
  «Skjul sette» og «Ny»-merke.
- Hyllesvar blir hugsa (`FileShelfAnswerCache`).
- Ei ny hylle står på framsida med ein gong, og ei eiga hylle startar på «Heile året».
- Fjernkontrollen held kolonna i alle rutenett.
- Biblioteksfiltera gjeld ikkje lenger hyllene.

Toppen av hyllesida er rydda etter tilbakemelding på første utkast. Merket «Smart hylle», regelen
som brikker og tellelinja er borte. Tala står berre på filteret, ikkje i bolkoverskriftene, og
toppfeltet er lågare. Alt anna er som i beta7.

## Bygg, einingstestar og lint

- Fullt bygg (`testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`,
  `--max-workers=2`) avslutta med `BUILD SUCCESSFUL`, kode 0. Einingstestane var `UP-TO-DATE` frå
  køyringa 16:34 på same hovudkjelde. Mellom dei to bygga vart berre éin import i ein Android-test
  retta.
- 959 einingstestar: 958 bestått, 1 hoppa over (valfri prøve mot ekte nett), 0 feil.
- Lint: 0 feil, 185 åtvaringar og 1 hint, same tal som for beta7.
- `check-release-consistency.py`: versjon, README, CHANGELOG og release-notat er i takt.

## Android-testar

Køyrde på isolert TV-profil 5566 med debug-bygget frå same kjelde, klasse for klasse med
`pm clear` før kvar klasse.

| Testklasse | Resultat |
| --- | --- |
| `SmartShelvesTest` | 23/23 |
| `SteadyRemoteRowsTest` | 1/1 |
| `LibraryPresentationUiTest`, `LibraryBrowserUiTest` | 9/9, 3/3 |
| `DesignRefreshUiTest` | 22/22 |
| `HomeMediaRowsTest`, `HomeLayoutUiTest`, `HomeHeaderTest` | 6/6, 3/3, 5/5 |
| `SeasonalTouchesTest` | 8/8 |
| `SetupAndDiscoverTest`, `UiConsistencyTest`, `ViewerExperienceTest` | 5/5, 3/3, 3/3 |
| `FocusOutlineTest`, `TvNavigationIntegrationTest` | 3/3, 2/2 |
| `LanguageResourcesTest`, `WideNavigationSettingsTest` | 6/6, 6/6 |
| `MediaRefinementTest`, `TvSettingsRedesignTest`, `TvUpdateUiTest` | 5/5, 6/6, 4/4 |
| `TvRefinementUiTest` | 37/38 |
| `RequestHistoryUiTest` | 4/7 |

Nye i `SmartShelvesTest`:

- `theTopHoldsOnlyTheNameAndTheActions`: ingen «Smart hylle», ingen regel og ingen tellelinje øvst.
  Talet står på filteret og ikkje i bolkoverskrifta.
- `theFirstCoversAreInViewOnArrival`: på ein TV-skjerm på 960 × 540 dp er heile første plakat
  synleg når sida opnar.

Feila er dei same som før, på kjelde eldre enn endringa:

- `TvRefinementUiTest.libraryEditorGivesARemoteItsFirstFocus`.
- `RequestHistoryUiTest`: `pageFailure…`, `loadOlder…` og `failedRefresh…`. `performScrollTo` finn
  ikkje knappar som rutenettet på TV aldri har lagt ut.

Mobilprofilen 5562 er ikkje køyrd. Tilbakepila på same linje som namnet gjeld berre mobil. Ho er
ikkje kontrollert på ein telefonskjerm.

## Signert produksjonsartefakt

- Universal-APK: 11 769 614 byte. `app.reelstack`, kode 145, `1.0.0-beta8`, minSdk 26 og
  targetSdk 36. Ikkje debuggable. Native kode for arm64-v8a, armeabi-v7a, x86 og x86_64.
- SHA-256: `91873ee763c2b6d9f466b508a8d0369874ebcab041859b97efad834f06e1a3db`.
- Sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping: 95 039 089 byte, SHA-256 `8e61e12c3f016650d8d1f771be17fa4636ccb9aeb907490b2cb3595519e3ac80`.
- `libffmpegJNI.so` og begge FFmpeg-lisensfilene er byteidentiske med beta7 for alle fire ABI-ane.
  Kjelde- og relenkingsarkivet er difor det same: 30 790 595 byte, SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Signert APK er installert med `install -r` over den offisielle beta7 på den lagra TV-profilen 5564
  (ekte Jellyfin og Emby):
  - Versjonen vart `1.0.0-beta8` / 145. Første installasjonstid er framleis 9. september 2026.
  - Appen starta på Heim med ekte rader frå Jellyfin og Emby.
  - Ei Halloween-hylle frå malen gav 45 filmar og 18 seriar. Sida opna med namnet, «Overrask meg»
    og «Endre», og deretter filteret (Alt 63, Filmar 45, Seriar 18), sorteringa og «Skjul sette».
    Bolken «Filmar» står utan tal, og heile første plakatrad var synleg. Fokus starta på «Overrask meg».
  - Testhylla vart sletta. Biblioteket, sidemenyen og framsida er utan hylla, som før.
  - Under testen spurde Android om USB-feilsøking for ein ukjend nøkkel frå ein adb-tenar på
    Windows-sida. Førespurnaden vart avvist, og tilgangane på eininga er uendra.

## Offentleg release

- Publisert som prerelease, ikkje draft, med nøyaktig éin universal-APK og fem assets:
  https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta8 . Stabil latest er ikkje endra.
- Kjelde/tag: `17bc0823d87a212ff41138740d03f74a1a746d79`, ein fast-forward av `main` frå beta7.
  `SOURCE_COMMIT.txt` peikar på same commit.
- Alle fem asset-digestar og storleikar i kladden samsvarte med arkivfilene.
- Den uautentiserte release-lista viser beta8 med `draft=false`, `prerelease=true` og APK-digest
  `sha256:91873ee7…a3db`. Den offentleg nedlasta APK-en er 11 769 614 byte og har same SHA-256 som det
  signerte bygget.
- GitHub-kontrollen «Bygg og test» på `main` for `17bc082` fullførte med `success`.

## Oppdatering gjennom appen

- **Isolert TV-profil 5566** (offisiell `1.0.0-beta7` / 144):
  - Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no fann `v1.0.0-beta8`
    (nedlasting 11,2 MB) med dei offentlege release-notata.
  - «Last ned oppdatering» → «Installer oppdatering» gjekk gjennom appen sine kontrollar. Android
    spurde «Do you want to update this app?», og etter «Update» meldte han «App installed.» Play Protect
    spurde ikkje.
  - Installert pakke er `1.0.0-beta8` / kode 145, sist oppdatert 9. oktober 2026 kl. 17:10:08. Første
    installasjonstid er framleis 19. september 2026 kl. 14:51:26.
  - «Open» starta appen på Heim, og krasjloggen var tom.
- Kontoar og preferansar over ein ekte oppdatering er kontrollerte på TV-profilen 5564 med `install -r`
  (sjå over). Den har ekte Jellyfin- og Emby-kontoar.
- Mobilprofilen 5560 er ikkje oppdatert. Play Protect stoppar installasjonen der (sjå beta7), og
  profilen står framleis på ei eldre utgåve.
