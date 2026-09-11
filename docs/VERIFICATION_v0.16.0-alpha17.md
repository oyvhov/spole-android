# Verifikasjon - Spole 0.16.0-alpha17

11. september 2026. Pakke `app.reelstack`, versjonskode 60.

## Bygg og testar

- Full Windows-byggje- og testrekkje: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`.
- JVM-testar: 400 køyrde, 400 bestått, 0 feil, 0 hoppa over.
- Lint: 0 feil, 47 åtvaringar.
- Målretta Android-instrumentering på isolert `Spole_Instrumentation` / `emulator-5562`: 11 av 11 kontoskjermtestar bestod.
- Heile instrumenteringspakken frå alpha16 vart ikkje køyrd om att; endringa er avgrensa til kontotilstand og fallback i innloggingsarket.

## APK

- APK: `Spole-v0.16.0-alpha17.apk`
- Pakke: `app.reelstack`
- Versjon: `0.16.0-alpha17`
- Versjonskode: 60
- Storleik: 3 949 341 byte
- SHA-256: `6479e0d7f78d4e9bf79ee854709eeb72648a1e5c1a97b6b4e4cf9581bd041b13`
- minSdk: 26
- targetSdk: 36
- `application-debuggable` finst ikkje i badging-output.
- Signeringssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`

## Oppdatering over lagra data

- Produksjons-APK-en vart installert med `adb install -r` på den varige review-profilen `Spole_Review`.
- Profilen vart oppdatert direkte frå versjonskode 57 til 60.
- Android sin `firstInstallTime` var framleis 7. september 2026 etter oppdateringa; berre `lastUpdateTime` endra seg.
- Appen viste den vanlege heimesida både før og etter oppdateringa, ikkje førstegongsoppsettet. Ingen appdata vart sletta.

## Artefaktar og GitHub-release

- Release-taggen peikar på kjeldecommit `d22fe5e8c8ce6e5a530fef8fbbed6ecad3c47074`.
- Publisert som prerelease: <https://github.com/oyvhov/spole-android/releases/tag/v0.16.0-alpha17>
- Offentleg APK-lenke: <https://github.com/oyvhov/spole-android/releases/download/v0.16.0-alpha17/Spole-v0.16.0-alpha17.apk>
- Releasen er ikkje ein kladd og inneheld nøyaktig éin APK.
- GitHub asset digest og offentleg nedlasta APK samsvarar med lokal SHA-256: `6479e0d7f78d4e9bf79ee854709eeb72648a1e5c1a97b6b4e4cf9581bd041b13`.
- GitHub `latest` peikar framleis på stabil `v0.15.1`; alpha17 blir funnen når Testutgåver er på.

## Merknader

- Rettar kontotilstanden og Seerr-innlogginga som vart rapportert etter oppdatering til alpha16.
- Direkte kontakt med `seerr.midtunet.no` kunne ikkje testast frå byggjemaskina fordi namnet berre er tilgjengeleg i brukaren sitt nettverk. Seerr-feilen er difor verifisert med nettverks- og UI-testar, ikkje ekte innloggingsdata.
