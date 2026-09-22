# Verifisering: 0.16.0-beta09

Versjonskode 76, pakke `app.reelstack`. Same eksisterande signeringssertifikat.

## Release-bygg

- `testDebugUnitTest` (477/477), `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease` køyrde med PASS.
- Release-katalogen skal innehalde APK, `mapping.txt`, `SHA256SUMS.txt` og `SOURCE_COMMIT.txt` før publisering.

## Endringar

- API-kall brukar delt OkHttp-klient med connection pooling.
- Heimefeed ber om færre felt og kan lagre delvis cache ved tenestefeil.
- Hero-kandidatane blir blanda mellom framhald, Neste opp og nyleg innhald.

## APK-verifikasjon

- SHA-256: `db9b1d4064175282a7e3109a471555208083c856f76c62d8364c0777543f84ca`.
- Pakkeidentitet frå `aapt dump badging`: `app.reelstack`, `versionCode='76'`, `versionName='0.16.0-beta09'`, `minSdk='26'`, `targetSdk='36'`.
- Signert med oppgåva sitt faste sertifikat (`36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`).
