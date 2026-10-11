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

TV-profilen 5564 med ekte kontoar blir halden på offentleg beta8 fram til den nye releasen er
publisert, for å prøve den faktiske nedlastings- og installasjonsflyten gjennom appen.

## Signert produksjonsartefakt og offentleg oppdatering

APK, mapping og kjeldecommit blir arkiverte etter at kjelda er frose. Signatur, metadata,
GitHub-digest, offentleg nedlasting og oppdatering frå beta8 blir dokumenterte her etter
publisering. FFmpeg-koden er uendra; kjelde- og relenkingsarkivet frå det same native-bygget
blir lagt ved releasen.

Detaljerte lokale loggar og skjermbilete ligg i den ignorerte byggmappa; kontoopplysningar blir
ikkje publiserte.
