# Verifikasjon — Spole 0.16.0-alpha09

Dato: 10. september 2026. Lokal arbeidskopi; ingen ny GitHub-publisering. [Endringar knytte til alle 13 brukarpunkt](TV_REFINEMENT_ALPHA09.md).

## Produksjons-APK

- Pakke: `app.reelstack`.
- Versjon: `0.16.0-alpha09`, versionCode `52`.
- Signert med den eksisterande Spole-nøkkelen, R8-minifisert og ikkje debuggable.
- Minimum Android API 26; target API 36.
- APK: `app/build/test-alpha09/Spole-0.16.0-alpha09.apk`.
- Storleik: 3 793 241 byte.
- SHA-256: `91883e52f9148994256e37c3cdb9798015dbf0dd6f51c3e8f2ced841357b8b44`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping er teken vare på som `app/build/test-alpha09/mapping-0.16.0-alpha09.txt`.

## Automatiske kontrollar

- `testDebugUnitTest`: **312 testar, 0 feil**. Omfattar katalogtypar, Next Up avgrensa før innlasting, metadata/framdrift/kvalitet, menyreglar og eksisterande tilgangsreglar.
- `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease` og `lintDebug`: bestod. Lint rapporterer **38 åtvaringar, ingen feil**; dette er ikkje ein påstand om eit åtvaringsfritt prosjekt.
- Endeleg byggjelogg: `app/build/tv-refinement-build5.log`.
- APK-identitet, fravær av debuggable-flagg og signatur er kontrollerte med Android-verktøya.
- Telefon: **221 ulike utførte Android-testar bestod**, med atterkøyringa omtalt nedanfor. Fullpakken hadde 228 tilfelle: 220 bestod, éin feila i teststyringa, og sju TV-testar vart hoppa over på telefon. Etter retting av teststyringa bestod heile spelarpakken: 25 utførte testar og éin TV-test hoppa over. Loggar: `app/build/alpha09-full-phone-final.txt` og `app/build/alpha09-player-rerun.txt`.
- Google TV: **15/15 målretta Android-testar bestod** på endeleg produksjonskode (`app/build/alpha09-tv-final-rerun.txt`). Dette omfattar appens oppstart/menyfokus, detaljar, bibliotek, innlogging og faktisk videodekoding med fjernkontroll. Quick Connect og bibliotekdialogen bestod òg med Android si systemskrift sett til 200 %: **2/2** (`app/build/alpha09-tv-system-large.txt`).

## Testisolasjon og avvik

Instrumentering køyrer på eigne telefon-/Google TV-profilar, med syntetiske kontoar og lokale videofiler. Review-profilane 5560 og 5564 har ekte innlogging og får berre signert produksjonsoppdatering; ingen instrumentering, sletting eller reinstallasjon med tap av data.

Den første fullkøyringa på telefon vart avbroten då emulatorprosessen stoppa. Ho er ikkje rekna som bestått. Fullpakken vart deretter køyrd med berre éin emulator aktiv. Eit testtrykk i den nye skjuling/vising-av-Tilbake-testen mangla eksplisitt touchscreen-kjelde, og lesinga brukte ein gammal tilgjengeligheitstre-cache. Teststyringa vart retta; begge spelartestklassane bestod atterkøyringa utan endringar i produksjonskoden. Resultatet er difor ikkje framstilt som éi feilfri fullkøyring.

Ein tidleg TV-runde bestod 13/13 målretta testar. Visuell gjennomgang med ekte data avdekte etterpå automatisk menyopning ved innlasting og filmapper i katalogen. Desse vart retta før den endelege APK-en; den tidlege TV-runden åleine blir difor ikkje brukt til å godkjenne siste navigasjonsendring.

Første runde med den siste TV-pakken gav to testfeil: integrasjonstesten føresette at geometrisk venstrenavigasjon alltid landa på Heim, og spelartesten sende neste Tilbake før skjuling av kontrollane var stadfesta. Testane kontrollerer no faktisk fokus i menyen og ventar på at kontrollane er borte. Heile 15-testpakken bestod atterkøyringa; produksjonskoden vart ikkje endra mellom desse to rundane.

## Signert oppgradering og ekte data

Den arkiverte produksjons-APK-en vart installert som oppdatering på både Google TV (5564) og telefon (5560). Eksisterande Jellyfin-/Seerr-kontoar, språk og visingsval var bevarte. Begge emulatorane er opne for vidare prøving.

Visuell kontroll av endeleg APK:

- TV starta med kollapsa meny etter at ekte data var ferdig lasta. Venstre inn i menyen utvida han; val av Bibliotek kollapsa han og flytta fokus til innhaldet.
- Bibliotekoversikta viste Filmar, Seriar, Barnefilmar og Samlingar med tenaren sine bilete. Filmkatalogen viste lesbare titlar og posterar; fysisk Tilbake returnerte til bibliotekoversikta.
- Taskmaster frå Hald fram å sjå viste Resume med faktisk fokus, 49 % sett, 25 minutt att, 8.5-vurdering og 1080p/H264/AAC 2.0. Ingen TV-knapp for Tilbake eller Detaljar. Denne episoden manglar framleis omtale frå tenaren.
- Aktivitet viste kjende titlar og faktiske statusar. Innstillingar viste bibliotekval og dei nye meny-/innhaldsvala. Brukaren sine val vart ikkje endra under gjennomgangen.
- Telefonen viste mobil botnmeny, Hald fram å sjå, Neste episode og Nye filmar. Bibliotekoversikta tilpassa seg ståande skjerm.

Private skjermbilete ligg lokalt under `app/build/alpha09-final-*`: TV-heim, episode, bibliotek, filmar, aktivitet, innstillingar og tilpassing; telefon-heim og bibliotek. Dei er ikkje publiserte eller lagde i kjeldekontrollen.

## Avgrensingar

Emulatorresultata omfattar ekte Android-videodekoding av syntetisk media, men erstattar ikkje fysisk TV-/nettbrett-test eller 60-minutts avspeling. Manglande omtale eller strøymetadata hos brukaren sin tenar kan framleis gi mindre informasjon på den aktuelle tittelen. Sjå endringsrapporten for bibliotektypar og funksjonsomfang.
