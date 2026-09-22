# Verifikasjon · 0.16.0-beta04

Pakke `app.reelstack`, versjonskode 71. Eksisterande produksjonssignatur skal bevarast.

## Omfang

Test på oppstart, cache/opningsflyt, oppstartsbanner og releasetest.

## Kontroll

- Byggkommando: `./gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`
  med `--console=plain --max-workers=2` og køyring i release-grenen.
- Signert universal release-APK, ikkje debuggable, minimum API 26/Android 8.
- APK-storleik, versjon og SHA-256 (oppdatert frå faktisk bygg).
- Versjon frå bygg:
  - `versionName = 0.16.0-beta04`
  - `versionCode = 71`
  - pakkenamn `app.reelstack`
- Signatur DN: `CN=Spole, OU=Mobile, O=JellyBin, L=Oslo, ST=Oslo, C=NO`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK, R8-mapping og sjekksum arkiverast i `app/build/release-v0.16.0-beta04/` som:
  - `Spole-v0.16.0-beta04.apk`
  - `mapping-v0.16.0-beta04.txt`
  - `SHA256SUMS.txt`
  - `SOURCE_COMMIT.txt`
- SHA-256 for `Spole-v0.16.0-beta04.apk`: `c6452c7a2206b372f0ee8125fd23575e4db9a53267d941f5a79d6929c7a16838`.

## Testresultat

- Einingstestar: køyrd i byggestrengen (`testDebugUnitTest`: pass).
- Lint: `app/build/reports/lint-results-debug.html` viser ingen blokkerande feil.
- Instrumenteringstestar: bygd (`assembleDebugAndroidTest`) og klar for manuell køyring på testeiningar.

