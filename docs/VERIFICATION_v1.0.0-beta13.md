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
dei ekte profilane 5560/5564.

## Publisert produksjonsartefakt

- Kjelde og tag: `0cb77821963b0fe06a804fb37ffe3abfd59cf96b`, `v1.0.0-beta13`.
  `SOURCE_COMMIT.txt` og APK-en sin innebygde VCS-commit samsvarer.
- Etter commit: `assembleRelease`, `BUILD SUCCESSFUL`, kode 0, etter sju sekund. Alle
  runtime-oppføringar utanom commit-/signeringsmetadata er byteidentiske med det testa bygget.
  R8-mappinga er også identisk.
- Produksjonspakke `app.reelstack`, kode 150, minSdk 26, targetSdk 36, fire ABI-ar og ikkje
  debuggable. Fast sertifikat SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Universal-APK: 11 824 262 byte, SHA-256
  `7430346deeea0c8f8051584080bdd9a74c78487885b42842ae4bf4760f5d5e72`.
- R8-mapping: 97 126 556 byte, SHA-256
  `b9054a8d80471861674077cfa475cce6d3959ea0c751cf0e5543df8efe317362`.
- Fire FFmpeg-JNI-bibliotek og begge lisensane er byteidentiske med offentleg beta9.
  Kjelde-/relenkingsarkiv: 30 790 595 byte, SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- [Beta13](https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta13) er publisert
  som prerelease, ikkje draft, med éin universal-APK og fem assets. Tag, filstorleikar,
  uploaded-status og alle fem GitHub-digestar vart kontrollerte før publisering.
  Stabil latest er ikkje endra.
- Uautentisert release-liste og offentleg APK-nedlasting er verifiserte; den nedlasta APK-en
  har same byte-storleik og SHA-256 som arkivfila. Ein frisk query-parameter vart brukt
  etter at det første kallet rett etter publisering fekk den førre release-lista frå mellomlager.

## Faktisk oppdatering gjennom appen

Den lagra TV-profilen 5564 vart halden på offentleg beta12/kode 149 til publisering.
Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no fann beta13 og notata.
«Last ned oppdatering» vart «Installer oppdatering» etter appen sine kontrollar. Android
«Update» fullførte installasjonen, og «Open» opna ekte framside att.

Etter oppdateringa viser pakkekontrollen kode 150 og `1.0.0-beta13`, sist oppdatert
15. oktober 2026 kl. 22:52:07. Første installasjonstid er framleis 9. september kl. 20:50:58.
Tenestepanelet viser både Jellyfin og Emby som «Tilkopla». Nynorsk og systemskrift 1.0
er bevarte. Dette er ei faktisk offentleg nedlasting og Android-oppdatering, ikkje ein
`adb install -r`-test.

Oppstartsskriptet fekk tidsavbrot på Windows-adb, men den same lagra TV-profilen var alt
ferdig oppstarta. WSL-adb stadfesta dette og vekte den eksisterande eininga. Ingen ny AVD
vart laga, og ingen konto-/brukardata vart sletta eller nullstilte.

GitHub [Bygg og test](https://github.com/oyvhov/spole-android/actions/runs/38804986372)
for releasecommit vart starta automatisk og var framleis `in_progress` ved sluttkontrollen.
Dei lokale bygg-, einings- og målretta UI-resultata ovanfor er avslutta; CI blir ikkje kalla bestått.
