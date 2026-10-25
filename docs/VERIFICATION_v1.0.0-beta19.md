# Verifisering av Spole 1.0.0-beta19

25. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 156.

## Omfang

Tilbakemelding på beta18 frå ekte TV: når fokus stod på «Home» i den samanslegne sidemenyen, viste
appen namnet i ei eiga boble ved sida av ikonet, over heroen. Eigaren vil ikkje ha slike etikettar
på TV.

Tre stader viste ein slik flytande etikett på TV, og alle er fjerna:

- `NavigationControl`: ein popup med namnet 220 ms etter at eit ikon i den samanslegne sidemenyen
  fekk fokus. Han fanst berre på TV og er teken bort.
- `TvPlayerAction`: ein Material-tooltip over knappen «Avspelingsval» i avspelaren, opna ved fokus.
- `LibraryHub`: Material-tooltipen på «Tilpass biblioteksida», som også opnar ved fokus. På telefon
  og nettbrett står han att for langt trykk og musepeikar.

Namna er framleis `contentDescription` på kontrollane, så TalkBack les dei.

## Bygg og testar

- 1223 einingstestar: 1222 bestått, éin valfri nettverkstest hoppa over. Lint: 0 feil.
- Ny test: `TvNoFloatingLabelsTest` (sidemenyen, avspelaren og biblioteket på TV). Alle tre feila
  med beta18-koden. `WideNavigationSettingsTest` kravde tidlegare at namnet vart vist, og krev no at
  det ikkje blir det.
- Isolert TV 5566: WideNavigationSettingsTest 7/7, TvPlaybackControlsTest 15/15, DesignRefreshUiTest 22/22, TvNavigationIntegrationTest 2/2, TvControlsRegressionTest 6/6, CompactNavigationTest 4/4, PlaybackOptionsUiTest 3/3, JellyfinPlayerUiTest 14/14, TvRefinementUiTest 36/37 (den kjende libraryEditorGivesARemoteItsFirstFocus, same feil som beta17 og beta18).

## Ekte profil

Den lagra TV-profilen 5564 starta heilt (Android klar, nett validert), men emulatoren stoppa
få sekund etter oppstart, to gonger på rad, utan feilmelding i loggen. Den visuelle kontrollen på
den ekte profilen er derfor ikkje gjort i denne runden. Den signerte APK-en er i staden prøvd gjennom
den ekte oppdateringa i appen på isolert TV 5566 (sjå nedanfor). Ingen data på 5564 vart endra.

Ingen førespurnad vart send, og ingen avspeling starta på den ekte kontoen.

## Publisering og oppdatering

- Release `v1.0.0-beta19` publisert 25. oktober 2026 som prerelease med éin APK
  (28 042 756 byte, SHA-256 `440bfb7dcb5d1fefa6bb5380ff3ad9d4960ee35dd62d62afedf963b7ea5564d7`),
  mapping, `SOURCE_COMMIT.txt`, `SHA256SUMS.txt` og dei to kjeldearkiva (FFmpeg og native
  undertekstbibliotek, uendra frå beta18). Alle seks digestane på GitHub var like dei lokale filene,
  og den offentlege nedlastinga gav same SHA-256 som det bygde APK-et. Sertifikat `36fa94f0…1a10`;
  dei 20 native biblioteka er byte for byte like beta18.
- Oppdatering i appen på isolert TV 5566: offisiell beta18 (155) viste «Spole v1.0.0-beta19 er
  klar». «Sjå oppdatering» → «Last ned oppdatering» → «Installer oppdatering» → Android «Update» →
  «App installed.». Appen opna som 1.0.0-beta19 (156) med data frå før.
- CI «Bygg og test» for `88fac15`: grøn.
