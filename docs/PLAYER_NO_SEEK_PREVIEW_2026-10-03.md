# Spoling utan bildepreview

Preview-boksen er fjerna frå mobilspelaren og den skjulte TV-OSD-en. Mobilspelaren legg ikkje lenger eit ekstra kort inn over tidslinja ved drag eller spoletrykk; play/pause og ±10-knappane held dermed same plass. TV viser framleis den kompakte tidsvisinga ved spoling. Kapittelmenyen er bevart.

## Verifisering

- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest` og `lintDebug`: godkjende. 837 einingstestar; ingen lint-feil.
- `PlayerSeekLayoutTest`: 3/3 bestått med systemskrift 1.0 og 3/3 med 2.0 på den isolerte `Spole_Instrumentation`, emulator-5562.
- Testane samanliknar plasseringa til alle tre transportknappane før og under drag, etter spoletrykk og når spoletilbakemeldinga går ut. Dei kontrollerer òg at kapittel med bilde berre gir tidsvising på TV.
- Syntetisk spelarstate; ekte videostrauming og fysisk fjernkontroll er ikkje testa i denne kontrollen. Dei lagra profilane på 5560 og 5564 er ikkje endra.
- `git diff --check`: godkjend. Endringa er ikkje publisert som ny release.
