# Stabil spoling og rolegare mobilkontrollar

Preview-fiksen i beta17 fjerna bildepreviewen, men spelaren la framleis inn «Gjer klar avspelinga …» som ei ekstra tekstlinje ved `busy`. Media3 melder denne tilstanden ved kort buffering etter spoling. Dei to fleksible mellomromma fordelte seg då på nytt, og transportknappane flytta seg opp.

## Endring

- Lastestatus endrar berre innhaldet i den faste play-/pauseknappen. Spinneren tek same plass; det kjem inga ekstra tekstlinje inn i oppsettet. Skjermlesaren får framleis lastestatusen.
- Play/pause har ei 56 dp ikonflate med mørk, gjennomskinleg bakgrunn og kvitt symbol, framfor ein stor temafarga knapp på 72 dp.
- Spoleknappane bruker enkle retningspiler. Kvart trykk spolar framleis 10 sekund, og skjermlesaretikettane seier dette.
- Tidslinja er nøytral, og tidene bruker vanleg skriftvekt og jamne talbreidder. Lange avspelingar bruker timar konsekvent. Ei einskild tekstlinje med fleksibel breidd hindrar at tidsfeltet pressar verktøya ut av rada.

## Verifisering

- `assembleDebug assembleDebugAndroidTest lintDebug`: BUILD SUCCESSFUL; null lint-feil.
- Isolert `Spole_Instrumentation`, emulator-5562: **17/17** Android-testar ved systemskrift 1.0 (`PlayerSeekLayoutTest` og `JellyfinPlayerUiTest`).
- **6/6** i `PlayerSeekLayoutTest` ved faktisk systemskrift 2.0.
- Nye testar utløyser buffering frå eit spoletrykk og samanliknar alle tre knappane før, under og etter, i både 412 × 840 dp og 740 × 360 dp mobilvising. Både framover, tilbake og utgått spoletilbakemelding er dekte.
- Bildekontroll av pauseknappen stadfestar nøytrale fargar og 56 dp storleik. Lokale skjermbilde før/under buffering og med stor skrift er også gjennomgåtte.
- Eksisterande kontrollar for undertekstar, fyll skjermen, lastestatus, diagnostikk, tilbakeknapp og store skriftstorleikar bestod.
- Syntetisk spelarstate. Ekte videostrauming, fysisk telefon og fjernkontroll er ikkje testa i denne runden. Ingen instrumentering eller sletting av lagra kontoar på 5560/5564.
- Einingstestsuiten er ikkje køyrd på nytt; dette er ei endring i Compose-oppsettet.
- `git diff --check`: godkjend. Endringa er ikkje publisert som ny release.
