# Verifisering av Spole 1.0.0-beta15

16. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 152.

## Omfang

Tilbakemeldingar på beta14 frå ekte TV og telefon:

- TV-framsida: fokus hoppa av og til til sidemenyen når heroen byta tittel. Årsaka var lina med
  fakta i heroen (år, lengd, vurderingar): ho kunne rullast og var difor fokuserbar, utan synleg
  ramme. Fokus landa der ved oppstart og ved retur frå menyen; når heroen byta, slutta lina å vere
  fokuserbar, og Android gav fokus til det første elementet på skjermen, sidemenyen. Feilen kom inn
  i beta12. På TV rullar lina ikkje lenger. I tillegg flyttar fokus seg til «Sjå meir» før
  «Spel av» forsvinn for ein tittel som ikkje kan spelast, og ned frå kontoknappen går til heroen.
- Mobil: «Be om» / «Be om sesongar» ligg i den faste lina nedst på tittelsida, som «Spel av».
- Be om-arket for filmar er lite: omslag, tittel, konto, eventuelle val, varsel og «Send førespurnad»,
  utan forklaringstrinn og standardsetningar. På TV er arket 600 dp breitt og får plass utan
  rulling; startfokus er «Send førespurnad». Seriar startar på første sesong som kan veljast.
- Oppdag på TV: rada med fokus blir rulla til rett under toppfeltet. Ein førespurnad med fokus
  viser bakgrunnsbilete, fakta og samandrag frå Seerr.

## Bygg og testar

- 1097 einingstestar: alle bestått utanom éin valfri nettverkstest som vart hoppa over. Nye testar
  dekkjer inngang i heroen med synleg fokus, fokus ved bytte til ein tittel utan avspeling, «Be om» i
  botnlinja på mobil, filmarket som får plass og startar på «Send», rada under toppfeltet, og
  bakgrunnsbilete frå Seerr. Testen for inngangsfokus feila med beta14-koden.
- Isolert TV 5566, klasse for klasse: RequestFlowUiTest 7/7, RequestRulesUiTest 4/4,
  TvControlsRegressionTest 6/6, SetupAndDiscoverTest 4/4, FocusOutlineTest 3/3,
  DesignRefreshUiTest 22/22, HomeLayoutUiTest 3/3, HomeMediaRowsTest 6/6,
  TvNavigationIntegrationTest 2/2, SmartShelvesTest 30/30, EnglishRequestFlowTest 3/3,
  CompactNavigationTest 4/4, UiConsistencyTest 1/1, ViewerExperienceTest 1/1,
  WideNavigationSettingsTest 7/7. `Alpha12UiTest.updatePanel…` feilar som i beta13 og beta14.

## Ekte profil

Lagra TV-profil 5564, signert bygg installert med `-r`:

- Framsida: fokus på «Spel av» ved oppstart; ned, opp, venstre til menyen og høgre attende til
  «Spel av»; fokus låg stilt på heroknappen gjennom over 50 sekund med heroar som bytte.
  Før rettinga låg fokus usynleg i fakta-lina etter oppstart og etter retur frå menyen (logga med
  eit mellombels testbygg), og ned frå kontoknappen gjekk innom menyen.
- Oppdag: førespurnaden «Hoarding for the Holidays» med bilete, fakta og samandrag i toppfeltet;
  ned til «Populært no» legg rada rett under toppfeltet.
- Be om-arket for «Masterplan»: Kvalitet «HD-1080p», Lagringsstad «Filmar 10 · 4,8 TB ledig»,
  varsel og «Send førespurnad» med fokus, alt synleg utan rulling. Arket vart lukka utan å sende.

Ingen førespurnad vart send. Android TV sin «Set up Device Lock» vart avvist med «Not now».
Testpakka brukte berre isolert 5566.

## Publisert produksjonsartefakt

- Universal-APK: 11 844 502 byte, SHA-256
  `5ace3fbb2d3aa31fdcbce1e243b7120da0aa404254d2b9cffda60a4bcafbf993`.
- R8-mapping: 97 520 120 byte, SHA-256
  `b9514ed5683a08a9e602c90dcd6b66b87a450e138804bfd9809589dc7e63541c`.
- Produksjonspakke `app.reelstack`, kode 152, ikkje debuggable. Fast sertifikat SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- FFmpeg-JNI-bibliotek og lisensar er byteidentiske med beta13; kjelde-/relenkingsarkivet er gjenbrukt
  (30 790 595 byte, SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`).
