# Verifisering av Spole 1.0.0-beta11

14. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 148.

## Innhald

Dobbeltrykk på videoen, reine spolingsikon på sideflatene, større nettbrett-tidslinje og
rettingar i rapportering og oppdatering av episodeframdrift. Sjå
[mobilavspeling og offline](MOBILE_PLAYBACK_AND_OFFLINE.md).
Beta10 er fletta inn før denne utgåva blir bygd; endringane i smarte hyller er bevarte.

## Bygg, einingstestar og lint

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`,
  `--max-workers=2`: `BUILD SUCCESSFUL`, kode 0, etter 19 minutt.
- 981 einingstestar i 145 suitar: 980 bestått, 1 valfri nettverkstest hoppa over, 0 feil.
- Lint: 0 feil, 194 åtvaringar og 1 hint.
- Versjonskontrollen og språkkontrollen bestod: 1656 standardnøklar og éin tillaten standardnøkkel.
- Logg: `app/build/beta11-build.log`. Detaljar og skjermbilete ligg berre lokalt i byggmappa.

## Android-testar

Testane bruker debug- og test-APK frå den endelege runtime-kjelda, på eksisterande isolerte
profilar med `SpoleTestRunner`. Ingen testpakke vart køyrd på review-profilane 5560/5564.

| Profil og utval | Resultat |
| --- | --- |
| Mobil 5562: `PlayerSeekLayoutTest`, `JellyfinPlayerUiTest`, `LocalPlaybackStoreTest` | 14/14, 11/11, 7/7 |
| Mobil 5562: heile `JellyfinPlayerTest` | 38/41 |
| TV 5566: `TvPlaybackControlsTest`, `SmartShelvesTest` | 14/14, 27/27 |

Til saman 111 av 114 bestod. Dei tre feila vart køyrde med dei same testmetodane på den
uendra beta10-APK-en (kode 147) frå den reine arbeidsmappa `spole-sesong`, `0d5c464`
(berre dokumentasjon etter releasecommit `8c56165`). Alle tre feila likt der:

- `countdownAdvancesFromRealVideoBeforeTheCurrentEpisodeEnds`: episoden er avslutta når testen ventar tidleg nedteljing.
- `cancellingEarlyCountdownKeepsCurrentEpisode`: tidleg avbryting oppfyller ikkje testen sitt framdriftskrav.
- `miniPlayerKeepsVideoRunningAndReturnsWithoutRestarting`: timeout ved retur frå PiP.

Dette er reproduserte feil frå før, ikkje nye feil som berre opptrer i beta11. Dei er ikkje
retta i denne utgåva. Dei andre 38 integrasjonstestane dekkjer mellom anna faktisk lokal video,
spoling, pause, bakgrunn, rapportfeil, autentisering og kontoavgrensing.

Første mobilrunde vart avbroten medan fire emulatorar la press på WSL; review-mobilen viste
ein System UI-ANR. Etter at dei ferdige ekstra profilane vart avslutta utan nullstilling,
vart heile mobilutvalet køyrt på nytt. Tabellen over er frå denne avslutta køyringa, ikkje frå
den avbrotne runda. Mellombels lagringsgrense på testeininga er sett tilbake til opphavleg verdi.

Loggar: `app/build/release-v1.0.0-beta11/android-phone.log`, `android-tv.log` og
`android-beta10-baseline.log`. Opphavleg rapport frå den avbrotne køyringa er bevart separat.
Mobil og nettbrett i begge retningar er også visuelt kontrollerte ved skriftstorleik 2.0,
sjå [mobilavspeling](MOBILE_PLAYBACK_AND_OFFLINE.md).

## Produksjonsartefakt og oppdatering

Signert universal-APK er kontrollert som `app.reelstack`, kode 148, `1.0.0-beta11`, minSdk 26,
targetSdk 36 og ikkje debuggable. Han inneheld arm64-v8a, armeabi-v7a, x86 og x86_64.
Sertifikat SHA-256 er `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
FFmpeg-kjeldearkivet er det same som beta9/beta10: 30 790 595 byte,
SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

Endeleg APK-hash, kjeldecommit, installasjon over førre utgåve og den offentlege
oppdateringsflyten blir førte inn etter kjeldefrys og kontroll av dei publiserte filene.
