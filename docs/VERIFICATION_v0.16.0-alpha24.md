# Verifikasjon · 0.16.0-alpha24

Pakke `app.reelstack`, versjonskode 67. Eksisterande produksjonssignatur skal bevarast.

## Omfang

Enkel Jellyfin-/Seerr-onboarding, same konto stadfesta på begge tenester,
oppsettslenkjer med berre adresser, valfri Seerr og passordalternativ.
QR og korte oppsettkodar er ikkje implementerte.

## Kontroll

Bygg: `app/build/alpha24-build.log`, BUILD SUCCESSFUL (2 min 44 s).

- Einingstestar: 465/465, ingen feil.
- Lint: 0 feil, 31 eksisterande åtvaringar.
- Signert universal release-APK, ikkje debuggable, minimum API 26/Android 8.
- Storleik: 4 017 874 byte.
- SHA-256: `25c7c3599b0dd0cd9812521e4a454048c8173abf40830a57d80c6851a0fe19d1`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK og tilhøyrande R8-mapping er arkiverte i `app/build/release-v0.16.0-alpha24/`.

- Android-testar: 10/10 på isolert TV (22,535 s), 18/18 på isolert mobil (43,814 s), ingen hoppa over.
- Loggar: `app/build/alpha24-tv-tests.log` og `app/build/alpha24-phone-tests.log`.
- Oppsettslenkjer er kontrollerte ved kald og varm oppstart på begge testprofilane.
- Stor tekst 2.0, TV-fokus, valfri Seerr, passordinnlogging og vern mot kontooverskriving er dekte.
- Heile Android-testpakken og fysisk TV er ikkje køyrde i denne runden.

- TV-review 5564: signert `install -r` frå alpha23/kode 66 til alpha24/kode 67 lykkast.
  Heim med ekte bibliotek, profilbilete, framdrift og lagra tema er bevarte.
- Mobil-review 5560 er halden på alpha23 for test av GitHub-oppdatering etter publisering.

- Publisert prerelease: https://github.com/oyvhov/spole-android/releases/tag/v0.16.0-alpha24
- Kjeldecommit: `16a316a4132b4aac509ad595956b31692cad83cf`.
- Offentleg release-liste utan innlogging viser alpha24 med éin APK og riktig digest.
- Offentleg APK lasta ned utan Authorization-header; hash samsvarer med lokalt bygg.
- Mobil 5560: «Sjekk no» fann alpha24 frå alpha23, lasta ned og kontrollerte APK-en.
  Android si oppdateringsgodkjenning vart fullført; installert pakke er kode 67/alpha24.
  Etter opning kom ekte bibliotek, favorittar, framdrift, profilbilete og lagra tema tilbake.
- Mellombels løyve til installasjon frå Spole er sett tilbake til førre standardverdi.
Ekte felles førstegongsinnlogging mot brukaren sine tenarar er ikkje prøvd;
nettverks- og identitetsflyten blir testa med syntetiske tenarar.
