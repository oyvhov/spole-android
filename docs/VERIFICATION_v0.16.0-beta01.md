# Verifikasjon · 0.16.0-beta01

Pakke `app.reelstack`, versjonskode 68. Eksisterande produksjonssignatur skal bevarast.

## Omfang

Test på språk, heimeside, bottom navigation i lågt opptak, vurderingar, tenestestatusfelt,
demo-data og grunnleggjande navigasjon på TV/mobil/tablet.

## Kontroll

- Bygg: `app/build/beta01-unit.log`, `app/build/beta01-assembleDebug.log`,
  `app/build/beta01-assembleDebugAndroidTest.log`, `app/build/beta01-lint.log`
  og `app/build/beta01-assembleRelease.log`, alle BUILD SUCCESSFUL.
- Signert universal release-APK, ikkje debuggable, minimum API 26/Android 8.
- APK-storleik: `11 591 170` byte.
- SHA-256: `8f1795c5123d8ccedd15a0a43082cf4c29c400473f37fd83ca3cd0bca4f576ff`.
- Versjon frå bygg:
  - `versionName = 0.16.0-beta01`
  - `versionCode = 68`
  - pakkenamn `app.reelstack`
- Signatur DN: `CN=Spole, OU=Mobile, O=JellyBin, L=Oslo, ST=Oslo, C=NO`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK og tilhøyrande R8-mapping er arkiverte i `app/build/release-v0.16.0-beta01/` med `SHA256SUMS.txt`,
  `mapping-v0.16.0-beta01.txt` og `SOURCE_COMMIT.txt`.

## Testresultat

- Android-testoppgåver (instrumentering) er bygde og klarte for handsaming av APK, men blir ikkje køyrde som del av release-byggstegane.
- Oppdatert heimeside/radrekkjefølgje, språkstøtte og demo-opplegg er dekte i dokumenterte byggartefaktar.
- Verifisering på ekte eining blir gjort mot oppdateringsflyten etter publisering.

## Release

- Publisert som prerelease: https://github.com/oyvhov/spole-android/releases/tag/v0.16.0-beta01
- Kjeldecommit: `baeb2f92cacdb802fed62426503013d26658f9d6`
- App-APK-lenkje: https://github.com/oyvhov/spole-android/releases/download/v0.16.0-beta01/Spole-v0.16.0-beta01.apk
- Offentleg release-liste utan innlogging skal vise éin APK med riktig digest.
- Etterpublisering av last ned + installasjon via "Sjekk no" blir utført på review-emulator.







