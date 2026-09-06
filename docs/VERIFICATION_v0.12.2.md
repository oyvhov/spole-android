# Verifisering – popup-opning 0.12.2

## Feil stadfesta før endringa

Den nye testen `repeatedOpeningNeverOvershootsOrReversesDirection` vart først køyrd mot
0.12.1-koden på isolert emulator 5562. Han feila: topp-posisjonen gjekk til y=366 og så
tilbake til sluttposisjonen y=369. Tidlegare testar venta på ferdig animasjon og såg ikkje dette.

Den faktiske Material3 1.4.0-kjelda frå Google Maven brukar ein spatial spring med damping 0.9.
Opning kunne difor gå forbi sluttposisjonen sjølv med fast høgd og deaktiverte dragegestar.
`onTextLayout` skreiv i tillegg state som sette inn omtale-knappen ved neste komposisjon.

## Endring

- `StableSheetDialog`: innfødd Compose Dialog med fast 82 % overflate, 320 ms ikkje-overskytande
  opning og 200 ms lukking. Berre draw-translation, ingen dynamiske draanker eller høgdeanimasjon.
- Plattformen sin ekstra dimming/vindaugsanimasjon er av; Compose eig scrim og rørsle.
- System-/tastaturmargar blir handterte i dialogen, medan toolbar er utanfor rulleinnhaldet.
- Skeleton er synleg under opning/nettverk. Detaljar blir viste samla med ein kort opacity-overgang.
- Omtaleteksten blir målt før plassering, slik at utvidingsknappen ikkje kjem ei ramme seinare.
- Ingen nettverksprotokollar, bibliotekreglar, innloggingar eller tilgangsgrenser er endra.

## Testplan og resultat

Endeleg bygg: **168/168 JVM-testar**, **79/79 Android-testar**, `OK (79 tests)` på isolert
emulator 5562, 187,03 sekund. Lint: **0 feil, 20 åtvaringar**. Release-bygg bestod.
Nye regresjonstestar dekkjer tre etterfølgjande opningar ramme for ramme; metadata før, under og
etter opning; feil/fallback og lukking under lasting. Eksisterande testar dekkjer heldt drag, fling,
kalender-retur, alle toolbar-posisjonar, stor skrift og Android Tilbake under sending.

Skjermopptak og bilete med ekte kontoar er lokale og skal ikkje inn i Git eller utgåva.
Ingen ekte førespurnader eller avspelingskommandoar blir sende. Dette er ikkje ein fysisk fps-benchmark.

## Ekte kontoar

Produksjons-APK-en vart installert med `-r` på `Spole_Review`, port 5560. Seerr-, Jellyfin-,
Emby- og Radarr-tilkoplingane var bevarte. Den første Jellyfin-filmen på Heim vart opna etter
appstart: opptaket viser skeleton under opning, deretter samla detaljar. Omtalen vart utvida og
innhaldet rulla; toolbar og X blei ståande. Emby-innloggingsarket vart opna med same faste topplinje.
Ingen innlogging vart sendt, ingen konto bytt og ingen brukardata sletta.
Feltfokus vart kontrollert utan å sende innlogging. Eit fullt dokka skjermtastatur og fysisk
telefon-biletrate er ikkje manuelt verifiserte i denne runden.

## Produksjonsfil

- Versjon 0.12.2, versionCode 32, pakke `app.reelstack`, minSdk 26.
- Storleik: 7 069 100 byte.
- SHA-256: `d847a32b07ffd5a850130c1ca5b3ddfc8576d6ca5a41d6892810bb974b40ab82`.
- Signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Same produksjonsnøkkel som 0.12.1; oppgradering vart stadfesta utan avinstallering.
