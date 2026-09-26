# Verifisering — Spole 0.18.0-beta9

Dato: 26. september 2026

Utgangspunkt: 0.18.0-beta8 med dei ucommitta endringane som alt låg i arbeidsmappa frå
25. september: omsette sjangernamn i detaljvisinga, timar i tidsvisinga i TV-spelaren for lange
filmar, omsett «Stats for Nerds», fri botnkant i TV-biblioteket og nye TV-testar for toppbiletet og
titlar over to linjer. Dei er gjennomgåtte og følgjer med i denne utgåva.

## Bygg og artefakt

- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease`
  fullførte med **BUILD SUCCESSFUL** (5 min 6 s) på Windows. `check-release-consistency.py` og
  `check-translations.py` (1443 nøklar) gjekk gjennom.
- Einingstestar: **783 køyrde, 0 feila, 0 feil og 0 hoppa over**. Lint: **0 feil**, 161 åtvaringar.
- Produksjons-APK: `app.reelstack`, `0.18.0-beta9` / versionCode `129`, minSdk `26`, targetSdk `36`,
  universell for fire ABI-ar, ikkje `debuggable`, med FFmpeg-lisensane.
- APK: `Spole-v0.18.0-beta9.apk`, 11 413 966 byte, SHA-256
  `6b57a67c71b8308a5edf04107b4a1b57d56453d6bc5eea745bc6f4eb2cda340f`.
- Signatur: sertifikat-SHA-256 `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- FFmpeg-modulen er uendra sidan beta8, og kjeldepakken er den same: SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

## Kva brukaren melde

1. Nedlastingssida: kortet øvst braut teksten bokstav for bokstav («La / st / er / ne / d»), og
   framdriftslinja var berre ein strek og ein prikk.
2. «Sjå fråkopla» på ei ferdig nedlasting krasja appen.
3. Nedlastingar skal kunne skjulast frå menyen på telefon og nettbrett, ha eigen plass i
   Innstillingar, og ikkje vere i menyen som standard.
4. Søket på mobil skal vere eit globalt søk, ikkje hoppe til Oppdag. Brukaren valde «ikon øvst +
   val i Innstillingar».
5. På TV startar ein episode, stoppar litt «for å leggje inn underteksten», og held fram.
6. Innlastinga på framsida er ikkje optimal.

## Årsaker og rettingar

| # | Årsak | Retting |
|---|---|---|
| 1 | `Row` målte pauseknappen før teksten med `weight(1f)`; teksten fekk resten. Sporet var same farge som kortet. | `ActionRow` legg knappen under når teksten ville fått under 45 % av breidda. Spor i sidefargen, utan stopp-prikk. Norsk desimalkomma. |
| 2 | `OfflinePlayerActivity.open` fekk app-konteksten utan `FLAG_ACTIVITY_NEW_TASK` → `AndroidRuntimeException`. | Aktivitetskonteksten blir send; `open` legg til flagget når konteksten ikkje er ein aktivitet. |
| 3 | Nedlastingar var hardkoda inn i menyen. | `Personalization.showDownloadsInMenu` (av), `touchMenu()`, rad i Innstillingar, tilbakepil og `downloadsReturnTab`. |
| 4 | Søket var ein kopi av Oppdag med delt søkjefelt. | `GlobalSearchScreen` med eigen `SearchSlice`; ikon ved profilbiletet; søkelina som val. |
| 5 | Valt sidecar-undertekst held Media3 sin videolastar att medan fila blir henta. Emby treng om lag 8 s første gong; grensa var 8 s. | Teksten blir henta ved sida av videoen og slått på når fila er i minnet. Grense 60/90 s. Lydspråk sett før `prepare()` ved direkte avspeling. |
| 6 | Skjelett ved kvar oppfrisking, grense på samanslått liste, skiftande rekkjefølgje, retry kvart minutt. | `loadedSources`, grenser per tenar, fast flettrekkjefølgje, helt som følgjer tittel, back-off, UI før Room. |

## Einingstestar

- `testDebugUnitTest`: **783 køyrde, 0 feila, 0 hoppa over** (757 i beta8; 26 nye).
- Funne på eininga og retta: brytarane «Nedlastingar i menyen» og «Søkelina på framsida» lagra
  valet, men skjermen oppdaterte seg ikkje når berre eitt av dei endra seg, fordi
  `observePersonalization` berre høyrer på ei liste med nøklar. `PersonalizationObserverTest`
  feilar utan rettinga og går gjennom med ho.
- Nye: `OfflinePlayerLaunchTest` (3, gjenskaper krasjet før rettinga), `DownloadsAndSearchUiTest`
  (7, mellom anna nedlastingskortet på skriftstorleik 2.0), `TouchMenuTest` (4),
  `GlobalSearchUiStateTest` (3), `HomeRefreshStabilityTest` (2), `HomeRowMergeTest` (3),
  `PersonalizationObserverTest` (1), norsk desimalkomma i `OfflineDownloadsPresentationTest`,
  back-off i `HomeFeedRecoveryTest`.
- `lintDebug`: **0 feil**, 161 åtvaringar (same tal som beta8), 1 hint.

## Instrumentering (isolert mobilprofil 5562)

Profilen stod på systemskrift 2.0 frå ei tidlegare manuell økt; det gjorde at
`ActivityAdaptiveTest` feila. Skrifta vart sett til 1.0 før køyringane, som i referansekøyringane.

Først vart klassane som endringane rører ved, køyrde målretta. `HomeSearchNavigationTest` (4) kom
då aldri forbi det første oppsettvalet (`setup-other`), same årsak som dei tre kjende feila i
klassen, og `CompactNavigationTest` (2, kjende) rekna med seks faner; med Nedlastingar ute av
menyen får fem etikettar plass heile. Begge testane er retta, og testen for botnlinja vel no
Nedlastingar inn i menyen for å prøve den trongaste linja.

Eit forsøk på heile pakka i éin `am instrument` stoppa etter 22 testar med «Process crashed» i
`Alpha12UiTest`. Pakka vart difor køyrd éin klasse om gongen med ferske APK-ar
(`app/build/verify-2026-09-26/run-per-class.sh`), så eit krasj ikkje kan stoppe resten.

- **Heile pakka, klasse for klasse:** **380 køyrde, 366 bestått, 14 feila**, ingen krasj, på
  25 minutt (beta7: 377 køyrde, 15 feila). `Alpha12UiTest` (5) og `AdaptiveDialogTest` (2) gjekk
  gjennom.
- **Køyrde om att med tømde appdata** (`pm clear` før kvar klasse, `run-per-class-clean.sh`):
  `HomeSearchNavigationTest` 4/4, `ReelstackSmokeTest` 8/8 og `SheetInteractionTest` 10/10. Dei
  fem feila kom av at klassane deler appdata: oppstartsdekselet venta fem sekund på ein konto som
  ein tidlegare klasse hadde lagt att, og «Kalender» mangla på framsida. Arktesten ventar berre
  eitt sekund på at arket lukkar seg.
- **Att, alle frå før denne runden:**
  - `JellyfinPlayerTest` (7): dei same sju som i beta7. Den åttande derifrå,
    `disabledSubtitleIsWarmedAndEnablesWithoutAnotherDownload`, er erstatta av
    `subtitlesOffFetchNothingAndSwitchingOnLoadsTheTextOnce`. Alle fem undertekst-testane går
    gjennom: HLS-tekstspor, av/på utan ny henting, `slowSubtitleExtractionNeverStallsThePicture`,
    tenarfeil og Tilbake under heng.
  - `TvLibraryRegressionTest` (1), som i beta7.
- **Retta:** `DesignRefreshUiTest.libraryTvKeepsLastShelfAboveSafeBottomEdge`, ein ny test i dei
  ucommitta endringane frå 25. september, rekna TV-kanten om til pikslar med tettleiken til
  telefonen, medan `ForcedSize` skalerer innhaldet ned. Han måler no med tettleiken i det
  skalerte biletet.
- `UiConsistencyTest`, `HomeSearchNavigationTest` og `CompactNavigationTest`, som feila i beta7,
  går gjennom.
- **Mot det endelege bygget** (0.18.0-beta9, tømde appdata): `DesignRefreshUiTest` (22),
  `ReelstackSmokeTest` (8), `HomeSearchNavigationTest` (4), `OfflineDownloadsUiTest` (1) og
  `CompactNavigationTest` (4): **OK (39 tests)**.

## TV med ekte data (emulator-5564, signert førehandsbygg av same kode, `install -r`)

Episodar av *Sterling Point* på Emby som aldri var spelte, så tenaren måtte hente ut teksten frå
MKV-fila (om lag 30 tekstspor). Avspelingsstatus lesen frå MediaSession fire gonger i sekundet.

| Bygg | Episode | Start | Etter start |
|---|---|---|---|
| Før (installert beta8 + arbeidsmappa) | S1 E6 | spelar ved 1,8 s med 2 s buffer | **stoppar ved 3,3 s i 5,3 s**, så vidare |
| Etter, første versjon (8 s grense) | S1 E7 | spelar ved 2,2 s | ingen stopp; teksten nådde ikkje fram innan 8 s |
| Etter, endeleg | S1 E8 | spelar ved 2,5 s | **ingen stopp i 25 s**; `stage=subtitle ms=8113 result=ready`, norsk tekst synleg |

Framsida på TV: ut av appen og inn att (full oppfrisking) gav same helt og same kort i alle bilete
frå 0,7 til 4,2 s, utan skjelett.

Spor etter testen: E5–E8 står i «Hald fram» i TV-emulatoren si lokale avspelingsjournal i opptil
24 timar. Alle vart spelte under 5 % av lengda.

## Mobil (emulator-5560, signert førehandsbygg av same kode, `install -r`)

Profilen `Spole_Review` har **ingen innlogga tenester** (Jellyfin, Emby, Seerr, Radarr og Sonarr står
alle på «Server and sign-in»), så appen viser demodata. Ekte nedlasting og «Sjå fråkopla» kunne
difor ikkje prøvast der; krasjet er gjenskapt og retta i `OfflinePlayerLaunchTest`. Med
`-gpu swangle` var skjermbileta svarte og trykk kom ikkje fram; med `-gpu swiftshader` verka begge.

Kontrollert på eininga (engelsk appspråk):

- Heim: søkeikon ved profilbiletet, inga søkeline, botnmeny utan Nedlastingar.
- Søkeikonet opnar ein eigen søkeskjerm med tastatur og markør; treff under «Add something new»;
  éitt Tilbake fører til Heim, ikkje Oppdag.
- Innstillingar: rada «Downloads» under Playback opnar Nedlastingar med tilbakepil, menyen markerer
  Settings, og både pila og Tilbake fører til Innstillingar.
- «Search bar on Home» på: lina kjem under toppen og opnar same søk. Av: lina er borte med ein gong.
- «Downloads in the menu» på: fana kjem rett før Settings og opnar ei vanleg toppside. Av: borte.
  Begge vala vart sette tilbake til standard (av) etterpå.
- Skriftstorleik 2.0 på eininga: emulatoren mista biletet etter konfigurasjonsbytet (svart
  skjermbilete), så 2.0 er dekt av Robolectric- og instrumenteringstestane. Skrifta er sett
  tilbake til 1.0.

## Installasjon og oppdatering

- **Mobil (5560), `adb install -r` før publisering:** 0.18.0-beta8 (128) → 0.18.0-beta9 (129) med
  uendra `firstInstallTime`. Appen startar utan krasj og opnar på framsida med same innhald som før.
- **TV (5564), ekte oppdatering gjennom appen:** Innstillingar → Varsel og oppdatering →
  Appoppdateringar → Sjekk no viste «v0.18.0-beta9 · Nedlasting · 10,9 MB» med release-notata. Last
  ned oppdatering → Installer oppdatering → Android «Update» → «App installed» → Open.
  0.18.0-beta8 (128) → 0.18.0-beta9 (129), uendra `firstInstallTime`, same konto (Øyvind · Seerr)
  og «Sjå vidare · Jellyfin» med ekte titlar. Ingen krasj.

Kjelde-commit og publisert tagg: `2c896f98944e7c4e4bd8c51ed64b3e31973f43cf`.
Release: <https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta9>

- Fem assets. GitHub-digest `sha256:6b57a67c…340f` er lik den lokale hashen. Releasen er
  publisert som prerelease, står i den offentlege lista utan autorisasjon, og ei offentleg
  nedlasting har same SHA-256.
