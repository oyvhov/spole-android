# Verifisering: Spole 0.17.0-beta23

18. september 2026. Versjonskode 102. Publisering pa uttrykkeleg forespurnad.

## Omfang

- Barnemodus og profilbyte (Fase 1: BM-1 til BM-4):
  - BM-1: Fleirprofil-stotte med isolerte tilkoplingar og tokens, uendra hovudkonto.
  - BM-2: Profilbyte direkte fra avatar-ikonet ovst til hogre (ProfileSwitcher).
  - BM-3: Legg til barnekonto fra Jellyfin (Users/Public) samt manuell innlogging med brukarnamn/passord.
  - BM-4: 4-sifra PIN-sikring med salta SHA-256 hash, to-stegs stadfesting, progressiv utestenging, og fjernkontrollstotte.
  - Foreldrekontroll: Innstillingar skjult i barnemodus; retur til hovudkonto krev PIN.
- Forbetra ikon og design pa nedlastings- og oppdateringssida.
- Rett biletvising pa «Hald fram a sja» for Jellyfin og Emby.
- Mjukare og jamnare navigering i TV-innstillingar.
- Reinare OSD-kontrollar pa mobil.

## Endeleg bygg og artefakt

- testDebugUnitTest assembleRelease: BUILD SUCCESSFUL.
- JVM: Alle einheittestane bestod (inkludert ProfileStorageTest, PublicUsersParserTest og PinSecurityTest).
- Pakke: app.reelstack, versjon 0.17.0-beta23, kode 102, minSdk 26, targetSdk 36, ikkje debuggable.
- APK: Spole-v0.17.0-beta23.apk (11 110 370 byte).
  SHA-256: 66c47f5c704653e55cd0fd2e51e2c40cba377340ee954b9694435c134d795fe5.
- Sertifikat SHA-256 stadfesta av apksigner:
  36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10.
- Native FFmpeg-kjelde-/relenkingsarkiv: spole-ffmpeg-source-and-relink.tar.gz (5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132).
- APK, mapping og sjekksummar arkiverte i app/build/release-v0.17.0-beta23/.

## Testkoyring pa TV-emulator

- Installert pa emulator-5564 med adb install -r.
- Eksisterande innlogging og brukardata bevarte.
- Full verifikasjon av profilveljar, oppretting av profil og feilhandtering mot ekte Jellyfin-tenar.
