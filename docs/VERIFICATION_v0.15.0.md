# Verifisering: Spole 0.15.0

8. september 2026. Integrert Jellyfin-spelar og rett klientnamn i Jellyfin.

## Endringar og regresjonsvern

- Det delte autorisasjonshovudet bruker no `Client="Spole"` for Jellyfin-innlogging,
  Quick Connect, bibliotek, profil, bilete og spelaren. Einings-ID, tilgangsteikn og lagring er uendra.
- JVM-testane kontrollerer namnet ved vanleg innlogging, Quick Connect, bibliotek og avspeling.
  Ein eigen test bevarer også ein eldre `homereel-android`-ID utan å bruke han som klientnamn.
- Android-spelartesten kontrollerer klientnamnet i faktiske HTTP-kall til den syntetiske
  testtenaren medan video blir dekoda. Den fokuserte kontrollen bestod før releasebygginga.
- Spelarkode og den førre manuelle testen med ekte Jellyfin-data er dokumenterte i
  `VERIFICATION_JELLYFIN_PLAYER.md`. Denne releasen endrar berre klientnamn og versjonsfelt i produksjonskoden etter den testen.

## Releasekontroll

- Versjon: 0.15.0 / 42. Pakke: `app.reelstack`. Android 8.0+, targetSdk 36.
- JVM: 253 testar, 0 feil, 0 hoppa over.
- Bygg: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease` bestod.
- Lint: 0 feil, 28 åtvaringar, som i spelargjennomgangen. Ingen nye undertrykte reglar.
- Første Android-runde: 115/116 bestod. Søkjetesten kontrollerte eit felt før den asynkrone
  fanetilstanden hadde oppretta det. Testen ventar no på at feltet finst (maks 5 sekund), og
  kontrollerer framleis at eit vanleg fanebesøk ikkje tek fokus. Ingen produksjonskode er endra for dette.
- Endeleg Android-runde: **116/116 bestod** på `emulator-5562`, 238,026 sekund.
- Signert APK vart installert med `-r` på `emulator-5560`. Pakkevisinga stadfesta 0.15.0 / 42,
  appen starta, og Innstillingar viste Jellyfin, Emby og Seerr som tilkopla med dei bevarte kontoane.

## APK-identitet

- Fil: `Spole-v0.15.0.apk`, 3 460 917 byte.
- SHA-256: `d847015ec8451c60db507acc219639d7517d2d60d9bb89c8ad05baa342bb6fec`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping frå same produksjonsbygg blir publisert saman med APK-en.
- APK-innhaldet vart kontrollert: ingen syntetiske spelar-testfiler eller signeringsnøklar.

## Avgrensingar

Emulatoren er ikkje ein måling av yting eller batteribruk på fysisk telefon. Alle kodekar,
HDR/biletundertekstar, langvarig avspeling og mobilnettbyte er ikkje verifiserte. Omkoding krev
støtte og kapasitet på Jellyfin-tenaren. Emby-avspeling, casting, PiP og nedlasting er ikkje med.

Ingen ekte kontoar eller passord blir brukte i automatiske testar. Review-emulatoren får berre
oppdatering over eksisterande app; instrumentering køyrer på den separate testemulatoren.
