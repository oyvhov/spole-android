# Verifisering av Spole 1.0.0-beta10

12. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 147.

## Innhald

Smarte hyller med meir plakat og mindre tekst, sjå «Etter beta9» i
[SMART_SHELVES_2026-10-08.md](SMART_SHELVES_2026-10-08.md):

- Flisa i biblioteket er tre plakatar som fyller henne, med namnet på éi linje og utan undertittel.
- Rada på framsida har hylleikonet framfor tittelen og sluttar i eit «Sjå alle»-kort.
- På mobil står brikkene på hyllesida på éi rad som rullar sidelengs.
- Byggjaren har mista forklaringsavsnitta; periodane viser datoane sine, og sjangrar faldar seg bak
  «Fleire sjangrar».

Alt anna er som i beta9. Bygd frå `origin/main` (beta9, `1ffda12`) i arbeidsmappa `spole-sesong`.

## Bygg, einingstestar og lint

- Fullt bygg (`testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`,
  `--max-workers=2`) avslutta med `BUILD SUCCESSFUL`, kode 0, etter 3 minutt 46 sekund.
- 976 einingstestar: 975 bestått, 1 hoppa over (valfri prøve mot ekte nett), 0 feil. Ny:
  `SmartShelfTest.aPeriodKnowsItsWholeRun` (Halloween, jul midt i perioden, påske 2027, heile året).
- Lint: 0 feil, 189 åtvaringar og 1 hint, same tal som for beta9.
- `check-release-consistency.py` og `check-translations.py`: i takt (1656 nøklar).

## Android-testar

Køyrde på isolert TV-profil 5566 med debug-bygget frå same kjelde, klasse for klasse med
`pm clear` før kvar klasse.

| Testklasse | Resultat |
| --- | --- |
| `SmartShelvesTest` | 27/27 |
| `HomeMediaRowsTest`, `HomeLayoutUiTest` | 6/6, 3/3 |
| `LibraryBrowserUiTest` | 3/3 |
| `DesignRefreshUiTest` | 22/22 |
| `SeasonalTouchesTest` | 8/8 |
| `LibraryPresentationUiTest` | 9/10 |
| `TvRefinementUiTest` | 36/38 |

Nye i `SmartShelvesTest`:

- `aTileIsItsPostersWithTheNameOnOneLine`: flisa er 2:1, namnet er éi linje ved telefonbreidd, og
  namnet er det einaste som står på flisa.
- `aPhoneKeepsTheChoicesOnOneLine`: ved 412 dp er brikkerada under 60 dp høg, og «Skjul sette» kjem
  fram ved å rulle rada.
- `aLongGenreListFoldsBehindMore`: tjue sjangrar gir seksten brikker og «Fleire sjangrar», den valde
  først; eit trykk viser resten.
- `aPeriodChipCarriesItsDates`: Halloween-brikka har «20» i seg; «Heile året» har ingen tal.
- `theLibraryRowOpensShelvesAndMakesNewOnes` sjekkar no òg at regelen ikkje står på flisa.
- `aShelfOnHomeGetsARowPerServerAndAWayIn` går gjennom «Sjå alle»-kortet, som har same testmerke som
  knappen hadde.

Dei tre feila er kontrollerte mot uendra `origin/main` (`1ffda12`) bygd i ei rein arbeidsmappe og
køyrd på same profil:

- `LibraryPresentationUiTest.libraryOverviewHasOneNextRowIndependentOfHomeSwitches` finn ikkje
  `resume-card-new` ved 412 × 840 på ein TV. Feilar likt på beta9-kjelde; beta9 køyrde klassen berre
  på mobilprofilen.
- `TvRefinementUiTest.remoteFocusKeepsRailGeometryAndCaptionBaselinesStable`: den lange tittelen i
  `ResumeRail` blir to linjer der testen ventar éi. Feilar likt på beta9-kjelde; klassen vart ikkje
  køyrd på TV for beta9.
- `TvRefinementUiTest.libraryEditorGivesARemoteItsFirstFocus`: kjend frå før.

Mobilprofilen 5562 er ikkje køyrd. Telefonbreidda er prøvd gjennom `ForcedSize` i testane over.

## Signert produksjonsartefakt

- Universal-APK: 11 786 702 byte. `app.reelstack`, kode 147, `1.0.0-beta10`, minSdk 26 og
  targetSdk 36. Ikkje debuggable. Native kode for arm64-v8a, armeabi-v7a, x86 og x86_64.
- SHA-256: `10d1eb68d674202cb1d67b7208952ced1e5a500ac622b502d9d997067fd64630`.
- Sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping: 95 459 760 byte, SHA-256 `6c6cf362bad25daf269eb2c272aab595478d4c078146fc0cdb580db1e1e1aaa4`.
- `libffmpegJNI.so` og begge FFmpeg-lisensfilene er byteidentiske med beta8 for alle fire ABI-ane.
  Kjelde- og relenkingsarkivet er difor det same: 30 790 595 byte, SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

## Ekte data

Signert APK er installert med `install -r` på den lagra TV-profilen 5564 (ekte Jellyfin og Emby),
først eit lokalt bygg med berre flisa (kode 146) over beta9, så beta10 (147) over det. Første
installasjonstid er framleis 9. september 2026, og appen starta på Heim med ekte rader begge gongene.

- Ei Halloween-hylle frå malen gav 45 filmar og 18 seriar. Flisa under «Smarte hyller» i biblioteket
  var tre plakatar kant til kant med «Halloween» og gresskaret på éi linje nedst, 480 × 240 px.
- På framsida stod rada «Halloween» med gresskaret framfor tittelen og Jellyfin-merket til høgre.
  Ytst i rada, etter alle titlane, stod «Sjå alle»-kortet i same ramme som plakatane. Eit trykk opna
  hyllesida med fokus på «Overrask meg».
- I byggjaren («Endre hylla») stod sjangrane med «Grøssar» først og «Fleire sjangrar (20)» etter
  seksten brikker. Periodebrikkene las «Halloween · 20. okt.–1. nov.», «Jul · 1. des.–13. jan.»,
  «Advent · 1. des.–24. des.» og «Påske · 21. mars–29. mars». Forklaringsavsnitta under Sjangrar,
  Stikkord og Når var borte; «Gir minst 45 filmar og 18 seriar.» stod øvst som før.
- Testhylla vart sletta frå byggjaren («Slett hylla» → «Trykk igjen for å slette»). Biblioteket,
  sidemenyen og framsida er utan hylla. Brytarane under «Tilpass biblioteksida» vart kontrollerte mot
  beta8-bileta etter at eit fjernkontrollskript gjekk feil; ingen var endra.
- Mobilbreidd (412 dp) er prøvd i Android-testane over, ikkje på ein telefon. Skjermbilete ligg
  berre lokalt.

## Offentleg release

- Publisert som prerelease, ikkje draft, med nøyaktig éin universal-APK og fem assets:
  https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta10 . Stabil latest er ikkje endra.
- Kjelde/tag: `8c561659bf55158c8bc0b35df8b42cb1ea17294e`, ein fast-forward av `main` frå beta9.
  `SOURCE_COMMIT.txt` peikar på same commit.
- Alle fem asset-digestar og storleikar i kladden samsvarte med arkivfilene.
- Den uautentiserte release-lista viser beta10 med `draft=false`, `prerelease=true` og APK-digest
  `sha256:10d1eb68…4630`. Den offentleg nedlasta APK-en er 11 786 702 byte og har same SHA-256 som det
  signerte bygget.
- GitHub-kontrollen «Bygg og test» på `main` for `8c56165` fullførte med `success`.

## Oppdatering gjennom appen

- **Isolert TV-profil 5566**, først sett til den offisielle beta9-APK-en frå GitHub (same SHA-256 som
  release-digesten, `install -r` over beta8):
  - Ved opning varsla appen sjølv «Spole v1.0.0-beta10 er klar». «Sjå oppdatering» opna panelet med
    `v1.0.0-beta10`, «Nedlasting · 11,2 MB» og dei offentlege release-notata.
  - «Last ned oppdatering» → «Installer oppdatering» gjekk gjennom appen sine kontrollar. Android
    spurde «Do you want to update this app?», og etter «Update» meldte han «App installed.» Play
    Protect spurde ikkje.
  - Installert pakke er `1.0.0-beta10` / kode 147, sist oppdatert 12. oktober 2026 kl. 01:03:27.
    Første installasjonstid er framleis 19. september 2026 kl. 14:51:26.
  - «Open» starta appen på Heim, og krasjloggen var tom.
- Kontoar og preferansar over ein ekte oppdatering er kontrollerte på TV-profilen 5564 med `install -r`
  (sjå over). Den har ekte Jellyfin- og Emby-kontoar.
- Mobilprofilen 5560 er ikkje oppdatert; Play Protect stoppar installasjonen der (sjå beta7).
