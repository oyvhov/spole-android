# Verifikasjon — Spole 0.16.0-alpha14

Dato: 10. september 2026. Pakke `app.reelstack`, versjonskode 57.

## Bygg og automatiske testar

- Endeleg bygg: `app/build/alpha14-build.log`, BUILD SUCCESSFUL, 137 oppgåver (68 køyrde, 69 uendra). JVM-testar, debug/test-APK, debug-lint og signert release vart køyrde samla.
- 344 JVM-testar, 0 feil. Nye testar dekkjer avgrensa automatisk gjeninnlasting og at manglande identitet gir ei åtvaring utan å hente andre brukarar sitt innhald.
- Lint: 0 feil, 55 åtvaringar. Eit tidleg mellombygg fekk ein intern lint-feil medan kjelda vart redigert; det endelege bygget køyrde på uendra kjelde og bestod.
- Isolert telefon, port 5562: 8 bestått, 1 TV-test korrekt hoppa over. Runner rapporterer OK (9 tests), 41,61 sekund. Dei fire nye testane dekkjer TV-fokus utan endra tekstpikslar, mobilkant, siste kort, dobbel skriftstorleik og feilforklaring. Dei eksisterande testane dekkjer bibliotekval, filter og oppdateringspanel.
- Isolert Android TV, port 5566: 6 av 6 bestått, 15,48 sekund. Inkluderer pikselkontroll av tekst ved fokus, alle avrundingane og tidleg fokusert førespurnadsknapp.
- Testane køyrde i same WSL-prosess som starta emulatoren; begge køyringane fullførte. Ingen instrumentering vart køyrd på dei innlogga profilane.

## APK

- `Spole-v0.16.0-alpha14.apk`, 3 904 713 byte.
- SHA-256: `93de79819604a33672ecf039225404404d927d517c1f8eafa6cef0c1d1fca9f8`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`, verifisert med apksigner.
- versionName `0.16.0-alpha14`, versionCode 57, minSdk 26, targetSdk 36, ikkje debuggable.
- R8-mapping og kontrollsum arkiverte i `app/build/release-v0.16.0-alpha14`.

## Publisering

Annotert tag `v0.16.0-alpha14` og release-kjelda `fccdcb5` vart pusha atomisk. GitHub-releasen er offentleg, publisert og markert som testutgåve. Ho inneheld éin universal-APK, R8-mapping, SHA256SUMS og SOURCE_COMMIT. GitHub-digest og byte-storleik vart kontrollerte før publisering. Etterpå vart både den offentlege nedlastingslenkja og API-asset-endepunktet som appen brukar lasta ned utan autentisering; begge kontrollsummane samsvarar med den signerte lokale APK-en.

## Visuell kontroll og grenser

Den lagra mobilprofilen `Spole_Review` (5560) vart oppgradert med signert `install -r` frå alpha12 / kode 55 til alpha14. Installeringa returnerte Success. Etter oppstart viste den eksisterande kontoen ekte titlar i både «Hald fram å sjå» og «Neste episode», utan ny innlogging. Dette stadfestar at konto/data vart bevarte og at produksjonsbygget hentar innhaldet. Det er ikkje ei kunstig injisert nettverksfeil eller ein test av GitHub-installasjonsdialogen.

WSL stoppa under den vidare ventinga etter den første UI-kontrollen. Den planlagde ekstra kontrollen etter 80 sekund og skjermbiletet vart ikkje fullførte. Dei to isolerte instrumenteringskøyringane over fullførte før dette.

TV-banneret vart rendra lokalt for å kontrollere den nye S-konturen. Mobilradene er framleis horisontalt sveipbare: eit neste kort kan vere delvis synleg ved skjermkanten, men ikkje klipt ved ein innvendig høgremarg.

Fysiske TV-ar og alle nettverkstilstandar er ikkje dekte av emulatorane. Automatisk retry kontrollerer ikkje brukaren sine tenarar; feil adresse, utgått innlogging eller manglande tilgang må framleis rettast i innstillingane. Ei vellukka tom sjåvidare-liste blir ikkje fylt med kunstige titlar.
