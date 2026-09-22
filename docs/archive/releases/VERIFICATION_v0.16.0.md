# Verifikasjon: Spole 0.16.0

Versjonskode 78, produksjonspakke `app.reelstack`. Ordinær utgåve frå main.

## Automatiske kontrollar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`: BUILD SUCCESSFUL.
- 489 einingstestar, ingen feil eller hoppa over. Ny regresjonstest køyrer både Jellyfin og Emby med Seerr utilgjengeleg og krev at biblioteket blir levert før Seerr svarar.
- 22 Android-testar bestått på isolert TV-profil 5566: StartupRevealTest (6), CombinedSetupUiTest (6), HomeMediaRowsTest (6), SynopsisLayoutTest (3), TvLibraryRegressionTest (1).
- Lint: 0 feil, 31 åtvaringar.
- APK-signatur kontrollert mot eksisterande sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.

## Kontroll med ekte data

Signert APK installert over eksisterande installasjon på lagra TV-profil 5564. Kontoane er bevarte, Heim viser ekte Jellyfin-rader og hero. Episodedetaljar kontrollerte visuelt: kompakt hovudfelt med eige episodebilete, sesongveljar og episodar i nummerrekkjefølgje. Ingen private skjermbilete er lagde i Git.

Dei nye oppdateringane og filtreringa ved fjerning av tenester er i felles datalag for telefon, nettbrett og TV. Dette er ikkje ei måling på brukaren sitt fysiske nettbrett, og vi lovar ikkje ei bestemt oppstartstid. Den separate mobile instrumenteringsprofilen er utilgjengeleg; TV-køyringa erstattar ikkje mobiltesting av Quick Connect med stor skrift. Ny ekte Quick Connect-innlogging er ikkje køyrd mot brukarkontoane.

## Publiseringskontroll

Publisert som ordinær, offentleg release `v0.16.0`, med éin APK, SHA256SUMS, R8-mapping og SOURCE_COMMIT. Taggen peikar på `8c6fd0a`. Main er oppdatert.

- APK: `Spole-v0.16.0.apk`, 11 650 290 byte.
- SHA-256: `33bc91c68dd871d48f6fa55d6aaab452eb76df2f529651b2f95ef1e52449f232`.
- GitHub-digest kontrollert før publisering. Metadata henta utan autorisasjon etter publisering; draft=false og prerelease=false. Offentleg nedlasting har same SHA-256.
- Den lagra mobilprofilen 5560 starta Android, men eksponerte ikkje produksjonspakka eller ekstern lagring. Ingen data vart sletta eller erstatta. Oppdatering med ekte mobilkontoar kunne difor ikkje gjennomførast.
- I staden vart publisert beta09 installert på isolert test-TV 5566. Via appen: Sjekk no fann 0.16.0, nedlasting og kontroll fullførte, Android sitt installasjonsløyve vart gitt til produksjonsappen og installasjonsdialogen godkjend. Installert versjon er kontrollert til 0.16.0 / 78. Denne testen brukar demo og dokumenterer nedlastings-/installasjonsflyten, ikkje ekte kontobevaring; den siste er kontrollert separat på TV-profil 5564.
