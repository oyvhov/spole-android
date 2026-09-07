# Innstillingar · designoppdatering

- Matte grupper for tenester, framsideval og varsel/oppdatering erstattar harde skiljelinjer.
- Mindre seksjonsoverskrifter skil kategoriane frå handlingane.
- «Tilpass framsida» opnar/lukkar med animasjon og hugsar utvida tilstand ved rekomponering. Eigne film- og seriebrytarar er bevarte.
- Tilkopla tenester har grøn hake; feil og åtvaringar er framleis synlege. Konto- og tilgangsreglar er uendra.
- Designgjennomgangen prioriterte leserekkjefølgje, færre synlege val og tydeleg gruppering framfor ekstra dekor.

Verifisering: debug-bygg og 203 JVM-testar bestod. UiConsistencyTest bestod 3/3 på den isolerte Android-emulatoren, med systemets font_scale sett til 2.0. Brytartesten kontrollerer lukka gruppe, opning og éi endring per trykk. Manuell visuell kontroll av normal tekststorleik med demodata er utført; ekte kontoar er ikkje testa. Ingen ny produksjons-APK publisert i denne runden.
