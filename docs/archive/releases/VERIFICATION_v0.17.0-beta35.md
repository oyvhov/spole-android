# Verifisering: Spole 0.17.0-beta35

20. september 2026. Versjonskode 114. Publisert som testutgåve på uttrykkeleg førespurnad.

## Omfang

- Resume-førespurnader ber no om både `Primary` og `Thumb` frå Jellyfin og Emby.
- Breie «Fortsett å sjå»-kort prioriterer miniatyrbiletet.
- Cache-schema er auka for å unngå at gamle poster-URL-ar blir viste ved oppstart.

## Endeleg bygg og artefakt

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  `BUILD SUCCESSFUL`, avslutta med kode 0.
- JVM-testar: 700/700 bestått, 0 feil, 0 hoppa over.
- Lint: 0 feil, 125 åtvaringar.
- Pakke `app.reelstack`, versjon `0.17.0-beta35`, kode 114, minSdk 26, targetSdk 36,
  ikkje debuggable. Universal: arm64-v8a, armeabi-v7a, x86, x86_64.
- APK: 11 204 174 byte. SHA-256:
  `5dcf84ae5b65acf7f4644efcd681a487da9d348da77abbf342ec4f775913b522`.
- Signatur SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- FFmpeg-kjelde-/relenkingsarkiv SHA-256:
  `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`.

## Android-testar og review

Android-testane vart ikkje køyrde i denne release-runden. Berre `emulator-5564` var tilkopla,
og han er ein review-eining med ekte brukardata som ikkje skal få instrumentering. Isolerte
5562/5566 var ikkje tilgjengelege. Ingen ekte kontoar eller appdata vart endra.

## Publisering

- Commit/tag: `12de424048b668ef847b85c96725e4b829161241` / `v0.17.0-beta35`.
- Publisert som prerelease, ikkje draft, med nøyaktig éin APK.
- GitHub-digest og offentleg nedlasta APK samsvarar med lokal SHA-256.
- Release: https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta35
