# Verifisering av Spole 0.11.3

## Kontrollar

- `testDebugUnitTest`: bestod.
- `assembleDebug`: bestod.
- `lintDebug`: bestod utan lint-feil.
- Radarr/Sonarr-feed: gamle release-datoar utanfor 28-dagarsvindauget blir filtrerte bort.
- Radarr-feed: berre digital/fysisk release blir brukt; kinodato åleine blir ignorert.
- Sonarr-feed: fortidige episodar går til Nyleg tilgjengeleg, framtidige episodar går til Kjem snart.
- Emulator: release-kort kan opnast i detaljpopup utan at appen krasjar.

## Avgrensing

`emulator-5560` hadde `app.reelstack.debug` med demodata og ingen lagra ekte tenestetilkoplingar i
denne køyringa. Ekte konto- og tenardata må derfor verifiserast etter at kontoane er logga inn i
same debug-installasjon.
