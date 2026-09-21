# Verifisering — Spole 0.18.0-beta2

Dato: 22. september 2026

## Bygg og identitet

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: bestått i den reine WSL-arbeidskopien.
- `:app:assembleRelease`: bestått i Windows med den faste Spole-signaturen.
- Pakke: `app.reelstack`; versjon: `0.18.0-beta2` (versionCode 122).

## Målretta kontrollar

- `SheetEscapeTest`: 4/4 bestått på isolert emulator `emulator-5562`.
- TV-reviewprofil `emulator-5564`: signert APK oppgradert med `adb install -r`; installert versjon og heimesida kontrollert utan å starte avspeling eller endre konto-/framdriftsdata.
- Heil Android-testkøyring vart starta på `emulator-5562`. Ho avdekte eldre UI-forventingar og x86-video-tidsgrenser utanfor denne endringa; dei målretta navigasjons- og arketestane over er bestått.

## Relevante avgrensingar

- Chromecast er medvite ikkje i denne releasen.
- Inga nedlasting vert vist eller tilgjengeleg på TV.
