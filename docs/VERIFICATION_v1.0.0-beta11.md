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

- Kjelde/tag: `1de108a5f743007402ebdaa4f92015f5315e8974`, `v1.0.0-beta11`.
  `SOURCE_COMMIT.txt` og APK-en sin `version-control-info.textproto` peikar på same commit.
- Bygg etter kjeldefrys: `BUILD SUCCESSFUL`, kode 0, etter 1 minutt 24 sekund.
  Alle APK-oppføringar utanom commit-/signeringsmetadata er byteidentiske med det testa
  produksjonsbygget. R8-mappinga er også identisk; ingen runtime-endring etter testane.
- Universal-APK: 11 786 702 byte, SHA-256
  `980b80e497e7e105a29bca0d63ed9bcf18317974391169c7a0e2b03b0cda1efb`.
- R8-mapping: 95 952 193 byte, SHA-256
  `9df9bd042f20c257a1ecbfa19e2d1231130b33da6ba19cfed28bce8796982d3b`.
- Dei fire JNI-biblioteka og begge FFmpeg-lisensane er byteidentiske med offentleg beta9.

Den lagra mobilprofilen 5560 vart oppdatert med signert `install -r` frå beta9/kode 146
til beta11/kode 148. Første installasjonstid er framleis 15. september 2026 kl. 10:09:51.
Den lagra framsida, lasta kunst, engelsk språk og stor skrift er bevarte. Appoppdateringar
viser `Spole 1.0.0-beta11`. Dette er installasjons- og UI-kontroll; tenarinnlogging på mobil
vart ikkje prøvd på nytt.

## Offentleg release og oppdatering gjennom appen

- [Beta11](https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta11) er publisert som
  prerelease, ikkje draft, med éin universal-APK og fem assets: APK, mapping, `SHA256SUMS.txt`,
  `SOURCE_COMMIT.txt` og FFmpeg-kjeldearkivet. Stabil latest er ikkje endra.
- Alle fem asset-storleikar, `uploaded`-status og GitHub-digestar samsvarte med arkivfilene
  før publisering. Den uautentiserte offentlege release-lista viser beta11 med same APK-digest.
- Den offentlege APK-en vart lasta ned utan autentisering. Han er 11 786 702 byte og har
  same SHA-256 som den signerte arkivfila.
- Den lagra TV-profilen 5564 vart halden på beta10/kode 147 til publisering.
  Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no fann beta11 og release-notata.
  «Last ned oppdatering» gjekk vidare til «Installer oppdatering» etter appen sine kontrollar.
  Android viste «Do you want to update this app?»; «Update» fullførte installasjonen.
- Etter opning viser TV-profilen beta11/kode 148, sist oppdatert 14. oktober 2026 kl. 08:48:28.
  Første installasjonstid er framleis 9. september 2026 kl. 20:50:58. Nynorsk og dei lagra
  innhaldsradene er bevarte. Tenestepanelet viser både Jellyfin og Emby som «Tilkopla».
- Dette er ei faktisk oppdatering gjennom appen på TV, ikkje ein `adb install -r`-test.
  Kontoar eller brukar-/AVD-data vart ikkje nullstilte.
- GitHub [«Bygg og test»](https://github.com/oyvhov/spole-android/actions/runs/38523921412)
  for releasecommit fullførte med `success`. Ingen ekstra manuell CI- eller Jev-køyring vart starta.

Synk mellom to fysiske einingar og fysisk nettbrett vart ikkje prøvd. Framdriftsrettingane
er kontrollerte med syntetiske Jellyfin-/Emby-fixturar og loopback-video på dei isolerte profilane.
Lokale loggar, artefaktkontroll og skjermbilete ligg i `app/build/release-v1.0.0-beta11/`.
Dei inneheld ikkje publiserte kontoopplysningar; berre dei fem fastsette releasefilene er lasta opp.
