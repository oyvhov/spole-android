# Dynamisk plassering av mobil-toppmenyen

Statuslinja sin tryggleiksmarg vart brukt to gonger: først i appen sin `Scaffold`,
deretter av `MobileLibraryFeature`. Dette gav for mykje tomrom over Spole-logoen,
søk og profilbildet, særleg på telefonar med stor kamerautskjering.

Appen markerer no `Scaffold`-margane som brukte med `consumeWindowInsets` etter
at dei er lagde til. Hero-toppmenyen legg berre til attverande systemmargar og
8 dp luft. Statuslinje, kamerautskjering og sidekantar blir dermed handterte éin
gong, ut frå det faktiske vindauget. Ingen telefonmodell eller prosent av
skjermhøgda er hardkoda.

Toppmenyen behaldar innhaldsstyrt høgd og trykkflatene. Hero bruker den målte
header-høgda til å halde filmlogo, metadata og handlingar frå toppmenyen.
Parallax og touch-sveiping er uendra.

## Stabil kunst i detaljpopupen

`openLibraryDetails` brukte `heroUrl` som bakgrunn. Dette feltet kan vere eit
`Thumb`-bilete, medan detaljkallet seinare gav eit `Backdrop`-bilete. Opninga
brukar no det same bakgrunnsvalet som mobil-Hero, med same store biletførespurnad.

Mobil- og TV-detaljflata beheld bakgrunnen frå opninga gjennom sein metadata.
Når ei eldre cacherad manglar bakgrunn, står den mørke flata til den rette
bakgrunnen kjem. Poster og miniatyrbilete blir ikkje brukte som mellombels
bakgrunn. Mobilkunst tonar inn på 180 ms; reduserte rørsler fjernar overgangen.
Lokale demobilete er framleis støtta. Riktig kunst blir ikkje blokkert av at
popupen sin opningsanimasjon framleis køyrer.

## Verifisering

- Endeleg bygg: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`
  bestod på fem minutt. **837/837** JVM-testar, ingen feil eller hoppa over.
- Tolv mobil-Hero-testar inkluderer kompakt 320 × 640 dp, høg 430 × 932 dp med
  64 dp topputskjering og ulike sideutskjeringar. Bakgrunnspolicyen blir kontrollert
  før og etter metadata, utan poster-fallback og med same høgoppløyste adresse.
- Lint: **0 feil**, 168 åtvaringar og éin hint.
- Lokal produksjonspakke: `app.reelstack`, framleis beta15 / kode 135,
  11 453 710 byte. Dette er eit upublisert testbygg; den publiserte beta15-APK-en
  og release-taggen er uendra. APK og tilhøyrande R8-mapping er bevarte under
  `app/build/mobile-header-details-local/`.
- Signeringssertifikat SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Lokal APK SHA-256:
  `21680dff1dd95ad5022a5a1496def21549c06370c6adbfcf0fd952f95a18ea5e`.
- Isolert mobil 5562: **10/10** Android-testar med normal skrift og **2/2** med
  fysisk systemskrift 2.0, ingen hoppa over. Dei to siste kontrollerer faktisk
  systemmarg og lesbare handlingar. Parallax-området kan framleis sveipast både
  horisontalt og vertikalt.
- Bilettestane skil raud poster, grøn opningsbakgrunn og blå sein metadata-kunst.
  Mobil og TV-komponenten held den grøne bakgrunnen; ein mobilserie utan kjent
  bakgrunn viser aldri den raude posteren og får den rette bakgrunnen utan å
  endre Hero-ramma. TV-komponenten er testa på isolert mobil, ikkje TV-AVD.
- Den første bilettestkøyringa brukte ein lysstyrketerskel som ikkje tok omsyn
  til dei mørke gradientane. Testen måler no fargeidentitet på ei flate utan tekst.
  Ingen produksjonsendring var nødvendig etter det endelege bygget.

- Begge lagra review-profilar er oppdaterte med signert `install -r` til det
  lokale testbygget. Mobilen sin opphavlege installasjonsdato 15. september og
  TV-en sin 9. september er bevarte; ingen appdata er sletta og ingen
  instrumentering er køyrd på desse profilane.
- Mobil: logo, søk og profil ligg visuelt høgare utan å overlappe statuslinja.
  Engelsk språk, grøn profil og eksisterande demo er bevarte. Severance-popupen
  opnar med bakgrunn og stabil layout. Mobilen hadde ikkje ein ekte mediekonto;
  sein nettverksmetadata er difor kontrollert med fixture-testane ovanfor.
- TV: ekte bibliotek og profilen er bevarte. Toy Story 5-detaljar er opna med
  bakgrunnskunst, riktig logo, ferdig metadata og synleg fjernkontrollfokus.
  Ingen avspeling eller profil-/bibliotekskriving er starta.
- Mobil-emulatoren viste eit mellombels «System UI isn't responding» etter
  kaldstart. «Wait» gjenoppretta systemet; det gjaldt System UI, ikkje Spole.
  Begge popupane er lukka og emulatorane står på framsida.
- Skjermbileta er berre lagra lokalt under den ignorerte byggmappa.

Desse resultata gjeld det lokale testbygget før versjonsauken. Endringane er med
i beta16; sjå [release-verifiseringa](VERIFICATION_v0.18.0-beta16.md) for endeleg
APK, testar, signatur og oppdateringsprøve.
