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
- Ekte TV 5564 blir halden på beta03 til offentleg GitHub-oppdatering kan prøvast. Endeleg resultat blir lagt til etter publisering.
- Reelle Emby-filmar, avspeling, bibliotek, Rotten Tomatoes og tenarbyte vart kontrollerte i dei føregåande lokale bygga; sjå EMBY_CINEMATIC_DETAILS.md. Endeleg TV-utsjånad blir kontrollert etter oppdateringa.
- Ingen emulatorbrukardata, kontoar, passord eller signeringsnøklar er del av release-kjelda.
- Alle HDR-/surround-/undertekstkombinasjonar og automatisk episodeskifte på fysisk TV er ikkje fullstendig verifiserte. Live-TV, platemenyar og offline-nedlasting er ikkje implementerte.
