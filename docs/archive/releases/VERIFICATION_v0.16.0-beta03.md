# Verifikasjon · 0.16.0-beta03

Pakke `app.reelstack`, versjonskode 70. Eksisterande produksjonssignatur skal bevarast.

## Omfang

Test på oppstart, ikon/splash, biletlasting og release-artifakt.

## Kontroll

- Byggkommando: `./gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`
  med `--console=plain --max-workers=2` og kjøring i release-grenen.
- Signert universal release-APK, ikkje debuggable, minimum API 26/Android 8.
- APK-storleik, versjon og SHA-256 (oppdatert frå faktisk bygg).
- Versjon frå bygg:
  - `versionName = 0.16.0-beta03`
  - `versionCode = 70`
  - pakkenamn `app.reelstack`
- Signatur DN: `CN=Spole, OU=Mobile, O=JellyBin, L=Oslo, ST=Oslo, C=NO`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK, R8-mapping og sjekksum arkiverast i `app/build/release-v0.16.0-beta03/` som:
  - `Spole-v0.16.0-beta03.apk`
  - `mapping-v0.16.0-beta03.txt`
  - `SHA256SUMS.txt`
  - `SOURCE_COMMIT.txt`
- SHA-256 for `Spole-v0.16.0-beta03.apk`:  
  `de27d88737392cd7cef5113233c8a32ea13907ff2fbaede47b406ef93125facd`.

## Testresultat

- Einingstestar: køyrd i byggestrengen.
- Lint: rapportert i `app/build/reports/lint-results-debug.html` og bygget skal vere fri for blokkerande feil.
- Instrumenteringstestar: bygd; kjøyring og manuell bekreftelse på ekstern TV kan utførast i oppdateringssteg.
