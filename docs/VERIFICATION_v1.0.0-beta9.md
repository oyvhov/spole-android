# Verifisering av Spole 1.0.0-beta9

11. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 146.

## Innhald og kjelde

Mobilheroen og den første synlege rada deler no eitt mellomrom. Den store treffflata til
karusellprikkane er bevara, og tomme rader tek ikkje seksjonsplassen. Releasen tek også med
«Spelar no»-popupen og dei visuelle endringane dokumenterte i [LAYOUT.md](LAYOUT.md) og
[DESIGN_SYSTEM.md](DESIGN_SYSTEM.md).

Nyare hovudkjelde gjennom beta8 er fletta inn, med sesongtema, smarte hyller og rettingar for
fjernkontroll. Den felles kortbreidda og beta8 si kantplassering i rader er begge bevarte.

## Bygg, einingstestar og lint

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`,
  `--max-workers=2`, avslutta med `BUILD SUCCESSFUL`, kode 0, på den endelege runtime-kjelda.
- 975 einingstestar i 143 suitar: 974 bestått, 1 hoppa over, 0 feil.
- Lint: 0 feil, 189 åtvaringar og 1 hint.
- `check-release-consistency.py` og `git diff --check` bestod.

## Android-testar og visuell kontroll

Testane bruker debug- og test-APK frå same endelege kjelde og `SpoleTestRunner`.
Ingen instrumentering vart køyrd på dei lagra review-profilane.

| Profil og utval | Resultat |
| --- | --- |
| Isolert mobil 5562: `HomeVisualPolishTest`, `ActivityAdaptiveTest`, `WideNavigationSettingsTest`, `LibraryPresentationUiTest`, `MobileHeroGestureTest`, `SessionDetailsTest` | 37/37 |
| Isolert mobil 5562, 1080 × 2400, skriftstorleik 2.0: `MobileHeroGestureTest` | 7/7 |
| Isolert TV 5566: `HomeMediaRowsTest`, `HomeLayoutUiTest`, `TvNavigationIntegrationTest`, `FocusOutlineTest`, `SteadyRemoteRowsTest`, `TvUpdateUiTest` | 19/19 |

Til saman 63 beståtte testkøyringar; dei sju hero-testane er køyrde i to oppsett.
Første mobilrunde hadde éin fokusfeil medan Android SystemUI viste ein ANR-dialog og tok
vindaugsfokuset. Etter at dialogen var rydda bort, bestod alle 37 utan endring i app eller testar.
Det er det avgrensa UI-utvalet over som er køyrt, ikkje heile Android-testsamlinga.

Den lagra mobilprofilen 5560 vart oppdatert med signert `install -r` over beta6.
Han hadde allereie eksplisitt demomodus på engelsk; denne gjennomgangen er difor visuell
mobilkontroll, ikkje stadfesting av tenarinnlogging på mobil. Normal skrift og 2.0 vart
kontrollerte med lasta kunst. Overgangen under prikkane er omtrent halvert, og kunstfeltet
held same minimumshøgd som beta8. Treffflatene er framleis 48 dp. Ved 2.0 er knappane og
metadata lesbare; lange radoverskrifter blir avkorta etter den eksisterande regelen.
Skriftstorleiken vart sett tilbake til 1.0 etter kontrollen.

## Signert produksjonsartefakt

- Kjelde/tag: `6915bdfd07f6db86e03f33d72b31374053d86289`.
  `SOURCE_COMMIT.txt` og APK-en sin `version-control-info.textproto` peikar på same commit.
- `assembleRelease` etter kjeldefrys avslutta med `BUILD SUCCESSFUL`, kode 0.
  Alle APK-oppføringar utanom commitmetadata og signeringsmetadata er byteidentiske med
  det testa bygget; R8-mappinga er også identisk. Ingen runtime-endringar vart gjorde etter testane.
- Universal-APK: 11 771 926 byte. `app.reelstack`, kode 146, `1.0.0-beta9`, minSdk 26,
  targetSdk 36, ikkje debuggable, med arm64-v8a, armeabi-v7a, x86 og x86_64.
- APK SHA-256: `41472e9689602e9655f6c2ad3b87c204fe20559cb89721a364a1b2f07e61e936`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping: 95 384 656 byte, SHA-256
  `7665f62740496be170ef2afc7d42ccefeb56430f4095f1556a0447eb2b6c8bb5`.
- Dei fire `libffmpegJNI.so`-filene og begge FFmpeg-lisensane er byteidentiske med offentleg
  beta8. Kjelde- og relenkingsarkivet frå same native-bygg er 30 790 595 byte, SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Den endelege arkiv-APK-en er installert med `install -r` på mobilprofilen 5560. Versjon 146
  er stadfesta; første installasjonstid er framleis 15. september 2026, og demovalet, språket
  og den lagra framsida er bevarte.

## Offentleg release og oppdatering gjennom appen

- [Beta9](https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta9) er publisert som
  prerelease, ikkje draft, med éin universal-APK og fem assets: APK, mapping, `SHA256SUMS.txt`,
  `SOURCE_COMMIT.txt` og FFmpeg-kjeldearkivet. Stabil latest er ikkje endra.
- Alle fem asset-storleikar og GitHub-digestar vart kontrollerte i kladden før publisering.
  Den uautentiserte offentlege release-lista viser same APK-digest. Den offentleg nedlasta
  APK-en er 11 771 926 byte og har same SHA-256 som den signerte arkivfila.
- Den lagra TV-profilen 5564 vart halden på offentleg beta8 / kode 145 til publisering.
  Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no fann beta9 og release-notata.
  «Last ned oppdatering» gjekk vidare til «Installer oppdatering» etter appen sine kontrollar.
  Android viste «Do you want to update this app?» og deretter «App installed.» etter «Update».
- «Open» opna beta9 med ekte Jellyfin- og Emby-rader, lasta kunst og den lagra profilen.
  Installert versjon er 146 / `1.0.0-beta9`; første installasjonstid er framleis
  9. september 2026 kl. 20:50:58. Nynorsk, «Heile året», bakgrunn «Midnatt · djup blå» og lime
  er bevarte. Tenestepanelet viser framleis Jellyfin og Emby som tilkopla.
  «Sjekk automatisk» og «Testutgåver» er framleis på. Ingen appkrasj finst i TV-eininga sin
  krasjbuffer etter oppdateringa.
- Ingen aktiv avspeling vart vist ved denne sluttkontrollen. Start/stopp av ekte tenarøkter
  vart difor ikkje prøvd på nytt; popup- og oppdateringslogikken er dekt av testane over.
- GitHub [«Bygg og test»](https://github.com/oyvhov/spole-android/actions/runs/38098418326)
  for releasecommit fullførte med `success`. Vanleg branch-bygg køyrde versjons- og språkkontroll,
  einingstestar, lint og debug-bygg. Ingen ekstra manuell bygg-, instrumenterings- eller Jev-køyring
  er starta. UI-testane i tabellen over vart køyrde lokalt på dei isolerte profilane.

Detaljerte lokale loggar og skjermbilete ligg i den ignorerte byggmappa; kontoopplysningar blir
ikkje publiserte.
