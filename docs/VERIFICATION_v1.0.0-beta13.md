# Verifisering av Spole 1.0.0-beta13

15. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 150.

## Omfang

Nye smarte hyller blir knytte til vald konto og eventuelt bibliotek. Eldre hyller og
sesongmalar bevarer uttrykkeleg globalt omfang. Bibliotekutval blir sendt som `ParentId`
på alle regelspørjingar; ei tom liste sender ingen spørjingar. Eigarkontoen bruker ein
opak nøkkel frå tenestetype, stabil tenaridentitet og brukar-ID. Rute-/tokenbyte endrar
ikkje nøkkelen. Ingen nye tenarobjekt eller kontoar blir oppretta.

## Bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`,
  `--max-workers=1`: `BUILD SUCCESSFUL`, kode 0, etter 8 minutt 5 sekund.
- 1028 einingstestar i 147 suitar: 1027 bestått, éin valfri nettverkstest hoppa over, null feil.
  24 nye testar dekkjer lagring, eigarkonto, failover, bibliotek-ID-ar, OR/AND-spørjingar,
  avgrensa cache, seine svar og bevaring av sesongradene si plassering.
- Lint: null feil, 196 åtvaringar og eitt hint. Språkkontrollen passerer med 1665 standardnøklar
  og éin tillaten standardnøkkel. Release-metadata samsvarer.
- Isolert TV 5566, `SmartShelvesTest`, systemskrift 1.0: 30/30 bestått, avslutta køyring på 69 sekund.
  Dei tre nye prøvene dekkjer tenar-/bibliotekval, bevaring ved gjenteke tenartrykk, sperre mot
  lagring av tomt utval og faktiske trykkflater ved dobbel skrift i nettbrettformat.
- Første avslutta køyring av same 30 på TV med systemskrift 2.0 gav 25/30, inkludert alle dei
  tre nye. Fem eksisterande prøvefunksjonar feila: `theRemoteWalksPastTheNameField` og
  `theNameFieldFollowsATemplateAndTheOwner` fekk Compose-idling-timeout;
  `theFirstCoversAreInViewOnArrival`, `theRemoteGoesStraightDownAColumnAndBackToTheTop` og
  `shelfCoversAreSmallerThanTheLibraryGrid` braut forventningar om synleg første rad, kolonne
  og kortbreidd. Desse er ikkje kalla beståtte eller beviste som gamle feil i beta12.
  Ny font-1-køyring passerer alle 30, men er ikkje ein erstatning for font-2-resultatet.
- Eit første bygg avslutta med prosesskode 1 etter kompilering, utan rapportert kompilerings-
  eller testfeil. Loggen er bevart. Bygget ovanfor er den etterfølgjande avslutta køyringa;
  mellom køyringane vart standardvalet presisert etter brukarsvaret og sesongposisjonen sikra.

Ingen brei Android-pakke med fleire hundre uvedkomande testar vart starta på nytt for beta13.
Fullførte beta12-resultat står i [eiga verifisering](VERIFICATION_v1.0.0-beta12.md), og blir ikkje
rekna som nye beta13-testar. Lokale bygg-, test- og biletfiler ligg i `app/build/release-v1.0.0-beta13/`.

## Ekte profil og visuell kontroll

Den signerte produksjons-APK-en er installert med `-r` over beta12 på den lagra mobilprofilen
5560. Kode 150 og `1.0.0-beta13` er stadfesta; første installasjonstid er framleis
15. september 2026 kl. 10:09:51. Ekte framside, kunst og innlogging kom tilbake.

Innstillingar → Pages and menu → Edit library page → New smart shelf viser den tilkopla
Jellyfin-kontoen og dei faktiske biblioteka. Frå rotnivået er ingen bibliotek førehandsvalde;
«Choose at least one library» står framme. «Filmar» vart valt, og same avgrensing heldt seg
ved systemskrift 2.0. Heile ord, valmarkering, Cancel og Save er visuelt kontrollerte i ekte PNG.
Utkastet vart avbrote utan å lagre ei testhylle. Skrift 1.0 og Heim er sette tilbake.

Testpakka brukte berre isolert 5566. Ingen instrumentering, utlogging eller nullstilling på
dei ekte profilane 5560/5564. Offentleg artefakt og faktisk oppdatering blir førte etter publisering.
