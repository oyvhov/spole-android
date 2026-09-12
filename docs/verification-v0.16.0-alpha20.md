# Verifisering av Spole 0.16.0-alpha20

13. september 2026. Pakke `app.reelstack`, versjonskode 63.

## Omfang

Samlar TV-navigasjon og neste-episode-endringane i `TV_UI_POLISH.md` og mobil-/sesongendringane i `MOBILE_SETTINGS_AND_SEASONS.md`. Desse rapportane dokumenterer tidlegare lokale bygg med versjonskode 62; APK-identiteten der er historisk.

Ingen kontoar, signeringsfiler, emulator-data eller skjermbilete med ekte innhald er med i Git eller release-assets. Testane brukar isolert profil. Dei innlogga profilane blir berre oppdaterte over eksisterande app.

## Avgrensingar

Ingen fysisk treg TV var tilgjengeleg. Neste-episode-kortet og overgangsreglane er testa med syntetiske avspelingstilstandar; full overgang mellom to verkelege mediefiler er ikkje testa. Eksisterande botnmeny på mobil kan bryte lange etikettar ved dobbel skriftstorleik, medan dei nye innstillingsradene veks med teksten.

## Testresultat for versjonskode 63

- 448 av 448 einingstestar bestått, ingen hoppa over.
- 26 av 26 Android-testar bestått på isolert mobilprofil 5562: MobileSettingsTest, UiConsistencyTest, WideNavigationSettingsTest, TvSettingsRedesignTest, PersonalizationTest og tre innstillings-/kontoflytar frå ReelstackSmokeTest. TV-/nettbrettvisingar er eksplisitt konfigurerte av dei respektive UI-testane.
- Lint: 0 feil, 29 åtvaringar.
- Før versjonsløftet vart også 37 TV-testar og 12 mobile bibliotek-/kontotestar køyrde; sjå den historiske TV-rapporten for nøyaktig omfang og tidsavbrot. Desse er ikkje talde som nye testar av versjonskode 63.

## Endeleg APK

- Full byggkommando fullførte med `BUILD SUCCESSFUL` på 6 minutt og 11 sekund.
- Universal produksjons-APK: `Spole-v0.16.0-alpha20.apk`, 3 983 574 byte.
- Android 8.0 / API 26 eller nyare. ARMv7, ARM64, x86 og x86_64 i same APK. Ikkje debuggbar.
- SHA-256: `2d0627f42d96a0e7a088745333e72f46cfb6f91b0d27f2fe96844b9dbb30f48b`.
- Verifisert sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK, tilhøyrande R8-mapping og sjekksum er arkiverte i `app/build/release-v0.16.0-alpha20`. `SOURCE_COMMIT.txt` blir skrive etter kjeldecommit.

## Oppdatering

Før publisering vart den signerte APK-en installert med `install -r` over alpha19 på den innlogga mobilprofilen 5560. Android stadfesta versjonskode 63. Kontoavatar, ekte bibliotek/favorittar og Skog/Lime-tema vart viste etter oppstart. Første opning kom før emulatornettet var klart; ny opning etter nettverksoppstart lasta ekte innhald.

TV-profilen 5564 står på alpha19 for kontroll av den offentlege nedlastings- og installasjonsflyten etter publisering.
