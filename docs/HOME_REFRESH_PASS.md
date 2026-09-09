# Heim: oppdatering med fjernkontroll og språk

9. september 2026, kjeldearbeid etter alpha06.

## Endringa

Heim hadde dra-ned-for-å-oppdatere, men mangla ei tilsvarande handling for fjernkontroll. Nedst ved oppdateringsstatusen ligg no «Oppdater biblioteket». Ho brukar same oppdateringsflyt som dra-rørsla, utan nye tenestekall eller endra tilgangsreglar.

Knappen brukar felles fokusmarkering frå designsystemet og minst 48 dp treffhøgd. Under oppdatering blir fokuset ståande på knappen, teksten viser lastestatus og nye aktiveringar blir ignorerte. Etterpå kan brukaren prøve på nytt utan å leite etter fokus. Teksten kan vekse ved stor skrift; det er inga fast teksthøgd.

Seks nye ressursnøklar per språk dekkjer handlinga, kontoomtale, bibliotekfeil og framhaldshandlinga. Metadata frå tenestene blir ikkje omsette.

## Avgrensing

## Verifisert

- [x] 283 einingstestar, ingen feil.
- [x] Åtte Android-testar på isolert emulator-5562: fjernkontroll, stor skrift og språkressursar.
- [x] Release- og testbygg fullførte. Lint har null feil og 33 åtvaringar; åtvaringane er ikkje ferdig rydda.
- [x] Eksisterande signeringssertifikat kontrollert; signert oppdatering installert med `-r` på TV-emulator-5564.

Første testforsøk fekk feil fordi WSL-tilkoplinga fall ut under installasjonen av appen. Etter stadfesta installasjon vart testane køyrde på nytt. Fjernkontrolltesten set eksplisitt tastaturmodus, slik at telefonemulatoren ikkje startar testen i berøringsmodus.

## Attståande

Dette er ikkje full TV-godkjenning eller full engelsk omsetjing. Fysisk fjernkontrolltest står framleis att. Ingen ny publisering er bestilt i denne bolken.
