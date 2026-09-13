# Verifisering: 0.16.0-alpha21

Versjonskode 64, pakke `app.reelstack`. Same eksisterande signeringssertifikat.

Funksjonsgrunnlaget vart kontrollert før versjonsauken: 450 einingstestar og 61 ulike
Android-testar (37 TV, 24 mobil) bestod. Siste mobiljusteringar vart kontrollerte manuelt
med skriftstorleik 2.0 i signert APK. Sjå [gjennomgangen](TV_FEEDBACK_PASS2.md).

## Release-bygg

- Fullt bygg avslutta med `BUILD SUCCESSFUL` etter 7 minutt og 8 sekund.
- 450/450 einingstestar bestod etter versjonsauken. Android-testane over vart køyrde
  under funksjonsgjennomgangen før versjonsauken, og er ikkje talde som nye løp av kode 64.
- Lint: 0 feil, 30 åtvaringar.
- Universal produksjons-APK, ikkje debuggbar: `Spole-v0.16.0-alpha21.apk`, 4 000 322 byte.
- API 26 eller nyare; ARMv7, ARM64, x86 og x86_64.
- SHA-256: `95f00a3647e22f7c4c1dc05f16532a76aa6cef466c177e725a5ecf276e40309f`.
- Verifisert sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK, tilhøyrande R8-mapping og sjekksum er arkiverte i `app/build/release-v0.16.0-alpha21`.

## Oppdatering før publisering

Den signerte APK-en vart installert med `install -r` over versjonskode 63 på den innlogga
mobilprofilen 5560. Android stadfesta kode 64 og alpha21. Appen viste same kontoavatar,
ekte innhald og Skog/Lime-tema. Ingen kontoar eller brukardata vart nullstilte.
TV-profilen 5564 vart ståande på kode 63 for å kontrollere nedlasting og installasjon
gjennom appen etter publisering.

Offentleg digest, nedlasting og TV-oppdatering blir dokumenterte etter publisering.
