# Verifisering: Spole 0.15.1

8. september 2026. Raskare avspelingskontrollar, fungerande tilbake under video
og animert Spole-logo ved oppstart. Funksjonsendringane er dokumenterte i
`VERIFICATION_STARTUP_PLAYER_POLISH.md`; denne releasen byggjer på `cb39612`.

## Endeleg test av releasekoden

- Versjon: 0.15.1 / 43. Pakke: `app.reelstack`, Android 8.0+, targetSdk 36.
- Bygg: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`
  bestod på Windows med prosjektet sitt eksisterande signeringsoppsett.
- Einingstestar: **253/253**, ingen feil eller hoppa over.
- Full Android-runde: **127/127 bestod**, 300,311 sekund, på `emulator-5562`.
- Lint: **0 feil, 28 åtvaringar**, ingen nye undertrykkingar.
- Den tidlegare ufullførte fulltesten er no erstatta av ein fullført releasekontroll.

Berre éin emulator køyrde om gongen for å unngå minnemangelen frå førre gjennomgang.
Testane brukte den eksisterande isolerte `Spole_Instrumentation` med syntetiske kontoar.
Ingen instrumentering vart køyrd på review-emulatoren med ekte kontoar.

Fullpakken omfattar 13 testar med faktisk videodekoding og åtte testar av spelargrensesnittet:
Android Tilbake, skjermpila etter automatisk skjuling, berøring → kontrollar → pause,
spoling, rotasjon, lydspor, undertekst, kvalitet, bakgrunn, kontoendring og feil/retry.
Seks oppstartstestar kontrollerer mellom anna logoform, stabil layout, avgrensa venting,
tilgjengelegheit og skriftstorleik 2.0. Framside, søk, popup, innlogging og førespurnader
er òg med i fullpakken.

## APK-identitet

- Fil: `Spole-v0.15.1.apk`, **3 477 297 byte**.
- SHA-256: `cb868d36b711560a772e6c00d03caf1b67a3dd145834766076dbebbdb1dd6cef`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Versjon og pakkenamn er kontrollerte i byggmetadata, og signaturen er verifisert.
- APK-innhaldet inneheld ikkje spelar-testfiler eller signeringsnøklar.
- `mapping-v0.15.1.txt` frå det same R8-byggjet og `SHA256SUMS.txt` følgjer APK-en.

## Oppdatering på review-emulatoren

- `Spole_Review` / `emulator-5560` vart opna att med eksisterande data etter fulltesten.
- Pakkevisinga stadfesta først 0.15.0 / 42. Den signerte APK-en vart installert med `-r`,
  og pakkevisinga stadfesta deretter **0.15.1 / 43**.
- Appen opna normalt; profilen og ekte Jellyfin-/Emby-bibliotek vart viste på framsida.
- Innstillingar viste Jellyfin, Emby og Seerr som **Tilkopla**. Ingen kontoar vart sletta.
- Skjermbilete og UI-uttrekk ligg berre lokalt i ignorert byggmappe, ikkje som releasevedlegg.
- Manuell kontroll av ekte Jellyfin-video på den same spelarkoden vart gjort før versjonsauken,
  som dokumentert i `VERIFICATION_STARTUP_PLAYER_POLISH.md`. Releasepakken er i tillegg
  kontrollert med heile den automatiske videotestpakken.

## Avgrensingar

Emulatortestar er ikkje ein attest på bildefrekvens, batteribruk eller alle kodekar på
fysiske telefonar. Langvarig avspeling, alle HDR-/tekstformat og mobilnettbyte er ikkje
fullt verifiserte. Nokre straumar krev omkoding på Jellyfin-tenaren. Emby-avspeling,
casting, bilete-i-bilete og offline-nedlasting er ikkje med i denne versjonen.
