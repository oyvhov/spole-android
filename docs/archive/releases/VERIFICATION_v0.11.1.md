# Verifisering av Spole 0.11.1

## Bygg

- Pakke: `app.reelstack.debug`
- Versjonskode: `23`
- Versjonsnamn: `0.11.1-debug`
- APK: `Spole-v0.11.1-debug.apk`
- Storleik: `28 424 229` byte
- SHA-256: `57e895d1f72d0ef4212d96bed2974a030fa2e546de9c77374eb7e6995e3e88db`
- Signeringssertifikat (SHA-256): `a5dfdbc82a57f1b84c3778485dc448d589f9d7b6ae4566de3648b4eb777a872b`

## Automatiske kontrollar

- 149 av 149 einingstestar bestod.
- 62 av 62 Android-testar bestod på Android 16-emulator.
- Android lint fullførte med 0 feil og 21 åtvaringar.
- Den endelege, hash-kontrollerte APK-en vart brukt i Android-testen.

## Emby-innlogging

- Emby-innlogging returnerer tilgangsteikn og brukar-ID frå `/Users/AuthenticateByName`.
- Profilen blir stadfesta mot `/Users/{Id}` med den returnerte brukar-ID-en.
- Ein eigen nettverkstest stadfestar at `/Users/Me` aldri blir kalla for Emby.
- Feilmeldingane skil mellom feil brukarnamn/passord, nettverk, adresse og tenarfeil utan å vise svarinnhald eller hemmelege verdiar.

## Oppgradering og ekte data

Versjon 0.11.1 vart installert over 0.11.0 i den vanlege emulatoren. Talet på lokale appfiler var 157 både før og etter installasjonen. Eksisterande innloggingar vart bevarte, og ekte innhald frå både Jellyfin og Emby lasta på framsida etter oppstart.

Eit reelt Emby-passord vart ikkje brukt under automatisert verifisering. Brukaren må derfor prøve sjølve passordinnlogginga på nytt etter installasjon; den feilande profilstaden er dekt av både einings- og Android-test.
