# Verifisering: 0.16.0-beta05

Versjonskode 72, pakke `app.reelstack`. Same eksisterande signeringssertifikat.

## Release-bygg

- Fullt build-forløp avslutta med `BUILD SUCCESSFUL` etter 2 minutt 57 sekund.
- 477/477 einingstestar bestod (`testDebugUnitTest`).
- Android-testpakke/assembly vart bygd (`assembleDebugAndroidTest`), men instrumentert APK-testkøyring
  vart ikkje gjennomført i denne syklusen.
- Lint (`lintDebug`) rapporterte 30 åtvaringar og 0 feil.
- Universal produksjons-APK, ikkje debuggbar: `Spole-v0.16.0-beta05.apk`, 11 632 890 byte.
- SHA-256: `dd133346134f95669b9a801bf9a4a3468ea31e06032f04e67e617d8b0d4dbe4a`.
- Verifisert sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK, R8-mapping og sjekksum er arkivert i `app/build/release-v0.16.0-beta05`.

## Oppdateringskontroll

- Versjonen er pakka med `versionCode=72` og `versionName=0.16.0-beta05`, og `aapt` rapporterer pakke-ID `app.reelstack`.
- Release-artefakt må publiserast som prerelease og ikkje draft i offentleg GitHub-release.