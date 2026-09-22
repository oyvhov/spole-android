# Verifikasjon: Spole 0.17.0-beta01

Produksjonspakke `app.reelstack`, versjonskode 80. Beta frå `main`; stabil `latest` skal framleis vere 0.16.1.

## Kjelde og kontrollomfang

Endringane er gjennomgåtte og dokumenterte i [designløftet](DESIGN_REFRESH_2026-09-15.md) og [finpussen](DESIGN_REFINEMENT_2026-09-15.md). Dei historiske lokale APK-hashane der er ikkje hash for denne releasen.

507 einingstestar og siste 49 Android-testar bestod før versjonsauken. Den eigne avspelingsrunden køyrde 39 testar utan feil, med éin telefonspesifikk test hoppa over på TV.

## Release-bygg

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease` avslutta med `BUILD SUCCESSFUL`.
- 507/507 JVM-testar bestod. Lint: 0 feil, 57 åtvaringar.
- 13/13 Android-testar på isolert TV 5566 bestod etter versjonsauken: episodeskifte med ulike fil-ID-ar, tekstklargjering, lokal framdrift, oppstartslogo, episode/serie-retur og mellomlager. Ingen hoppa over i denne runden.
- Produksjonspakke `app.reelstack`, versjon `0.17.0-beta01`, kode 80, minimum Android API 26, ikkje debuggable.
- APK: 11 690 910 byte. SHA-256: `65728b423ca240623268e5985dead19db5337a5a6970239024d4d30842eb735f`.
- Signeringssertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Signert oppgradering med `install -r` på fast mobilprofil 5560 lykkast. Installasjonen rapporterer kode 80 og rett versjon; ingen appdata vart sletta. Ekte innlogging på denne profilen er ikkje nyverifisert.
- Fast TV-profil 5564 er halden på kode 79 for etterfølgjande GitHub-oppdatering gjennom appen.

## Publisering og oppdateringsflyt

- Kjeldecommit og uendra release-tag: `7d74bf7c2f63f17141292c10800cb56f4f1ea80d`.
- GitHub-release `v0.17.0-beta01` er offentleg, ikkje draft, og merkt prerelease. Stabil `latest` er framleis `v0.16.1`.
- Nøyaktig éin APK er lasta opp, saman med `SHA256SUMS.txt`, korrekt R8-mapping og `SOURCE_COMMIT.txt`.
- Release-lista er henta utan Authorization-header. Asset-digest og storleik samsvarer med det lokale bygget.
- APK-en er lasta ned på nytt frå den offentlege release-lenkja. SHA-256 samsvarer med `65728b423ca240623268e5985dead19db5337a5a6970239024d4d30842eb735f`.
- På den faste TV-profilen 5564 med kode 79 vart Testutgåver slått på. Appoppdateringar → Sjekk no fann `v0.17.0-beta01`. Last ned fullførte, og appen viste Installer etter kontroll av pakken.
- Android-løyvet for installasjon frå Spole måtte aktiverast. TV-en hadde både debug- og produksjonsappen med same namn; løyvet vart avgrensa til produksjonspakken med ADB, og debug-pakken vart sett tilbake til standard. Sjølve nedlastinga og pakkeverifikasjonen gjekk gjennom appen, og Android sin Update-dialog vart godkjend i UI.
- Android rapporterte «App installed». Installert pakke er kontrollert som `0.17.0-beta01`, kode 80. Oppstart via Open viste det ekte biblioteket og tidlegare utsjånad/menyval utan ny innlogging. Kontoar og appdata er bevarte.
- Dette er ein vellukka ende-til-ende-test av GitHub-oppdateringa på TV, ikkje berre `adb install -r`. Ingen ny APK eller flytting av taggen vart gjort etter publisering.

Private skjermbilete, kontoar, signeringsfiler, emulator-data og testloggar skal ikkje publiserast. R8-mapping og kjeldecommit blir lagde ved APK-en som eigne release-assets.
