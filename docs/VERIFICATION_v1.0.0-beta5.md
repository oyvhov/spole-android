# Verifisering av Spole 1.0.0-beta5

7. oktober 2026. Publisert beta, produksjonspakke `app.reelstack`, versjonskode 142.

## Kontrollen av kodeendringane

- 903 einingstestar: 902 beståtte, éin hoppa over, null feil.
- Lint: null feil, 185 åtvaringar og eitt tips.
- Utvalde instrumenterte prøver: 42 mobiltestar og 16 TV-testar beståtte etter målretta omprøver. To oppstartstestar på mobil består òg. Instrumentering berre på isolerte 5562/5566.
- Testfixturar og forventningar er retta for den personlege kalenderen frå beta3, rulling på telefon og «Spole» som tom spelartittel. Ingen av dei to kontobytteprøvene er tekne bort.
- Ekte HTTP-prøver dekkjer token i header, avviste omdirigeringar og socket-fallback. Profil-/PIN-prøver og forseinka svar dekkjer kontoavgrensinga.
- Lokal kandidat vart signert med eksisterande nøkkel og installert over beta4 på review-TV 5564. Kontoar, nynorsk og ekte Jellyfin-/Emby-rader vart bevarte. Kort navigasjon ved 192 MiB heap gav ikkje nytt minnekrasj.
- Native-biblioteka er uendra, med fire ABI-ar og 16 KiB `LOAD`-justering. FFmpeg-kjelder og relenkingsarkiv blir lagde ved releasen.

Detaljerte endringar og prov ligg i [status for stabil 1.0](STABLE_1_0_PROGRESS.md).

## Signert beta5 og sluttprøver

Sluttbygget gav `BUILD SUCCESSFUL` for einingstestar, lint, debug, test-APK og signert release.
Test-APK-en vart bygd på nytt for offline-integrasjonsprøva; produksjonskjelda er uendra.

| Felt | Verdi |
| --- | --- |
| Pakke / versjon | `app.reelstack` / `1.0.0-beta5` / kode `142` |
| APK-storleik | 11 564 130 byte |
| Publisert APK SHA-256 | `c59750065c062ab8299bb78b633f12d6c066fae39ba38e87a427a0f4f21742cc` |
| Kjeldecommit | `5e17951fdbfdd55fba4e2ee123a4dc33bf3fec68` |
| Signatur SHA-256 | `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` |
| Android | minimum 26, target 36; ikkje debuggable |
| FFmpeg-arkiv | 30 790 595 byte; SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21` |
| Lokal arkivering | `app/build/release-v1.0.0-beta5/`, med tilhøyrande R8-mapping |

Slutt-APK-en vart pakka etter release-commit fordi Android-byggverktøyet legg commit-ID-en i
`META-INF/version-control-info.textproto`. Samanlikning av alt upakka innhald stadfesta at berre
denne metadatafila endra seg frå kandidaten før commit; koden, ressursane og R8-mappinga er like.
Bygget av den frosne kjelda gav `BUILD SUCCESSFUL`. Digest og kjeldecommit ligg i dei publiserte
`SHA256SUMS.txt` og `SOURCE_COMMIT.txt`. Taggen blir ikkje flytta ved denne rapportoppdateringa.

Den signerte beta5 vart installert med `-r` over kandidaten på review-TV 5564. Kontoar og val
vart bevarte. Ekte Jellyfin- og Emby-filer viste 1080p AVC-bilete og EAC3 5.1 gjennom den
bundla FFmpeg-dekodaren. Dei korte statistikkprøvene viste null lydavbrot og null droppa bilete.
Jellyfin sitt SUBRIP-spor vart valt; rendering vart ikkje stadfesta der. Ei undertekstlinje var
synleg i Emby-prøva. Emulatoren køyrer utan høyrbar lyd, så dette er dekodar-/biletprov.

Første offline-integrasjonsprøve vart avvist med `STORAGE_LIMIT`: den isolerte telefonen hadde
386 MiB ledig, under reserven på 512 MiB. Grensa er bevara. Etter frigjering av kompilerte
systemcachar bestod den same prøva, `OK (1 test)` på 12,898 sekund. Ho dekkjer autentisering
i header, delvis nedlasting, pause/gjenopptaking med HTTP Range, byte-identisk lokal cache
etter stengd testtenar, framdrift i den ekte offline-spelaren, tilbakekalling etter profilbyte
bort og tilbake og avvising av fila etter kontobyte. Konto-/appdata på review-einingane er
ikkje rørte. Isolert telefon vart starta med ein bevart diskkopi; inga nullstilling var nødvendig.

## Publisering og oppdatering

[GitHub Release](https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta5) er publisert
med `draft=false`, `prerelease=true` og `latest=false`. Fem assets vart kontrollerte mot lokale
byte-storleikar og GitHub sine SHA-256-digestar: éin universal release-APK, R8-mapping, FFmpeg-
kjelde-/relenkingsarkiv, `SHA256SUMS.txt` og `SOURCE_COMMIT.txt`.

Release-metadata og
[APK-en](https://github.com/oyvhov/spole-android/releases/download/v1.0.0-beta5/Spole-v1.0.0-beta5.apk)
vart henta utan Authorization-header. Den offentlege kontrollfila har same SHA-256 som den
lokale, signerte APK-en. Den annoterte taggen peikar på kjeldecommiten over.

På den eksisterande review-mobilen 5560 vart førre offentlege beta4 / kode 141 oppdatert gjennom
**Settings → Notifications and updates → App updates → Check now → Download update → Install
update** og Android sin installasjonsdialog. Appen fann beta5, lasta ned og kontrollerte fila,
og Android stadfesta «App installed». Installert versjon er beta5 / kode 142. Appen vart opna
att utan førstegongsoppsett eller nytt `AndroidRuntime`-krasj; engelsk og eksisterande demoval
vart bevarte. Denne mobilen var allereie i eksplisitt demo før prøva, så han stadfestar
oppdateringsflyten og lokale val, medan TV-prøva stadfestar bevaring av ekte kontoar.

Play Protect viste ein generell «App scan recommended»-dialog for den ukjende sideloada APK-en.
Det synlege valet «Install without scanning» vart brukt for den verifiserte, signerte fila;
Play Protect vart ikkje deaktivert. Det vart ikkje brukt `adb install` på denne mobilen.

GitHub-køyringa for kjeldecommiten består:
[Bygg og test](https://github.com/oyvhov/spole-android/actions/runs/37661516205), `completed/success`
for bygg, lint og einingstestar. Det vart starta éi branch-køyring; taggen starta ikkje eit ekstra
bygg. Instrumentering vart hoppa over som venta på push. Begge Jev-stega vart hoppa over;
denne releasen brukte ingen Jev-API-kall. Dei 28 lokale Jev-hjelpetestane brukte mockar.

## Avgrensingar

- Ingen fysisk mobil eller TV er tilkopla denne arbeidsstasjonen. Emulatorprøvene er ikkje fysisk kompatibilitetsprøving.
- Ekte vaksenpassord er ikkje brukt til PIN-gjenoppretting. Kodeflyten er gjennomgått; reell passordprøve står att.
- Ingen ny stabil 1.0 blir publisert med denne betaen. Lange nettbrot og dagleg offlinebruk treng vidare prøving.
