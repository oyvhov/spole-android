# Verifisering – innlogging og aktive nedlastingar 0.12.4

## Endring

- Ei innlogga teneste opnar som ei kompakt stadfesting med grøn hake.
- Adresse, kontofelt og utlogging blir viste først når brukaren utvidar delen.
- Utvidinga skjer inne i den faste popup-ramma, slik at arket ikkje kalibrerer ytre høgd etter innhaldet.
- Sonarr-køen bruker den unike køpost-ID-en før den delte nedlastings-ID-en. Fleire episodar frå same
  sesongpakke kan dermed visast samtidig utan doble Compose-nøklar.
- Kø og aktivitet blir i tillegg avdupliserte ved innlesing, og nedlastingslista har eit defensivt
  vern mot feilaktige identiske ID-ar frå ei tredjepartsteneste.

## Automatiske kontrollar

- **169/169 JVM-testar** bestod.
- **82/82 Android-testar** bestod på isolert emulator 5562 (`OK (82 tests)`, 271,226 sekund).
- Den nye køtesten sender to Sonarr-episodar med same nedlastings-ID gjennom både parser og UI.
- Innloggingstesten stadfestar kompakt start, grøn innloggingsstatus og mjuk utviding/lukking.
- Kalendertesten vart køyrd separat etter ei maskinavhengig tidsgrense og bestod **5/5**, før den
  komplette køyringa bestod utan feil.
- Lint: **0 feil, 20 åtvaringar**.
- Debug-, Android-test- og produksjonsbygg bestod.

## Emulator med ekte kontoar

Den permanente `Tunet_Test`-AVD-en vart starta utan sletting eller avinstallering. Gamle låsefiler vart
flytta til tryggleikskopiar, men emulatorprosessoren stoppa under oppstart og eininga vart verande
fråkopla. Ekte kontoar og `userdata-qemu.img` vart ikkje endra. Det vart derfor ikkje påstått nokon
manuell kontroll med ekte kødata i denne utgåva. Krasjtilfellet er i staden dekt ende-til-ende av den
isolerte emulatoren med to samtidige køpostar som deler tredjeparts-ID.

## Produksjonsfil

- Versjon 0.12.4, versionCode 34, pakke `app.reelstack`, minSdk 26, targetSdk 36.
- Storleik: 7 101 868 byte.
- SHA-256: `bb8537911f356681c64f64c93bc68c9bbcaa5e9722848ca2f4831623476c0a6b`.
- Signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Same produksjonsnøkkel som 0.12.3; APK-en kan installerast som oppgradering.
