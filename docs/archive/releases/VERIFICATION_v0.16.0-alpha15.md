# Verifikasjon — Spole 0.16.0-alpha15

10. september 2026. Pakke `app.reelstack`, versjonskode 58.

- Samla endeleg bygg av JVM-testar, debug/test-APK, lint og signert release bestod (`app/build/alpha15-final-build.log`, 2 min 33 sek). Dette inkluderer førehandlasting av dei tre bileta. Kjelde vart ikkje endra under bygget.
- 346 JVM-testar bestått. Utvalstestar kontrollerer tre ulike seriar, episoderepetisjonar, dublettar på tvers av tenester og at skjulte rader/manglande kunst ikkje blir valde.
- Lint: 0 feil, 57 åtvaringar.
- Android TV, isolert profil 5566: 5 av 5 testar bestått på endeleg bygg (`app/build/alpha15-tv-ui-tests.txt`, 8,516 sekund). Kontrollerer overgang til neste tittel, uendra knapp-posisjon, rett handling etter skifte, pause ved fjernkontrollfokus, kontooverlegg og dobbel skriftstorleik.
- APK: `Spole-v0.16.0-alpha15.apk`, 3 904 921 byte, SHA-256 `6ca7f63eb767905f9dca83d4ae8d4fae8661b8d07daf95855e4cdb1f6d9cc612`.
- Android apksigner stadfestar eksisterande sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Pakka metadata: alpha15 / kode 58, minSdk 26, targetSdk 36, ikkje debuggable. R8-mapping og kontrollsum arkiverte saman med APK-en.

Fysisk TV og alle serverdata er ikkje dekte av dei syntetiske testane. Utvalet er avgrensa til dei seriane den tillatne, innlasta bibliotekrada faktisk inneheld; færre enn tre ulike seriar blir ikkje fylte med dublettar. Rotasjonen gjer ingen nye nettverkskall.

Den lagra TV-profilen 5564 fekk signert `install -r` utan sletting av data. Første forsøk oppgraderte frå alpha12 / kode 55; det endelege forsøket installerte den ferdige alpha15-fila over det lokale mellombygget. Begge installeringane returnerte Success, og appstart returnerte OK. WSL avslutta før den planlagde UI-dumpen og skjermbiletet med ekte data. Kontobevaring og visuell kontroll med ekte data er derfor ikkje stadfesta i denne runden; dei fem fullførte skjermtestane brukar isolerte testdata.

Release `v0.16.0-alpha15` er publisert offentleg som testutgåve, frå release-commit `e1ab1ba`. Tag og grein vart pusha atomisk. APK, SHA256SUMS, R8-mapping og SOURCE_COMMIT er release-assets. GitHub sin digest vart kontrollert før publisering. Etter publisering vart både den offentlege nedlastingslenkja og API-asset-endepunktet til oppdateraren lasta ned utan innlogging; begge filene har same hash som den signerte lokale APK-en.
