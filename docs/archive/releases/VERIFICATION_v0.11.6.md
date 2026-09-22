# Verifisering av Spole 0.11.6

- `compileDebugKotlin`, `assembleDebug` og `lintDebug` består.
- `testDebugUnitTest` består med 151 testar og 0 feil.
- APK-en vart installert og opna på `emulator-5560` med demodata under emulatorpasset.
- Detaljarket vart opna frå Home og viste fast viewport, poster, fakta, omtale og biblioteksstatus.
- Den siste standardhøgda på 82 % er med i sluttbygget og er kontrollert gjennom lint/bygg og den
  eksisterande faste-viewport-regresjonstesten.
- Live Seerr/Jellyfin-data er ikkje verifisert på emulatoren i denne køyringa; emulatoren har ikkje
  aktive lagra tenestekontoar.
- Full connected-testkøyring starta 62 testar, men emulatoren gjekk offline etter 22 testar. Den eine
  registrerte feilen var derfor ein emulator-/ADB-feil, ikkje ein rapportert app-crash.
