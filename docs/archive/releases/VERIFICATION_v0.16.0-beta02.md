# Verifikasjon · 0.16.0-beta02

Pakke `app.reelstack`, versjonskode 69. Eksisterande produksjonssignatur skal bevarast.

## Omfang

Test på språk/tekst, heimeordning, oppstart, vising av tenester og demo-inndata.

## Kontroll

- Bygg: `app/build/beta02-build.log` frå
  `./gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease` med
  `--console=plain --max-workers=2 --no-build-cache -Pksp.incremental=false`, avslutta med `BUILD SUCCESSFUL`.
- Signert universal release-APK, ikkje debuggable, minimum API 26/Android 8.
- APK-storleik: `11 631 790` byte.
- SHA-256: `6a0a8502b74e3b6db5c3e7d34ba5cce1e760d75115e07af5e41b795ec8f8aa5a`.
- Versjon frå bygg:
  - `versionName = 0.16.0-beta02`
  - `versionCode = 69`
  - pakkenamn `app.reelstack`
- Signatur DN: `CN=Spole, OU=Mobile, O=JellyBin, L=Oslo, ST=Oslo, C=NO`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK, R8-mapping og sjekksum er arkiverte i `app/build/release-v0.16.0-beta02/` som:
  - `Spole-v0.16.0-beta02.apk`
  - `mapping-v0.16.0-beta02.txt`
  - `SHA256SUMS.txt`
  - `SOURCE_COMMIT.txt`

## Testresultat

- Einingstestar: utført som del av byggestrengen.
- Lint: rapportert i `app/build/reports/lint-results-debug.html` og bygget er framført utan kjende kritiske lint-feil i rapport.
- Instrumenteringstestar er bygde, men har ikkje vorte køyrde i denne omgang.
- Oppdateringsflyt er ikkje verifisert i ein ekstern installasjon i denne seksjonen enda.
