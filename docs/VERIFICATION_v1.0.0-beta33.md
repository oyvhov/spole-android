# Verifisering av Spole 1.0.0-beta33

2. november 2026. Produksjonspakke `app.reelstack`, versjonskode 170. Bygd oppå beta31 (`455ea54`).

## Omfang

Greina `launcher-discover` (`c762b41..88ab573`, fletta med beta31 i `8bc9add`): Oppdag for Stova.
Kontrakten står i `LAUNCHER_LINK.md` under «Oppdag for Stova».

- `launcher/LauncherDiscover.kt`, `LauncherDiscoverQueries.kt`: stiane `discover`, `discover/requests`,
  `discover/title`, `discover/seasons`, `discover/related` og `discover/search` på profilleverandøren,
  berre for Stova og berre for vaksne profilar, med extra `seerr` = `none`/`browse`/`personal`.
- `launcher/LauncherDiscoverActions.kt`: kalla `discover_request` og `discover_withdraw`, som personen.
- `spole://discover` (`LauncherTarget`, `PendingLauncherOpen`, `LauncherOpenActivity`, manifestet):
  opnar tittelen i Oppdag, også frå kald start; ei lasta rad går føre plasshaldaren.
- Ny tekst `launcher_discover_grown_ups` på nynorsk, bokmål og engelsk.

Versjonsnamnet beta32 vart brukt til eit lokalt, release-signert testbygg (versjonskode 169) som ligg
på Loftet-TV-en. Denne utgåva heiter difor beta33 med kode 170, så Loftet kan oppdatere seg i appen.

## Bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`: BUILD SUCCESSFUL.
- `testDebugUnitTest`: 1 602 testar, 0 feil, 1 hoppa over (den valfrie nettverkstesten).
- `lintDebug`: 0 feil, 218 åtvaringar (same tal som beta31).
- `check-release-consistency.py`: OK for 1.0.0-beta33.
- Android-testane på dei isolerte emulatorane (5562/5566) er **ikkje** køyrde i denne runden: eigaren
  har bede om at det berre blir testa på ekte einingar no. Den ekte TV-en Loftet er brukt i staden.

## Artefakt

- `Spole-v1.0.0-beta33.apk`: 28 139 820 byte, SHA-256
  `b050cd96c105129ca202351b5118ae36a2fa155b31470931e87a9a7dba3227ff`.
- `app.reelstack`, versjonskode 170, versjonsnamn `1.0.0-beta33`, minSdk 26, targetSdk 36, ikkje debuggable.
- Sertifikat SHA-256 `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` (det faste).
- `check-native-subtitles.py`: OK (fire ABI-ar, 16 KB-justering, åtte lisensar). Dei 20 native biblioteka
  er byte for byte like testbygget på Loftet, og ingen native kjelde er endra sidan beta31. Kjeldearkiva
  for FFmpeg og dei native undertekstbiblioteka er difor dei same som i beta31 (same digest).

## Publisering

- Release-commit `40bb6e7`, tagg `v1.0.0-beta33`, pusha med `git push --atomic origin main v1.0.0-beta33`.
  CI («Bygg og test») er grøn på `40bb6e7`.
- Seks assets med digest kontrollert mot GitHub: APK, `SHA256SUMS.txt`, mapping, `SOURCE_COMMIT.txt`,
  FFmpeg-kjelda og kjeldearkivet for dei native undertekstbiblioteka. Releasen er publisert som
  prerelease (ikkje `latest`), og den offentlege nedlastinga utan innlogging har same SHA-256 som bygget.
- **Oppdatering i appen på ein ekte TV:** Loftet (Google TV Streamer) med det lokale beta32-bygget (169):
  Innstillingar → Varsler og oppdatering → Appoppdateringer → Sjekk nå fann `v1.0.0-beta33` (26,8 MB) med
  release-notata, Last ned → Installer oppdatering → Android sin «Oppdater». Etterpå: versjonskode 170,
  `1.0.0-beta33`, kontoen og «Fortsett å se» for Øyvind er der, og Oppdag i Spole viser førespurnader og
  «Populært nå». Stova 0.2.0 på same TV viser Oppdag-rada for Øyvind og Spole-test og ingenting for
  barneprofilen.
- Merk: beta32-arbeidet med toppmenyen (`C:/JellyBin/spole-sesong`, ucommitta) er ikkje med i beta33.
  Loftet viser difor den eldre toppmenyen («Bibliotek») etter oppdateringa.
