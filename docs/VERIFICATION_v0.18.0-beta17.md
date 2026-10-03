# Verifisering av Spole 0.18.0-beta17

3. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 137.

- Kjapp release etter uttrykkeleg ønske frå brukaren: testsuiten er ikkje køyrd på nytt etter versjonsauken.
- Preview-fiksen vart rett før releasen verifisert med 837 einingstestar, null lint-feil og 3/3 spelartestar ved både systemskrift 1.0 og 2.0. Sjå [arbeidsrapporten](PLAYER_NO_SEEK_PREVIEW_2026-10-03.md).
- `assembleRelease`: BUILD SUCCESSFUL, 2 minutt 21 sekund, avslutta med kode 0.
- APK: 11 453 710 byte, minSdk 26, targetSdk 36, fire ABI-ar, ikkje debuggable.
- SHA-256: `befa604c7e499e87e02847cbd43868b5b19d81823e83b63cdd5a220b6a8642f8`.
- Sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK og nøyaktig R8-mapping arkiverte i `app/build/release-v0.18.0-beta17`.
- Fire FFmpeg-bibliotek og to lisensfiler er byteidentiske med beta16. Tilhøyrande kjelde-/relenkingsarkiv har SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Signert `install -r` frå beta16 til beta17 på lagra TV-profil 5564 lykkast. Første installasjonstid er framleis 9. september 2026 kl. 20:50:58. Ingen appdata sletta eller instrumentering på denne profilen.
- Full nedlasting/installasjon gjennom appen blir ikkje repetert i denne kjappe releasen. Offentleg nedlasting og GitHub-digest blir kontrollerte etter publisering.
