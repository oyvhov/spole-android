# Verifisering av Spole 1.0.0-beta37

7. november 2026. Produksjonspakke `app.reelstack`, versjonskode 174. Bygd oppå beta35 (`5330e44`) og det
lokale beta36-bygget (`4e992d5`, kode 173, berre på Shield).

## Omfang

Rettingane etter UI-gjennomgangen på Shield
([2026-11-07-ui-shield-apple-google.md](verification/2026-11-07-ui-shield-apple-google.md)):

- Heim på TV (`HomeScreen.kt`): to tilstandar, overskrifta til rada med fokus på linja `TV_ROW_TITLE_LINE`
  (44 dp), animert med `SpoleMotion.LandMs`. `HoldFeedStill` hindrar fokus i å rulle i tillegg. Helten tonar ut
  med rullinga (`ModulateAlpha`) og byter berre i ro. Tilbake fører til helten først. Mjuk toppkant.
- Heltebytet (`TabletLibraryFeature.kt`): `snap(delayMillis)` for det utgåande biletet. `tween(0, delay)` nådde
  målet med ein gong. Skugge under teksten på TV.
- Fokus (`FocusOutline.kt`): LIFT 1,09× med `liftShadow`.
- Overgangar: `detailBackdropPreviewUrl`, Oppdag-bakgrunn i detaljar, krysstoning ved fanebyte,
  vindaugsbakgrunn med merket, `CastRail` med toning, «Be om» først.
- Kantar: `RowKeyline.kt` (`rowKeylineScroll`, `rowEdgeFade`), sesongfaner, `SafeBottomBringIntoView`,
  `RailScrim`.

Arbeidet vart gjort av fleire: Heim og helten i hovudøkta, fokus, overgangar og kantar av tre parallelle
agentar i eigne arbeidstre.

## Bygg og testar

- Einingstestar på den endelege kjelda: 1 650 køyrde, 0 feil, 1 hoppa over. Lint: 0 feil, 219 åtvaringar. Omsetjingar: 1 695 nøklar OK. Release-samsvar OK. Nye testar: `HomeTvRowLineTest` (3), `HeroCrossfadeSpecTest` (3), `HomeSectionTitleTest` (+1), `LiftFocusIndicationTest`, `SeerrActionOrderTest`, `StartupMarkPlacementTest`, `DetailBackdropPreviewTest`, `RailScrimTest`, `HomeTvShelfKeylineTest`, `TvSeriesSafeAreaTest`. Testane for Heim og heltebytet var raude av rett grunn før rettinga (overskrifta på 266,5 dp, utgåande bilete på 0,0 etter 10 ms).
- Isolert TV 5566: ikkje køyrd. adb vart «unauthorized» i to oppstartar på rad (ny feil), så instrumenteringa kom aldri i gang. I staden prøvd på Loftet (Google TV Streamer, Android 14, ekte Jellyfin-profil, toppmeny, animasjonsfart 1×) med den signerte APK-en lagd inn med `install -r` (første installasjonsdato bevart): helt → rad → rad → opp → helt tek 0,41–0,50 s per steg (før 0,08–0,13 s), og overskrifta landar på same linje utan halve knappar eller rader. Tilbake frå radene gir heil helt med fokus på «Spill av». Heltebytet går jamt frå luma 78 til 61 over om lag 1,1 s utan søkk. Ein tittel opnar med logo og bakgrunn frå første bilete. Radene rullar til venstrelinja utan avkappa kort. Sesongfanene er heile, episodetitlane tonar ut nedst, og kald start viser merket frå første bilete. Sidemeny-skuggen er ikkje sett på eining (Loftet har toppmeny). Shield er ikkje rørd; han kan oppdatere i appen.

## Publisering

- Release-commit `3c122f4`, tagg `v1.0.0-beta37`, pusha med `git push --atomic origin HEAD:main v1.0.0-beta37`
  (spola fram frå `5330e44`).
- Signert APK: 28 157 424 byte, SHA-256 `d08d9ca3…7d48612`, sertifikat `36fa94f0…`, `app.reelstack` 174,
  ikkje debuggable. Dei native undertekstbiblioteka er kontrollerte (fire ABI-ar, 16 KB-justering, åtte
  lisensmerknader), og alle 20 native bibliotek er byte for byte like beta35.
- Seks assets med digest kontrollert mot GitHub: APK, `SHA256SUMS.txt`, mapping, `SOURCE_COMMIT.txt`,
  FFmpeg-kjelda og kjeldearkivet for dei native undertekstbiblioteka. Releasen er publisert som
  prerelease (ikkje `latest`), og den offentlege nedlastinga utan innlogging har same SHA-256 som bygget.
- CI «Bygg og test» på `3c122f4`: grøn ([køyring 42319801777](https://github.com/oyvhov/spole-android/actions/runs/42319801777), knapt seks minutt).

Ingen førespurnad vart send, og ingen avspeling starta på dei ekte kontoane under denne verifiseringa.
