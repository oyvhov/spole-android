# Verifisering av Spole 1.0.0-beta7

9. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 144.

## Innhald

Smarte hyller i biblioteket, på framsida og i sidemenyen, «Både stikkord og sjanger», sjangrar og
stikkord frå Emby, tastatur som ventar på OK i tekstfelt på TV, og at første kort på framsida ikkje
blir skore av når «Rolege overgangar» er av. Detaljar i
[SMART_SHELVES_2026-10-08.md](SMART_SHELVES_2026-10-08.md). Alt anna er som i beta6.

## Bygg, einingstestar og lint

- Fullt bygg (`testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`,
  `--max-workers=2`) avslutta med `BUILD SUCCESSFUL`, kode 0, etter 19 minutt 46 sekund.
- 951 einingstestar: 950 bestått, 1 hoppa over (valfri prøve mot ekte nett), 0 feil.
- Lint: 0 feil, 185 åtvaringar og 1 hint, same tal som for beta6.
- `check-release-consistency.py`: versjon, README, CHANGELOG og release-notat er i takt.

## Android-testar

Køyrde på isolert TV-profil 5566 med debug-bygget frå same kjelde som releasen.

| Testklasse | Resultat |
| --- | --- |
| `SmartShelvesTest` | 15/15 |
| `TvSetupTest` | 4/4 |
| `CombinedSetupUiTest` | 10/10 |
| `LoginExperienceTest` | 9/9 |
| `SetupAndDiscoverTest` | 5/5 |
| `LinkedLoginTest` | 7/7 |
| `AccountPanelsTest` | 11/11 |
| `TvSettingsRedesignTest` | 6/6 |
| `LibraryBrowserUiTest` | 3/3 |
| `HomeMediaRowsTest` | 6/6 |

Feil som òg finst på uendra beta6-kjelde (kjende frå før, på TV):

- `SheetInteractionTest` 3/10 og `SheetKeyboardFlowTest` 1/6. Feilane er touch-injeksjon og fokus i
  ark på TV.

Køyrde tidlegare i same arbeid, på same kode bortsett frå tekstfelta i oppsettet:

- `MediaRefinementTest` 5/5, `LibraryPresentationUiTest` 9/9, `DesignRefreshUiTest` 22/22,
  `SeasonalTouchesTest` 8/8, `WideNavigationSettingsTest` 6/6, `FocusOutlineTest` 3/3,
  `TvNavigationIntegrationTest` 2/2, `LanguageResourcesTest` 6/6, `HomeLayoutUiTest` 3/3 og
  `HomeHeaderTest` 5/5.
- `TvRefinementUiTest` 37/38: kjende `libraryEditorGivesARemoteItsFirstFocus`. På den ekte TV-en gir
  «Tilpass biblioteksida» opna med fjernkontrollen første fokus til første bibliotek.
- `ReelstackSmokeTest` 1/8: `@Before` kjem ikkje til Heim på TV, kjent frå før.

Mobilprofilen 5562 er ikkje køyrd. Han hadde full lagring.

## Signert produksjonsartefakt

- Universal-APK: 11 746 646 byte; `app.reelstack`, kode 144, `1.0.0-beta7`, minSdk 26 og
  targetSdk 36; ikkje debuggable. Native kode for arm64-v8a, armeabi-v7a, x86 og x86_64.
- SHA-256: `5acf2dccfc87d788911167de124f30030c19053516ec4daed69f3b69cb3bf830`.
- Sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping: 94 796 895 byte.
- `libffmpegJNI.so` er byteidentisk med beta6 for alle fire ABI-ane, og begge FFmpeg-lisensfilene er
  med. Kjelde- og relenkingsarkivet er det same som før: 30 790 595 byte, SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Signert APK er installert med `install -r` over beta6 på lagra TV-profil 5564 (ekte Jellyfin og
  Emby):
  - Versjonen vart `1.0.0-beta7` / 144, og første installasjonstid er framleis 9. september 2026.
  - Appen starta rett på Heim utan krasj. «Heile året», «Midnatt», «Lime» og «Rolege overgangar»
    var uendra.
  - Med Emby som kjelde viste byggjaren sjangrar og forslag til stikkord (halloween, christmas, dog …).
    «Ny smart hylle» frå «Tilpass biblioteksida» opna byggjaren.
  - I Jellyfin-oppsettet gjekk fjernkontrollen forbi adressefeltet utan tastatur (`mInputShown=false`).
  - Ingenting vart lagra, og kjelda er sett tilbake til Jellyfin.
- Mobilprofilen 5560 vart halden på beta6 til den ekte oppdateringa gjennom appen, sjå under.

## Offentleg release

- Publisert som prerelease, ikkje draft, med nøyaktig éin universal-APK og fem assets:
  https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta7 . Stabil latest er ikkje endra.
- Kjelde/tag: `cdd14dae812062ea62d15bf963690af8050c227d`, ein fast-forward av `main` frå beta6.
  `SOURCE_COMMIT.txt` peikar på same commit.
- Alle fem asset-digestar og storleikar i kladden samsvarte med arkivfilene.
- Den uautentiserte release-lista viser beta7 med `draft=false`, `prerelease=true` og APK-digest
  `sha256:5acf2dcc…f830`. Den offentleg nedlasta APK-en er 11 746 646 byte og har same SHA-256 som det
  signerte bygget.
- GitHub-kontrollen «Bygg og test» på `main` for `cdd14da` fullførte med `success`.

## Oppdatering gjennom appen

- **Mobilprofil 5560** (`1.0.0-beta6`): Settings → Notifications and updates → App updates → Check now
  fann `v1.0.0-beta7` med dei offentlege release-notata. «Download update» lasta ned APK-en, og «Install
  update» kom etter appen sine kontrollar. Android sin oppdateringsdialog vart godkjend.
  - Google Play Protect bad då om å sende appen til Google for skanning («Scan app»). Det er brukaren
    sitt val, så installasjonen vart avbroten med «Don't install app».
  - Profilen står framleis på `1.0.0-beta6`. Ingen appdata er endra.
- **Isolert TV-profil 5566** (`0.17.0-beta30` → offisiell beta6-APK med `install -r`):
  - Ved opning varsla appen sjølv «Spole v1.0.0-beta7 er klar». «Sjå oppdatering» → «Last ned
    oppdatering» → «Installer oppdatering» gjekk gjennom appen sine kontrollar.
  - Android meldte «App installed.» Play Protect spurde ikkje på TV-profilen.
  - Installert pakke er `1.0.0-beta7` / kode 144, sist oppdatert 9. oktober 2026 kl. 09:58:40. Første
    installasjonstid er framleis 19. september 2026 kl. 14:51:26.
  - Appen opna på Heim utan krasj.
- Kontoar og preferansar over ein ekte oppdatering er kontrollerte på TV-profilen 5564 med `install -r`
  (sjå over). Den har ekte Jellyfin- og Emby-kontoar.
