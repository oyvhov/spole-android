# Spole 0.17.0-beta39 – verifikasjon

Dato: 2026-09-21

## Resultat

- Versjon: `0.17.0-beta39`, versionCode `118`
- Commit: `7b5ef41` (`7b5ef41f7d6bec02503d33e8e5d47ac562b939a4`)
- Tag: `v0.17.0-beta39`
- Release: https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta39
- APK: [Spole-v0.17.0-beta39.apk](https://github.com/oyvhov/spole-android/releases/download/v0.17.0-beta39/Spole-v0.17.0-beta39.apk), 11 211 694 byte
- APK SHA-256: `4abe20a154896700f93562f03dd8258afaecbcf43dd17dc82a8ce736d73caf89`
- APK-signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`
- FFmpeg-kjeldearkiv SHA-256: `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`

GitHub-releasen er publisert som prerelease med nøyaktig éin universal-APK. Den offentlege
nedlastinga fekk same SHA-256 som den lokale APK-en, og GitHub oppgir same asset-digest.

## Endringar

- Mobilinnstillingane er samla i færre kategoriar, med Heim og navigasjon på same side.
- Appnamn, bibliotekvising, teksting og appspråk er flytta til meir logiske innstillingsgrupper.
- Ny separat tømming av bibliotekbufferen, utan å røre biletbuffer, innlogging eller historikk.
- Besøkte Jellyfin- og Emby-biblioteksider blir viste frå cache medan tenaren blir oppdatert.
- Profilpopup, speler-OSD og undertekstfeil er retta/stabiliserte.

## Test og bygg

- Release consistency-sjekk: bestått.
- `testDebugUnitTest`: 701 testar, 0 feil.
- `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease`: bestått.
- Lint: 0 feil, 127 åtvaringar og 1 hint; åtvaringane er eksisterande/deprecated API-varsel.
- APK verifisert som signert, ikkje debuggable, package `app.reelstack`, minSdk `26`, targetSdk `36`.
- Release-APK-en vart installert med `adb install -r` på `emulator-5564`. Kontoar og appdata vart
  ikkje sletta eller nullstilte.
- Full instrumenteringstest vart ikkje køyrd i denne releaseøkta; ho skal køyrast på den isolerte
  testemulatoren, ikkje på profilen med ekte kontoar.
