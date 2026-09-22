# Verifikasjon: 0.17.0-beta04

## Artefakt

- Pakke: `app.reelstack`, versjonskode **83**, versjon **0.17.0-beta04**.
- Universal release-APK: **11 735 322 byte**, ikkje debuggable, minimum Android 26.
- SHA-256: `31b39dc252520a404e3657eaee42ed634cc29344830507f78d9e98e224f280df`.
- Signatur: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping, SHA256SUMS og SOURCE_COMMIT blir leverte som separate release-assets.

## Bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`: BUILD SUCCESSFUL.
- **520/520 einingstestar** bestått. Emby-profil, bibliotek/filter, avspeling, kapittelmarkørar, spor, neste episode, vurderingar og kompakt episodeformat er dekte.
- **48 ulike TV-testar på 5566** bestått etter målretta omkøyring: TvRefinementUiTest, TvLibraryRegressionTest, DesignRefreshUiTest, WatchNextTest, LocalPlaybackStoreTest og EpisodeSeriesNavigationTest.
- TV-testane dekkjer høgare hero, pikselinnhald bak detaljtekst, stor skrift 2.0, tenarmeny, lagra tenarval og faktisk venstrejustering av logopikslar mot episodelinja.
- **19 ulike mobiltestar på 5562** bestått: JellyfinPlayerUiTest og SheetInteractionTest. Ein test med eitt sekund tidsgrense feila under parallell emulatorlast, men passerte uendra ved isolert omkøyring.
- Eldre DesignRefresh-assertar vart oppdaterte for ny rekkjefølgje på tittel/episodelinje, samanslått omtalesemantikk og rulling til mappekorta under den høgare heroen. Alle tre korrigerte testar passerte på TV. Første breie køyring var ikkje heilt grøn; tala over viser unike testar som er verifiserte etter retting/omkøyring, ikkje éi feilfri samla køyring.
- Lint: **0 feil**, 67 åtvaringar. `git diff --check` rein.

## Installering og grenser

- Endeleg signert APK installert med `install -r` på lagra review-mobil 5560. Mobilen viste onboarding før oppdatering; eksisterande tilstand vart bevart. Dette er ikkje prov på innlogga mobilkontoar.
- Ekte TV 5564 vart oppdatert frå beta03 gjennom Spole sin eigen GitHub-flyt etter publisering: Sjekk no viste beta04, Last ned fullførte kontrollen, Installer opna Android-dialogen, og Update installerte versjonskode 83. Dette var ikkje `adb install` på TV-en.
- Reelle Emby-filmar, avspeling, bibliotek, Rotten Tomatoes og tenarbyte vart kontrollerte i dei føregåande lokale bygga; sjå EMBY_CINEMATIC_DETAILS.md. Etter GitHub-oppdateringa viste TV-en ekte innhald og lagra profil att, utan ny innlogging. Blå aksent og eksisterande utsjånadsval var bevarte.
- Endeleg TV-hero og detaljbakgrunn er visuelt kontrollerte. Ekte Jellyfin-episode vart spelt til 0:06 og pausa; clearlogo og S1 - E5 hadde felles venstrekant i OSD. Avspelinga vart avslutta etter kontrollen. Private bilete ligg berre i ignorert `app/build/spole-beta04-*.png`.

## Offentleg oppdatering

- Publisert prerelease: https://github.com/oyvhov/spole-android/releases/tag/v0.17.0-beta04
- APK: https://github.com/oyvhov/spole-android/releases/download/v0.17.0-beta04/Spole-v0.17.0-beta04.apk
- Release-kjelde: `fe6e1e34dd529d7b5e55281b032aad2258a235a0`.
- Offentleg metadata henta utan autentisering: ikkje draft, éin APK, riktig storleik og digest.
- Offentleg nedlastingsfil har same SHA-256 som den bygde APK-en. Mapping og SOURCE_COMMIT er lasta opp.
- Ingen emulatorbrukardata, kontoar, passord eller signeringsnøklar er del av release-kjelda.
- Alle HDR-/surround-/undertekstkombinasjonar og automatisk episodeskifte på fysisk TV er ikkje fullstendig verifiserte. Live-TV, platemenyar og offline-nedlasting er ikkje implementerte.
