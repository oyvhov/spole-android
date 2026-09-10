# Verifikasjon — Spole 0.16.0-alpha13

Dato: 10. september 2026. Produksjonspakke `app.reelstack`, versjonskode 56, Android API 26–36.

## Omfang

Denne releasen samlar alpha12-oppdateraren og bibliotekarbeidet, dei etterfølgjande tema-/typografi- og TV-endringane, og oppstartsskriptet for lagra emulatorprofilar. [Release-notata](release-v0.16.0-alpha13.md) skildrar brukarendringane. [Release-flyten](RELEASE_WORKFLOW.md) er dokumentert for neste publisering.

Ved gjennomgangen vart ei uriktig annonsering av mottak for Jellyfin-fjernkommandoar funnen. Appen har ingen slik mottakar. `SupportsMediaControl` er derfor false og kommandolista tom; videostøtte blir framleis registrert. Ein regresjonstest kontrollerer dette etter verifisering av avspelingskontoen.

## Bygg

Første forsøk feila i KSP på absolutte Linux-stiar frå eit tidlegare bygg. Ingen kjelde- eller brukardata vart sletta. Det endelege bygget køyrer alle oppgåver på nytt utan byggcache og med KSP-inkrementell behandling av, slik release-guiden dokumenterer. Eit mellomforsøk hadde ein feil sitert PowerShell-parameter og køyrde ikkje byggoppgåvene.

Endeleg bygg (`app/build/alpha13-release-build3.log`) bestod alle 137 byggoppgåvene på nytt på 3 min 40 sek. 341 JVM-testar er bestått, utan feil. Lint: 0 feil og 55 åtvaringar. Debug-app, instrumenterings-APK og signert/minifisert release-APK vart bygde i same køyring. Kjelde som vart kompilert vart kontrollert for samtidige endringar; ingen kjeldefiler vart endra medan bygget køyrde.

## APK-identitet

- Fil: `Spole-v0.16.0-alpha13.apk`, 3 904 141 byte.
- SHA-256: `e32d5ec13166523a9600813a5af26c208526fe8a6c9c24c51a84fa23646e16ac`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`, stadfesta med Android apksigner.
- Pakke `app.reelstack`, versionName `0.16.0-alpha13`, versionCode 56, minSdk 26, targetSdk 36, ikkje debuggable.
- Leanback-startpunkt/banner finst i den pakka APK-en. R8-mapping og kontrollsum er arkiverte saman med APK-en i `app/build/release-v0.16.0-alpha13`.

## Android-kontroll

Siste TV-køyring fullførte 29 av 35 testar utan rapporterte testfeil før WSL stoppa adb og emulatoren. Desse dekkjer mellom anna bibliotekval/filter, fokusrammer, TV-detaljar, spelarkontrollar, innstillingar, oppstartsanimasjon og sidemenynavigasjon. Køyringa er **ikkje** rekna som ein fullstendig bestått testsuite. Logg: `app/build/alpha13-tv-tests4.txt`.

Fleire nye forsøk med éin emulator om gongen og separat adb-prosess vart òg avbrotne. Mobilens fulle instrumenteringssuite vart ikkje fullført. Til slutt returnerte sjølv eit enkelt WSL-kall `Wsl/Service/E_UNEXPECTED` («Catastrophic failure»). Det hindra også den planlagde produksjonsoppgraderinga på dei lagra profilane. Ingen appdata vart sletta. Dette er ei eksplisitt verifikasjonsgrense for testutgåva.

## Oppdatering

Alpha12 har versjonskode 55 og oppdateringsklienten. Alpha13 har høgare kode 56, same pakkenamn og er signert med den eksisterande nøkkelen. Dei lagra review-profilane vart ikkje medvite oppgraderte, og kan brukast til etterkontroll frå alpha12 når WSL fungerer igjen. Full nedlasting og Android-installasjon gjennom appen er ikkje stadfesta i denne runden. Ein lokal `adb install -r` ville heller ikkje åleine ha bevist GitHub-flyten.

Automatisk sjekk brukar tolv timars mellomrom; Sjekk no går utanom dette. Alpha13 er prerelease, slik at Testutgåver må vere på. Android krev stadfesting av installasjonen.

## Publisert artefakt

Release-taggen `v0.16.0-alpha13` peikar på commit `502757d48d505587aaf894cdd407e38425cab4ef`. Grein og annotert tag vart pusha atomisk. GitHub-releasen er publisert, offentleg og markert som testutgåve; den stabile «latest»-utgåva er ikkje erstatta.

Før publisering vart APK-storleik, uploaded-status og GitHub sin SHA-256-digest kontrollerte i kladden. Etter publisering vart release-lista henta utan autentisering. Ho inneheld alpha13 med éin APK og rett digest. Både den offentlege nedlastingslenkja og API-asset-endepunktet som appen brukar (`Accept: application/octet-stream`) vart lasta ned utan autentisering. Begge filene har nøyaktig same SHA-256 som den lokalt signerte APK-en. R8-mapping, kontrollsum og kjeldecommit er òg publiserte assets.

Dette stadfestar den offentlege distribusjonen som oppdateraren treng. Android sin installasjonsdialog og bevaring av kontoar under sjølve oppgraderinga er framleis ikkje testa i denne runden på grunn av WSL-feilen over.

Eit siste forsøk etter publisering stadfesta at oppstartsskriptet kan starte den lagra mobilprofilen, vente på validert nettverk og opne `app.reelstack`. Pakka på profilen vart avlesen som alpha12 / kode 55. WSL/adb stoppa igjen før oppdateringsmenyen kunne kontrollerast. Oppstartsskriptet fekk òg ei retting slik at WSL-flagg ikkje blir unødvendig siterte ved bakgrunnsoppstart; den retta oppstarten fullførte.

## Avgrensingar

Emulatorresultat dekkjer ikkje alle fysiske TV-ar, fjernkontrollar eller maskinvarekodekar. Mottak av «spel på»-kommandoar er ikkje levert. Automatisk gjenoppretting av tomme rader akkurat under ein kald emulator-nettverksovergang er ikkje stadfesta som løyst; kontrollerte profilar blir starta etter at nettverket er klart.
