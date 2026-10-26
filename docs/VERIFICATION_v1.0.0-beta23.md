# Verifisering · Spole 1.0.0-beta23

26. oktober 2026 · `versionCode=160` · produksjonspakke `app.reelstack`.

## Endringar

- Første offentlege utgåve med den lokalt utvikla Stova-lenkjekoplinga. Streng URI-validering, konto- og barnemodusgrenser og varm/kald opning frå beta21/beta22 er med.
- TV-film med medverkande brukar ikkje lenger 76% av høgda som tom hero. Heile portrett, namn og rolle skal vere synlege; kortbreidda aukar saman med TV-skriftstorleiken.

## Bygg og automatiske kontrollar

Kjeldekontroll og release-bygg vart køyrde frå same revisjon med JDK 21 og
Gradle-arbeidsmappa `C:/JellyBin/.gradle-home`.

| Kontroll | Resultat |
| --- | --- |
| JVM-einingstestar | 1295 bestått, 0 feil, 1 eksisterande LAN-test hoppa over (1296 totalt) |
| Android-instrumentering, mobil 5562 | 516 totalt: 452 bestått, 9 feila, 55 hoppa over |
| Android-instrumentering, TV 5566 | Målretta detalj-/kunstverkstest: 39/40 bestått; éin kjent fokusfeil |
| Robolectric-TV-layout | `TvDetailsRobolectricTest`: 2/2 bestått, også med 2× skriftstorleik |
| Lint | 0 feil, 212 åtvaringar, 2 hint |
| Debug, Android-test og signert release | `BUILD SUCCESSFUL` på 6 min 38 s |
| Native undertekstkontroll | Fire ABI-ar, 16 KB-justering for 64-bit og åtte lisensmerknader stadfesta |
| SHA-256 for signeringssertifikat | `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` |

Mobilinstrumenteringa køyrde heile 516-testpakken på `Spole_Instrumentation`.
Dei to `OfflinePlaybackIntegrationTest`-feila rapporterte `STORAGE_LIMIT`: AVD-en
hadde om lag 440 MB ledig, under den eksisterande 512 MiB-reserven. Dei sju andre
feila kom frå `LanguagePickerLayoutTest`, `ReelstackSmokeTest` (2), `ReleaseRowsTest`,
`SmartShelvesTest`, `SteadyRemoteRowsTest` og `TvLibraryRegressionTest`. Dette er
ikkje førte som bestått; fleire av desse klassane har kjende TV-/layoutavvik i eldre
releaseverifikasjonar. Dei nye detalj- og casttestane bestod i den målretta TV-køyringa.
Der feila `TvRefinementUiTest.libraryEditorGivesARemoteItsFirstFocus`, den same
kjende fjernkontroll-fokusfeilen som er dokumentert for tidlegare betaer.

## Release-APK

Den signerte universal-APK-en er 28 060 364 byte, SHA-256
`3af39ccc630d6946e56ad3eee227fa7509711a7b02c3b1964377f700c1daac54`.
Pakken er `app.reelstack`, versjon `1.0.0-beta23`, kode 160, minSdk 26; ho er
ikkje debug-flagga. Same signeringssertifikat som tidlegare Spole-utgåver.

Mappinga er 100 197 653 byte. FFmpeg-kjeldearkivet er 30 790 595 byte,
SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
libass-kjeldearkivet er 50 060 685 byte, SHA-256
`63d16b20ba39a14789633abf372eed82fa0061c8c974e164c6fe8cbd86efd935`.
Desse ligg saman med APK-en og `SOURCE_COMMIT.txt` i den lokale release-mappa;
hashlista blir kontrollert før opplasting.

## Oppdatering og kontodata

Beta23 vart publisert 26. oktober som GitHub prerelease:
[v1.0.0-beta23](https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta23).
Den offentlege, uautentiserte API-en viser `draft=false` og `prerelease=true`.
Nedlasta APK er 28 060 364 byte og har same SHA-256 som byggfila og GitHub sin
asset-digest: `3af39ccc630d6946e56ad3eee227fa7509711a7b02c3b1964377f700c1daac54`.

Den signerte beta23-APK-en vart først installert med `adb install -r` på den
lagra mobilprofilen `Spole_Review` / 5560. Ho viser `versionCode=160` og
`versionName=1.0.0-beta23`; `firstInstallTime` er uendra frå 15. september 2026.
Startskjermen viste Jellyfin-innhald utan innloggingsskjerm.

På den lagra TV-profilen `Spole_GoogleTV_Test` / 5564 stod beta18/155 før prøva.
Updateren hadde først ei lagra beta20-oppføring; «Check now» henta så den ferske
beta23-utgåva og release-notatet frå GitHub. «Download update» fullførte, Spole
kontrollerte fila, og Android viste den vanlege installasjonsdialogen. Etter
«Update» viste Android «App installed.». Pakken er no beta23/160.
`firstInstallTime` står framleis på 9. september 2026. Etter «Open» viste Spole
framleis Jellyfin-innhald og ikkje innloggingsskjermen. Ingen konto eller appdata
vart sletta.

På same TV-profil opna eg filmdetaljen «One Battle After Another» frå Jellyfin.
Ved 1920 × 1080 er portretta, namna og rollene for korta i visningsfeltet synlege
heilt ned. Resten av rollebesetninga ligg vidare i det vassrette rullefeltet.
`DPAD_DOWN` flytta fokus frå «Resume» til det første personkortet utan vertikal
avskjering. Ingen avspeling vart starta.
Kontrollbiletet er berre lagra lokalt i den ignorerte byggmappa:
`app/build/emulators/spole-beta23-cast-focus.png`.

Stova-koplinga vart fysisk prøvd på Shield med beta22: varmt og kaldt korttrykk opna rett Jellyfin-detalj, og Back gjekk til Spole Heim. Denne rapporten dokumenterer beta23-publiseringa og den verkelege oppdateringa på TV-profilen.
