# Verifisering: Spole 0.17.0-beta19

18. september 2026. Universal testutgåve for mobil og TV, pakkenamn `app.reelstack`,
versjonskode 98. Sjå [teknisk gjennomgang](PIXEL_AUDIO_AND_TV_CADENCE_2026-09-18.md).

## Testgrunnlag

- Endeleg Gradle-bygg: `BUILD SUCCESSFUL in 9m 41s`; `testDebugUnitTest`,
  `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease`.
  Lint: 0 feil, 93 åtvaringar. R8-optimalisering og release-kontroll fullførte.
- Både før og etter versjonsauken: 631 einingstestar og 29 målretta Android-testar
  bestod, ingen feil eller hoppa over. Endeleg Android-køyring: `OK (29 tests)`,
  151,009 sekund; lokal logg `app/build/release-v0.17.0-beta19/playback-tests.txt`.
- Android-testane bruker isolert 5562 og syntetiske Emby-/Jellyfin-kjelder.
  Injisert plattformfeil blir følgd av faktisk FFmpeg-dekoding utan ny serverforhandling.
  Pause, spoling, sporval, HLS og lesbare diagnosar ved 2× skrift er dekte.
- Fysisk Pixel 9 Pro og Shield/optisk/Sonos er ikkje testa. Årsaka til den rapporterte
  Jellyfin-hakkinga er ikkje stadfesta. Kjend PiP-returfeil frå beta18 er ikkje retta.

## Oppdatering og avgrensingar

- Standard oppstart av den lagra TV-review-profilen stoppa ved delt ADB-tilgang:
  `Address already in use`. Ingen ADB-tenar vart drepen og ingen review-data vart endra.
  Ekte kontoar på review-profilane kan difor ikkje verifiserast i denne runden.
- Eksisterande produksjons-beta18 (97) på isolert 5562 blir brukt for oppdateringskontroll.
  Dette er demomodus med nynorsk, ikkje ekte kontoar eller fysisk maskinvare.

## Artefakt

- APK: `Spole-v0.17.0-beta19.apk`, 11 063 178 byte, ikkje debuggable.
- Pakke `app.reelstack`, versjonsnamn `0.17.0-beta19`, kode 98, minSdk 26.
- APK SHA-256: `90c4a733465f58f48c8f3d585d67d132ce3fc3b04af669ac0ad6ce135d31c28c`.
- `apksigner verify --print-certs`: godkjend med same produksjonssertifikat:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- `zipalign -c -P 16 4`: godkjend. arm64-v8a, armeabi-v7a, x86 og x86_64.
- Native-modulen er uendra frå beta18. Kjelde-/relenkingsarkivet er identisk:
  `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`.
- APK og tilhøyrande R8-mapping er arkiverte under `app/build/release-v0.17.0-beta19/`.

Publiseringsresultat og oppdateringskontroll blir dokumenterte etter publisering.
