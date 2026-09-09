# Spole — minimerbar sidemeny

9. september 2026 · 0.16.0-alpha04 / 47 · lokal førehandsversjon, ikkje publisert.

## Endringa

- Fullhøgd, samanhengande sidemeny på breie appvindauge, ikkje ein flytande meny.
- 200 dp ikon/tekst eller 80 dp ikonrad, med roleg breiddeovergang og tonande etikettar.
- Same fokusbare knappar og ikonposisjonar i begge variantar. Aktiv side blir ikkje bytt ved minimering.
- Innhaldssida skiftar breidd éin gong; sidemenyen animerer over henne. Dette unngår kontinuerleg ny måling av bilete og rutenett.
- Lokal lagring av menyval. Mobilvindauge bevarer valet til siderada blir synleg igjen.
- Nynorsk/engelsk namn på veksleknappen og eksplisitte tilgjengelegheitsnamn på ikonrada.
- Ingen endring av tenester, rettar, innlogging eller avspeling. Dette er TV-førebuing, ikkje ferdig Android TV-støtte.

## Verifisering

- [x] Første bygg: 279/279 einingstestar, lint 0 feil / 28 eksisterande åtvaringar, 9/9 målretta nettbrett-testar og full mobilrunde 161/161. Loggar: `build.txt`, `tablet.txt`, `full.txt`.
- [x] Sluttbygg etter stabil innhaldsbreidd: 279/279 einingstestar, lint 0 feil / 28 eksisterande åtvaringar og 10/10 nettbrett-testar. Loggar: `stable-build.txt`, `stable-tablet.txt`.
- [x] Sluttrunde mobil: 21/21 målretta testar av navigering, hovudflyt, UI-konsistens, heimeside og adaptiv layout. Logg: `stable-mobile.txt`.
- [x] Release-APK installert over eksisterande review-app med ekte kontoar; begge breidder, gjenteken veksling og lagra minimert meny etter omstart er visuelt kontrollerte. Skjermbilete: `collapsed.png`, `expanded.png`.
- [ ] Fysisk Android TV/fjernkontroll og Galaxy Tab S7+ er ikkje testa her.

Testloggar og eventuelle lokale skjermbilete ligg i `app/build/sidebar-alpha04/`, ikkje i Git. Eksisterande brukarendringar i andre dokument skal bevarast.

Under live-kontrollen krasja emulatorens native `qemu-system-x86_64`/RenderThread med SIGSEGV to gonger. Dette var ikkje ein dokumentert Android-appkrasj. Den same review-AVD-en vart starta på nytt utan sletting, og APK-en reinstallert over bevarte kontodata. Sluttrunda brukar også ei lettare innhaldsoppdatering: berre sidemenyen skiftar breidd under animasjonen. Den endelege kontrollen lukkast utan emulatorvindauge og med Vulkan deaktivert; dette dokumenterer ikkje at kodeendringa åleine løyste vertsfeilen. Fysisk yting blir ikkje godkjend ut frå emulatoren.

## Lokal APK

`app/build/sidebar-alpha04/Spole-0.16.0-alpha04.apk` · 3 646 945 byte · pakke `app.reelstack` · bygg 47. R8-mapping er bevart i same mappe. SHA-256: `52B2B6173DAFBE01CA95716A12DC50E1C77C55C67BA1353847613E67AFCCF96D`. Eksisterande signeringssertifikat er verifisert: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`. Ingen GitHub-release i denne oppgåva.
