# Verifisering · sesongar og lokale varsel

Arbeidsendring etter Spole 0.14.1, 8. september 2026. Ikkje ein publisert release.

## Kva som er endra

- Sesongane er ikkje lenger førehandsvalde. Både nye og eksisterande seriar krev eit eksplisitt val.
- Seerr sine sesongdatoar og neste episode blir bevarte. Ukjend eller framtidig premiere blir ikkje
  framstilt som at innhaldet er forseinka i biblioteket. Godkjent førespurnad får rett status.
- Eigne varsel for delvis tilgjengelege / alt førespurde sesongar, med lokal lagring og personleg
  kontoavgrensing. Dei krev ikkje Sonarr-tilkopling eller førespurnadsløyve.
- Lokale varsel sender ikkje POST/DELETE, får ikkje eit oppdikta request-ID og kan fjernast lokalt.
- Ein eksakt eigen førespurnad og eit lokalt varsel blir slått saman utan å miste varslingshistorikk.
  Standard og 4K blir ikkje blanda. Varsel frå bjølla held seg til bibliotektilgjenge også etter samanslåing.
- Fast popup-ramme, skeleton til opninga er ferdig, kort innhaldsfade og separate, store treffflater.

## Testdekning

Einingstestar for sesongdato, ukjend dato, «Returning Series» utan overvåkingsslutning, aktivt sesongval,
godkjenning/førespurnad, 4K, neste episode og faktisk nedlasting/import/tilgjenge.

Android-testar på den isolerte emulator-5562 dekkjer:

- vanleg Seerr-brukar utan administrator-, førespurnads- eller Sonarr-tilgang;
- berre GET mot Seerr, ingen ny førespurnad ved lagring/fjerning av varsel;
- dobbelttrykk, persistens, kontobytte under lesing, utlogging og lokal avmelding utan nettverk;
- nøyaktig eigarskap, samanslåing, 4K og at eit enkelt varsel ikkje endrar ein fleirsesongsførespurnad;
- delvis sesong, eksplisitt val av framtidig sesong, standardvarsling ved ny førespurnad;
- vanleg og 2,0× tekst, fast lukking og at raske svar ventar på popup-opninga.

## Ekte data og grenser

Det signerte produksjonsbygget er kontrollert med `-r` på den permanente emulator-5560.
Kontoar og lokale val er bevarte. Silo viste tre bibliotekssesongar og ein alt førespurd sesong;
Lioness viste to bibliotekssesongar og ein delvis tilgjengeleg sesong med neste episode/dato.
Begge hadde låste sesongar og bjølle i staden for ein ny førespurnad. Ingen ekte
førespurnad, tilbaketrekking, avspelingskommando eller endring av tenarovervaking vart brukt i testen.
Skjermbilete ligg berre lokalt i ignorerte `app/build/`.

Dette er ikkje ein verifikasjon av ei ende-til-ende nedlasting på brukarens tenarar, bakgrunnsvarsel
på ein fysisk telefon eller at den ikkje namngjevne serien ville ha kome inn automatisk. Løysinga
les ikkje Sonarr-overvaking og gir ingen slike lovnader. Seerr sin AVAILABLE-status er kjelda for
varselet; det er ikkje eit abonnement på kvar ny episode eller garanti om ein framtidig heil sesong.

## Resultat

- 238/238 einingstestar bestod, inkludert 13 nye sesongtestar.
- 100/100 Android-testar bestod på emulator-5562 (227,272 sekund). Det er 13 nye testar.
- Etter siste ekstra kontobyttevakt ved fjerning: 34/34 målretta Android-testar bestod på nytt
  (68,42 sekund), inkludert innlogging, lagring, sesongflyt og popup-rørsle. Einingstestane vart òg køyrde på nytt.
- Lint: 0 feil, 23 åtvaringar. Ingen ny åtvaring i dei nye sesong-/varslingskomponentane.
- Signert release-, debug- og Android-testbygg er laga. Dette er ein lokal review-kandidat,
  framleis med versjonsnummer 0.14.1 / 40; han er ikkje APK-en som vart publisert som 0.14.1.
- Ingen GitHub-release eller tag er oppretta i denne runden. Publisering krev nytt versjonsnummer.

Dialogrørsla er testa automatisk ramme for ramme, med tidlege og seine data og fleire opningar.
Den manuelle kontrollen av ekte data er visuell kontroll, ikkje ei måling av yting på ein fysisk telefon.

## Lokal APK

- `app/build/series-flow-review/Spole-series-flow-review.apk`, 2 268 108 byte.
- SHA-256: `9271773d24f444d63b38a1ee761221d233fd0c537849163afc3b880984473b57`.
- Pakke `app.reelstack`, versjon 0.14.1 / 40 (lokalt review-bygg, ikkje ein ny GitHub-release).
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- `mapping.txt` er kopiert til same ignorerte mappe. Oppdatering av den innlogga emulatoren med `-r` bestod.
