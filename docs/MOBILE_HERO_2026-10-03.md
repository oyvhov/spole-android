# Mobil-Hero: bakgrunnskunst og touch

## Val av bilete

Mobil brukte den delte Hero-adressa. Jellyfin-/Emby-mappinga prioriterer ofte `Thumb`
for film og episode; desse bileta har gjerne ein tittel innebygd. I ei smal mobilramme
blei denne teksten forstørra og delvis kutta, samtidig som appen viste sin eigen logo.

Mobil bruker no eit eige `backdropUrl` frå filmen eller serien. For episodar og sesongar
blir bakgrunnsbiletet henta frå forelderen, med rett eigar-ID og bilettag.
Adressa blir teken vare på i den eksisterande metadata-cachen utan databasesletting
eller migrering av tabellar. Nettverkskalla ber alt om `Backdrop`; inga ekstra henting
per kort er nødvendig.

Gamle cache-rader kan bruke eksisterande bakgrunnsadresse. `Thumb` og `Primary` blir
ikkje brukte som mobilbakgrunn. Dersom bakgrunnskunst manglar eller feilar, står logo,
tittel og handlingar på den mørke flata. Lokale demobilete kan framleis visast.
TV og bibliotek behaldar sitt eksisterande kunstval.

## Sveiping og layout

Ein innfødd `HorizontalPager` flyttar kunst og tittel med fingeren. Toppmenyen står
fast. Spegla endesider gir kort sveiping mellom siste og første tittel, utan ein lang
animasjon tilbake gjennom heile lista. Vertikal rulling blir handtert av framsida.

Automatisk bytte ventar åtte sekund etter ei fullført sveiping, og stoppar under
horisontalt drag og sidebytte, når Hero er utanfor skjermen eller eit detaljark er ope. Reduserte
rørsler stoppar automatisk bytte; manuell sveiping og prikkval fungerer framleis.
Prikkane har 48 dp trykkflate og ein tittel som skjermlesaren kan lese. Knappane
kan bryte til neste rad, og Hero kan vekse ved stor skrift.

## Verifisering

- Endeleg bygg: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`
  bestod, `BUILD SUCCESSFUL` på 4 minutt 23 sekund.
- JVM: **831/831**, ingen feil eller hoppa over. Ni mobil-Hero-testar dekkjer rendering,
  touch, sløyfe, prikkval, vertikal rulling, innhaldsoppdatering og automatisk bytte.
- Lint: **0 feil**, 168 åtvaringar og éin hint.
- Isolert `Spole_Instrumentation` på 5562: **6/6** med vanleg skrift og **1/1** med
  fysisk systemskrift 2.0. Testane stadfestar sveiping, handling på synleg tittel,
  fast toppmeny, sløyfe, reduserte rørsler, automatisk pause og vertikal rulling.
- Den første Android-køyringa avdekte at timeren avbraut sin eigen animasjon.
  Sidebyttet køyrer no i komposisjonen sitt separate coroutine-scope. Ein regresjonstest
  på både JVM og Android stadfestar at byttet fullfører etter pausen.
- Produksjonspakke `app.reelstack`, lokal versjon `0.18.0-beta14` / 134,
  APK 11 453 710 byte, sertifikat SHA-256
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Lokal APK SHA-256:
  `ea4fead504d6236694d2fdb97452b409ed52ec19e0693f5637ced96cb529cd0d`.
- APK og tilhøyrande R8-mapping ligg under den ignorerte mappa
  `app/build/mobile-hero-local/`.
- Lagrede `Spole_Review` på 5560 er oppdatert med signert `install -r`. Opphavleg
  `firstInstallTime` er uendra; engelsk grensesnitt, grøn profil og eksisterande
  demomodus er bevarte. Ingen instrumentering vart køyrd på review-profilen.
- Visuell kontroll på review-mobilen: sveiping byter kunst og tittel, toppmenyen
  står fast, og Hero-handlingane får plass også med fysisk systemskrift 2.0.
  Systemskrifta er sett tilbake til det opphavlege 1.0. Skjermbileta ligg berre
  lokalt under `app/build/mobile-hero-review-*.png`.
- Mobilen var allereie i demo. Val av ekte Jellyfin-/Emby-bakgrunnskunst er kontrollert
  med parser-, adresse- og cache-testar, ikkje med ei ny innlogging på mobil.

Desse resultata gjeld det lokale testbygget før versjonsauken. Mobilendringane er med
i beta15; sjå [release-verifiseringa](VERIFICATION_v0.18.0-beta15.md) for endeleg APK,
signatur, publisering og oppdateringsprøve.
