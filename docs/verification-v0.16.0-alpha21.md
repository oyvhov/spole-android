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

## Publisering og oppdatering gjennom TV-appen

- Publisert som prerelease, ikkje stabil latest, på
  https://github.com/oyvhov/spole-android/releases/tag/v0.16.0-alpha21.
- Release-taggen peikar på kjeldecommit `95f2ca47beadaf9f2d422ffec3bfddbfc370c9e2`.
- Den offentlege release-lista utan autentisering viser utgåva med `draft=false`,
  éin universal-APK og korrekt digest. Ei separat offentleg nedlasting har same
  SHA-256 som den arkiverte APK-en over.
- TV-profilen 5564 fann alpha21 gjennom Sjekk no, lasta ned og kontrollerte APK-en.
  Android sin Update-dialog vart godkjend; pakkekontrollen etterpå viste kode 64
  og `0.16.0-alpha21`. Ein tidlegare installer-skjerm viste «App installed» medan
  kode 63 framleis var installert; testen vart difor gjenteken og først rekna som
  bestått etter faktisk kontroll av installert versjon.
- Appen vart opna att med same konto og Skog/Korall-tema. Ingen brukardata vart
  sletta. Mellombels løyve `REQUEST_INSTALL_PACKAGES` vart sett tilbake til `default`.
- Skjermbilete frå kontrollen ligg lokalt under `app/build/alpha21-*` og blir ikkje
  publiserte fordi dei inneheld ekte profil-/bibliotekdata.
