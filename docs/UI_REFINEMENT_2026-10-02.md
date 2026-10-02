# Hero, bibliotekkort og profilmeny · 2. oktober 2026

- Hero held det førre biletet fullt synleg under det nye under ein 500 ms overgang.
  Bileta blir førehandslasta i begge TV-flatene, og den ekstra biletfadinga er slått av.
  Tekstovergangen på vaksenflata er korta til 300 ms.
- Bibliotekkort i barnemodus opnar biblioteket som knappar. Fast valramme og hake er
  fjerna; trykkrespons og fjernkontrollfokus er bevarte. Gjeld mobil og TV.
- Profilmenyen opnar med 100 ms fading og 120 ms skalering frå 98 prosent.
  Vertikal gliding er fjerna, og redusert rørsle gir ingen animasjon.
- «Legg til barneprofil» er fjerna frå profilmenyen. Oppsettet finst under «Tenestene dine».

## Verifisering

- `testDebugUnitTest`: 810 testar bestått, ingen feil eller hoppa over.
- `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease`: vellukka.
- Lint: ingen feil, 167 åtvaringar og eitt hint.
- Signert produksjons-APK installert med `install -r` på dei eksisterande
  `Spole_Review` (5560) og `Spole_GoogleTV_Test` (5564). Signaturen samsvarar med
  den faste Spole-nøkkelen. Ingen appdata eller kontoar er sletta.
- Profilmenyen kontrollert på begge einingar: ingen handling for å leggje til barneprofil.
- TV: ekte innhald og eksisterande barneprofil opna. Bibliotekkorta har ingen valhake.
  Opptak av vaksen-Hero kontrollert i biletserie gjennom tittelbyte.
- Mobil: den lagra profilen viste demoinnhald og berre hovudprofil. Barnemodus på mobil
  er difor ikkje manuelt verifisert; den eksisterande Compose-testen for bibliotekkort
  er oppdatert og bestått. Ingen ny barneprofil eller konto er laga for kontrollen.
- Barnemodus på TV vart kontrollert under innlasting og i ferdig tilstand;
  eit automatisk byte mellom fleire barne-Hero-titlar vart ikkje observert i opptaket.
- Instrumentering er ikkje køyrd på einingane med lagra kontoar.
- Skjermbilete og opptak ligg berre lokalt i den ignorerte byggmappa.

Dette er eit lokalt bygg av 0.18.0-beta12, ikkje ein ny publisert release.
