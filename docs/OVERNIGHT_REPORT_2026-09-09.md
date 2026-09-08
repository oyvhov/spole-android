# Spole — nattrapport 9. september 2026

## Resultat

Dette er ei implementeringsrunde, ikkje berre eit nytt veikart. **0.16.0-alpha01 / bygg 44** er bygd som ein lokal, signert førehandsversjon. Han inneheld språkgrunnlag, eit verkeleg språkval og vidareutvikla nettbrettoppsett. Ingen ny backend, tenarkonfigurasjon eller publisering er innført.

**Full engelsk støtte og full nettbrett-/TV-støtte er ikkje ferdige.** Alpha-namnet er medvite: denne bolken gjer framsteg på fleire roadmap-punkt, men godkjenner ikkje heile 0.16 eller 0.18.

## Kva brukaren får

### Språkval som faktisk verkar

- Ny språkveljar i Innstillingar: Følg eininga, Norsk nynorsk og English · Førehandsvising.
- Eksisterande installasjonar held fram på nynorsk. Dette gjeld òg dei som berre har lagra tilkoplingar, utan andre appval.
- Nye installasjonar følgjer eininga for dei omsette tekstane. Eit eksplisitt språkval blir lagra lokalt og endrar ikkje kontoen eller metadata frå tenestene.
- Hovudnavigasjon, onboarding, sentrale tekstar på Heim/Oppdag/Aktivitet/Innstillingar, widget og avspelingskontrollar bruker Android-språkressursar.
- Lyd-, tekst- og kvalitetsmenyar i spelaren bruker stabile interne val. Omsett tekst avgjer ikkje lenger kva menyhandling som blir utført.
- Engelsk er tydeleg merkt som førehandsvising. Det står att nynorske detalj-/kontotekstar, førespurnadsstatusar, nettverksfeil, varsel og datoformat.

### Betre bruk av nettbrettplassen

- Heim og Oppdag kan bruke opptil 1120 dp innhaldsbreidd når vindauget har plass. Fleire titlar blir synlege utan at kvar plakat blir strekt.
- Aktivitet og Innstillingar held seg til ei lesbar hovudkolonne på opptil 840 dp.
- Breie vindauge får eit sentrert detaljpanel på 720 dp. Telefonen held fram med botnpanel.
- Navigasjon og panel følgjer faktisk vindaugsstorleik, ikkje namnet på eininga.
- Sein tekst eller bilete skal ikkje endre den ytre panelhøgda. Tastatur og reelle vindaugsendringar kan framleis krevje tilpassing.

### Eit grunnlag som er lettare å vidareutvikle

- Nye einingstestar for språkval, migrering, manglande ressursar, parameter, fleirtal og vindaugsreglar.
- Nye Android-testar for språkbyte, skjermgjenskaping, stor skrift, breiddeskifte, sein metadata og lukkeknapp med tastatur.
- [Språkrettleiing](LOCALIZATION.md) med ordliste og framgangsmåte for nye språk.
- [Veikart](../ROADMAP.md), [testmatrise](QA_MATRIX.md), layout- og designrettleiing er oppdaterte med delarbeidet. Fullkrav står opne når dei ikkje er oppfylte.

## Verifisering

Stadfesta i denne runden:

- **269/269 einingstestar**, køyrde på nytt utan gjenbruk av testresultata.
- **138/138 Android-testar** på den isolerte telefonemulatoren, 241,771 sekund. Dette omfattar eksisterande innloggings-, tilgangs-, førespurnads-, popup- og spelarregresjonar, inkludert faktisk avspeling av testvideo.
- **46/46 ekstra Android-testar** i eit faktisk breitt emulatorvindauge på 1600 × 1000, 160 dpi, 77,040 sekund. Panelgeometri, sein metadata, tastatur, stor tekst, språkbyte, kalender, Heim, tilgangsvising og spelargrensesnitt er med. Dette er ein endra vindaugsprofil på den isolerte emulatoren, ikkje eit fysisk nettbrett eller ei eiga TV-prøve.
- Heim og eit sentrert detaljpanel vart òg visuelt kontrollerte på brei skjerm med syntetiske data. Sidenavigasjonen og lukkeknappen var synlege, og panelet heldt seg innanfor skjermen. Skjermbileta ligg lokalt i ignorert byggmappe.
- **0 lint-feil, 28 åtvaringar**. Ingen nye undertrykkingar for å få grønt resultat.
- Debug-, test-, signert release-APK og release-AAB byggjer.
- APK-pakke `app.reelstack`, versjon `0.16.0-alpha01`, bygg `44`, minimum API 26 og mål-API 36.
- APK-signatur og 16 KB ZIP-justering er kontrollerte. Dette erstattar ikkje full Play-/einingsvalidering av AAB-en.
- Éin testfeil undervegs kom av at språkveljartesten fann både teksten i dialogen og den same teksten bak dialogen. Testen er avgrensa til dialogen; ingen produktfeil er skjult eller test deaktivert.

### Ekte kontoar og oppdatering

- Den faste `Spole_Review` på 5560 vart starta med eksisterande data etter at testemulatoren var stoppa. Berre éin emulator køyrde om gongen.
- Pakkeinformasjon stadfesta først **0.15.1 / 43**. Installasjon med `-r` lykkast, og etterpå var **0.16.0-alpha01 / 44** installert. Ingen avinstallasjon eller nullstilling.
- Heim viste det eksisterande profilbiletet og ekte, separate Jellyfin-/Emby-rader. Innstillingar viste **Jellyfin, Emby og Seerr som Tilkopla**.
- Den eldre installasjonen fekk nynorsk automatisk. Engelsk vart deretter valt i grensesnittet, appen vart avslutta heilt og starta att, og engelsk navigasjon og søk var framleis valde.
- Nynorsk vart sett tilbake gjennom språkveljaren. Alle tre tenestene stod framleis som tilkopla. Appen vart etterlaten på Heim.
- Radarr og Sonarr var ikkje tilkopla på denne review-installasjonen. Eg har ikkje hevda ny ekte kalenderverifisering.
- Ingen passord, kontoar eller bibliotek vart endra. Ingen verkeleg førespurnad eller videostraum vart starta i denne kontrollen. Skjermbilete med ekte data er berre lagra lokalt i ignorert byggmappe.

## Lokal pakke — ikkje publisert

Filene ligg i `app/build/preview-v0.16.0-alpha01/` og er ikkje sjekka inn i Git:

| Fil | Byte | SHA-256 |
| --- | ---: | --- |
| Spole-v0.16.0-alpha01.apk | 3552841 | `698af1c26e1b001c521688268f57f31dcc5ad7ac4505faad1e34611f881b8cb7` |
| Spole-v0.16.0-alpha01.aab | 8124766 | `9d5a59d43c2c148e83bc74589f692ce823977a70a456841ca8d4b17afd097d3e` |

R8-mapping er kopiert saman med pakken. Signeringssertifikatet er det eksisterande: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.

APK-en er laga frå arbeidsendringane over baseline `265321c`; han skal ikkje omtalast som bygd frå ein seinare dokumentasjonscommit. Dette er ein lokal preview, ikkje ein ny GitHub Release eller Play-kandidat.

### Reproduserbare testbevis

Lokale byggfiler under `app/build/overnight-2026-09-09/`:

- `jvm-build.txt`: ny køyring av einingstestar med `cleanTestDebugUnitTest testDebugUnitTest --no-build-cache`.
- `final-android-tests.txt`: full telefonrunde, sluttlinje `OK (138 tests)`.
- `tablet-android-tests.txt`: ekstra breiddetestar, sluttlinje `OK (46 tests)`.
- `tablet-home.png` og `tablet-details.png`: visuell kontroll med syntetisk innhald.
- `review-home.png` og `review-settings.png`: privat lokal oppdateringskontroll; skal ikkje publiserast eller sjekkast inn i Git.

Android-testane brukar `app.reelstack.debug.test/app.reelstack.SpoleTestRunner` på **5562**, aldri på den ekte review-installasjonen. Vindaugsstorleik og tettleik vart tilbakestilte etter breiddetesten. Einingstest-XML ligg under `app/build/test-results/testDebugUnitTest/`, lint under `app/build/reports/`.

## Kva som står att — og kvifor

1. **Fullføre engelsk:** flytte dei attståande apptekstane og gjere datoar, varsel, feil og detaljflytar språkrette. Så teste heile førespurnads- og avspelingsflyten på begge språk. Android sitt systemstyrte appspråkval er ikkje kopla på enno.
2. **Fysisk kvalitet:** lange avspelingar, nettbyte, kaldstart og første popup-opning må målast på telefon. Pixel 9 Pro XL og truleg OnePlus er registrerte som brukartesta, ikkje som nye fullførte testar av denne alphaen.
3. **Nettbrett:** fysisk eining, delt skjerm, rotasjon, eksternt tastatur og mus må gjennom heile kjerneflyten før støtte blir marknadsført som ferdig.
4. **Ekte brukarrettar og førespurnader:** admin/vanleg brukar og ein eigargodkjend tittel frå førespurnad til bibliotekvarsel. Eg har ikkje starta verkelege nedlastingar som test.
5. **Android TV:** eiga fjernkontrollflate, fokusflyt, innlogging, TV-startpunkt og spelarprøver står framleis i den avtalte TV-milepålen. Ingen TV-støtte er lagt til eller påstått i denne alphaen.
6. **Play:** kontoeigar, signeringsplan mellom kanalane, offentleg personvernside, testløp og butikkressursar må avklarast. Ein grønn lokal testpakke er ikkje i seg sjølv godkjenning for Play.

## Morgonrapport

Ein eingongsrapport er sett opp til **9. september kl. 07.00 norsk tid** i denne oppgåva. Han skal lese denne fila og den dåverande prosjektstatusen. Den lokale maskina må vere tilgjengeleg for at den planlagde køyringa skal kunne gå. Rapporten skal ikkje publisere eller endre tenestedata.
