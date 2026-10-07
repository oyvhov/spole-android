# Verifisering av Spole 1.0.0-beta5

7. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 142. Under sluttkontroll.

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
| APK SHA-256 | `2131fbe2396da14e8b29829fca1907ee013fff7e714426b26106d22430870423` |
| Signatur SHA-256 | `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` |
| Android | minimum 26, target 36; ikkje debuggable |
| FFmpeg-arkiv | 30 790 595 byte; SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21` |
| Lokal arkivering | `app/build/release-v1.0.0-beta5/`, med tilhøyrande R8-mapping |

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

Offentleg digest og oppdatering gjennom appen blir førte her etter publisering.

## Avgrensingar

- Ingen fysisk mobil eller TV er tilkopla denne arbeidsstasjonen. Emulatorprøvene er ikkje fysisk kompatibilitetsprøving.
- Ekte vaksenpassord er ikkje brukt til PIN-gjenoppretting. Kodeflyten er gjennomgått; reell passordprøve står att.
- Ingen ny stabil 1.0 blir publisert med denne betaen. Lange nettbrot og dagleg offlinebruk treng vidare prøving.
