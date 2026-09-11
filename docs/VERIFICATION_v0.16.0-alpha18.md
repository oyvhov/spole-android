# Verifikasjon - Spole 0.16.0-alpha18

11. september 2026. Pakke `app.reelstack`, versjonskode 61.

## Bygg og testar

- Full Windows-byggje- og testrekkje: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`.
- JVM-testar: 400 køyrde, 400 bestått, 0 feil, 0 hoppa over.
- Lint: 0 feil, 47 åtvaringar.
- Visuell kontroll på den varige TV-profilen `Spole_GoogleTV_Test` / `emulator-5564` med ekte innhald.
- Heroen held toppen og profilen synleg med fokus på «Sjå meir».
- Den første `Spelar no`-rada, inkludert heile kortet, er synleg i første skjermbilete.
- Ingen full instrumenteringsrunde vart køyrd; endringa er avgrensa til TV-oppsettet og vart kontrollert direkte på målskjermen.

## APK

- APK: `Spole-v0.16.0-alpha18.apk`
- Pakke: `app.reelstack`
- Versjon: `0.16.0-alpha18`
- Versjonskode: 61
- Storleik: 3 949 341 byte
- SHA-256: `81605056739c0aa7b759f317753c1489ab4edecd193e8bfc86f1f3d31ff11937`
- minSdk: 26
- targetSdk: 36
- `application-debuggable` finst ikkje i badging-output.
- Signeringssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`

## Oppdatering over lagra data

- Produksjons-APK-en vart installert med `adb install -r` på den varige TV-profilen.
- Android sin `firstInstallTime` var framleis 9. september 2026 etter oppdateringa; berre `lastUpdateTime` endra seg.
- Jellyfin- og Seerr-kontoane og ekte bibliotekinnhald var framleis tilgjengelege etter oppdateringa.
- Ingen appdata vart sletta.

## Artefaktar og GitHub-release

- Release-taggen peikar på kjeldecommit `a57f82de37be3bb42be2370b98cd06c7faf24c23`.
- Publisert som prerelease: <https://github.com/oyvhov/spole-android/releases/tag/v0.16.0-alpha18>
- Offentleg APK-lenke: <https://github.com/oyvhov/spole-android/releases/download/v0.16.0-alpha18/Spole-v0.16.0-alpha18.apk>
- Releasen er ikkje ein kladd og inneheld nøyaktig éin APK.
- GitHub asset digest og offentleg nedlasta APK samsvarar med lokal SHA-256: `81605056739c0aa7b759f317753c1489ab4edecd193e8bfc86f1f3d31ff11937`.
