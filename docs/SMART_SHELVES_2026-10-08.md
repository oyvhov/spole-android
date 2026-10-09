# Smarte hyller, rader på framsida og fokusfeil · 8.–9. oktober 2026

I beta6 kunne berre Halloween og jul få ei eiga side i biblioteket, og regelen var fast. No kan
kvar profil lage eigne hyller ut frå stikkord og sjangrar på tenaren. Ei hylle viser filmar og
seriar kvar for seg, med mindre omslag enn biblioteket elles, og kvar hylle kan stå i
biblioteket, på framsida og i sidemenyen, kvar for seg. Halloween og jul er ferdige malar av same slag.

## Endra

- **Regelen** tek med ein tittel når han har *eitt* av stikkorda *eller* éin av sjangrane, eller,
  etter valet «Både stikkord og sjanger», eitt av kvart.
  - Stikkord er TMDB-nøkkelorda tenaren har lagra (`Tags`), og sjangrar er dei frå tenaren (`Genres`).
    Tenaren set filtera saman med «og». «Både» er difor éi spørjing, og «eller» er to som blir slått
    saman. Titlar med stikkord kjem først, og kvar tittel kjem berre éin gong.
  - Valet kjem berre når hylla har både stikkord og sjangrar. Utan begge er regelen den same uansett.
  - Spørjinga går på tvers av alle bibliotek (`Recursive=true` utan `ParentId`).
  - «Filmar og seriar», «Berre filmar» eller «Berre seriar» og «Berre det du ikkje har sett»
    (`IsPlayed=false`) er med i spørjinga. Filtera og sorteringa i biblioteket gjeld på hylla òg.
  - Spørjingane brukar ein tålmodigare transport (40 sekund i staden for 9). Ein stor katalog kan bruke
    lenger enn ei Home-rad, og eit tidsavbrot ser ut som ein tenar utan sjangrar.
- **Plassering, hylle for hylle.** Tre brytarar i byggjaren, uavhengige av kvarandre:
  - **I biblioteket:** ei flis under «Smarte hyller». Utan nokon hyller (og utan ein sesongmal) er raden
    heilt borte. Ei første hylle lagar ein frå «Tilpass biblioteksida» eller frå «Tilpass framsida».
  - **På framsida:** ei eiga rad per hylle og per tenar, som «Halloween · Jellyfin» og
    «Halloween · Emby», med «Bla i alt». Rada kjem og går med perioden til hylla, og står ovanfor
    «Nye filmar» første gongen. Ho kan flyttast, skjulast og få eige kortformat (automatisk, plakat
    eller miniatyr) under Tilpass framsida, som kvar anna rad.
  - **I sidemenyen** (TV og nettbrett), medan perioden varer.
  - Står alle tre av, seier byggjaren frå om at hylla ikkje blir vist nokon stad.
- **«Smarte hyller» er ei rad i rekkjefølgja på biblioteksida,** som favorittar og bibliotekshyllene.
  Ho kan flyttast og skjulast under «Tilpass biblioteksida». Ei lagra rekkjefølgje frå før får henne
  over bibliotekshyllene. «Ny smart hylle» er ein liten, rund «+» etter siste flis. Teksten kjem fram
  berre medan fjernkontrollen står på knappen.
- **Malane** (Halloween, Jul, Julekalender, Påskekrim) har alle ei rad på framsida i perioden sin.
  Påske går frå palmesøndag til andre påskedag, rekna med påskeformelen. Malen kan slåast av per hylle.
- **Sida for ei hylle** har bolkane «Filmar» og «Seriar» med teljing, regelen øvst og om lag sju
  omslag i breidda på TV (tre eller fire på mobil), der biblioteket har fem og to.
- **Byggjaren** krev inga skriving:
  - Ein ny hylle kan starte frå ein mal. Sjangrane kjem som brikker frå tenaren, og fleire stavemåtar
    av same sjanger (Horror, Skrekk, Grøssar) blir éi brikke.
  - Stikkord blir føreslegne frå ei kort liste over vanlege nøkkelord, men berre dei tenaren har.
    Søkjefeltet finn resten, og «Legg til «…»» tek eit stikkord tenaren ikkje viser.
  - Øvst står kva regelen gir medan han blir endra: «Gir minst 44 filmar og 18 seriar.»
  - Svarar ikkje tenaren med sjangrar og stikkord, står det, med ei linje om kva som gjekk gale og
    «Prøv igjen». Det er ikkje det same som ein tenar utan sjangrar.
  - Byggjaren ligg på rotnivå i appen og kan opnast frå biblioteket, frå «Tilpass biblioteksida»
    og frå «Tilpass framsida».
  - «Lagre» er aldri grå. Utan regel seier byggjaren frå i staden. «Slett hylla» spør éin gong til,
    og ein endra mal blir «Tilbakestill». Ei sletta hylle tek rada si ut av den lagra framsida.
- **Tekstfelt på TV:** Compose sine `String`-tekstfelt startar inndata så snart fokus kjem, same kva
  `showKeyboardOnFocus` seier. På Google TV opna tastaturet seg når fjernkontrollen gjekk forbi
  namne- og søkjefeltet, og heldt så på fjernkontrollen. Det nye `RemoteTextField` byggjer på
  `TextFieldState`: tastaturet ventar på OK, og opp/ned går alltid ut av feltet. Same felt er no brukt
  for adresse, oppsettlenkje og brukarnamn i oppsettet av tenarar; passordfelta er som før. Ein variant
  tek ein vanleg `String`-verdi og skriv aldri eit ekko av det som nettopp vart skrive attende i feltet.
- Når byggjaren er lukka på TV, får «Endre» fokus att.
- **Framsida med «Rolege overgangar» av:** det fokuserte kortet veks 6 %. Første kort i ei rad låg
  heilt inntil kanten av rada og vart skore av mot sidemenyen. Radene på Heim går no ut i margen,
  og innhaldet startar med same innrykk som før. Valet er behalde: Google TV har ikkje eit vanleg
  tilgjengeval for å slå av animasjonar.

## Etter beta7 (9. oktober)

- **Hylla kom ikkje på framsida.** Tre grunnar, alle retta:
  - Ei ny hylle hadde «På framsida» av. Ei hylle ein lagar sjølv, er no på både i biblioteket og på framsida.
  - Ei hylle frå ein mal arva perioden til malen. Ei Halloween-hylle laga 9. oktober venta difor til
    20. oktober. Ei eiga hylle startar no på «Heile året», og malen gir berre regelen. Vel ein ein
    periode som ikkje har byrja, seier byggjaren når hylla kjem på framsida og i menyen.
  - Ei lasting som vart avbroten, til dømes av ein profilsjekk ved oppstart, vart rekna som ferdig, så
    rada venta ti minutt tom. No tel berre ei fullført lasting, og framsida ber sjølv om radene når ho visest.
- **Plakatane på flisa, rada og sida vart henta på nytt kvar gong.** Bileta låg i Coil sin diskbuffer,
  men kva titlar hylla hadde, vart spurt om frå tenaren ved kvar opning, og ein stor katalog brukar
  sekund på svaret. Det siste svaret på kvar hyllespørjing ligg no i ei fil i app-bufferen
  (`FileShelfAnswerCache`, namngitt med ein hash og haldt i 14 dagar). Flis, Home-rad og side blir
  teikna frå det med ein gong, og tenaren sitt ferske svar tek over etterpå. Bufferen blir tømd med
  resten av mediebufferen ved utlogging.
- **Ny side for ei hylle:**
  - **Toppfelt:** bakgrunnsbilete frå ein av titlane under to mjuke skuggar, ei vifte av tre plakatar på
    breie skjermar, namnet og dei to knappane. Ikkje noko meir å lese: merket «Smart hylle», regelen
    som brikker og tellelinja vart tekne bort etter første utkast, fordi toppen hadde seks rader før
    første plakat. Regelen står i byggjaren. Toppfeltet er lågare, og på TV syner første plakatrad
    under det. På mobil står tilbakepila på same linje som namnet.
  - **Overrask meg** opnar ein tilfeldig tittel du ikkje har sett.
  - **Filter:** Alt, Filmar og Seriar med tal, når hylla har begge slaga. Tala står berre her, ikkje
    i bolkoverskriftene «Filmar» og «Seriar».
  - **Sortering:** etter regelen, nyleg lagt til, nyaste først, tittel A–Å eller best vurdert. Valet blir
    lagra på hylla og gjeld òg flisa og rada på framsida. Tenaren sorterer, så «Nyleg lagt til» er dei
    nyaste i heile katalogen. «Eller» er to svar som blir sorterte saman att.
  - **Skjul sette** tek sette titlar bort. Når alt er sett, seier sida det og tilbyr å vise dei att.
  - **Ny:** titlar lagde til dei siste 14 dagane får eit merke.
  - På TV startar fokus på «Overrask meg» med heile toppfeltet synleg. Første tittel er eitt trykk ned.
  - Sida rullar berre når fokus treng det, og då med ein glimt av neste rad under. TV-ens eigen
    rulling dreg fokus mot ein tredjedel av skjermen og kutta toppfeltet. Den første versjonen som
    rulla «akkurat nok», let fokus hoppe til første kolonne når neste rad låg heilt utanfor. Glimten
    held neste rad utlagd, så ned går rett ned same kolonne.
  - Opp att til filterrada eller toppfeltet rullar heile toppen fram att. Unntaket er ein topp som er
    høgare enn skjermen, til dømes med største tekst. Då held vanleg rulling den fokuserte knappen synleg.
    Rullinga mot toppen går føre knappen si eiga «vis meg»-rulling, som elles stoppa sida halvvegs med
    «Smart hylle» kutta. Går fokus ned att før ho er ferdig, stoppar ho.
- **Biblioteksfiltera påverkar ikkje lenger hyllene.** Hyllespørjinga sende standardsorteringa frå
  biblioteksida med. Ho har no berre sin eigen regel og si eiga sortering.
- **Fjernkontrollen i redigeringsradene:** ned frå brytaren i ei rad med ekstra knappar («Ny smart hylle»,
  «I sidemenyen», bibliotekval på framsida) gjekk forbi knappane til neste rad. Ned går no til knappane først.
- **Raske trykk opp og ned held kolonna i alle rutenett.** Med raske trykk ned frå tredje kolonne
  hamna fokus i første. Det gjaldt òg det vanlege biblioteket, ikkje berre hyllesida. Eit trykk kom
  fram før rullinga, til ei rad som ikkje var lagd ut. Compose legg då ut rada ein tittel om gongen og
  gir fokus til den første. `Modifier.steadyRemoteRows` ser kvar fokus er. Eit trykk går med ein gong når
  rada bak er lagd ut, og ventar elles til rullinga har henta ho fram. Berre eitt trykk ventar om
  gongen, så ein knapp som blir halden inne, stoppar når han blir sleppt. Gjeld bibliotek, smarte
  hyller, søk, Oppdag, Aktivitet, førespurnadshistorikk og dei tre rutenetta i barnemodus.

## Emby

- Emby er ei vanleg kjelde: hyller, sida for ei hylle, Home-rader og byggjaren verkar mot Emby òg,
  og rada på framsida kjem per tenar. Prøvd mot ein ekte Emby-tenar (same filmar og seriar som på
  Jellyfin): «Stikkord eller sjanger» gir 40 filmar og 18 seriar, og stikkordet `halloween` åleine gir 3 filmar.
- **Emby svarar ikkje på `Items/Filters`,** den lista Jellyfin og biblioteksfilteret brukte. Spole prøver
  difor `Items/Filters`, så `Items/Filters2`, og fyller ut med Emby sine eigne lister `Genres` og `Tags`.
  På den ekte Emby-tenaren kjem no både sjangrar og forslag til stikkord (halloween, christmas, dog …),
  og biblioteksfilteret på Emby får «Sjanger» på same måten. Når ingenting svarar, viser byggjaren kva
  tenaren sa og «Prøv igjen».
- Jellyfin og Emby har ikkje like stikkord på same filmar. «Både stikkord og sjanger» gav 3 filmar
  og 1 serie på Jellyfin og 1 film på Emby.

## Verifisering

- Einingstestar: 949, 0 feil, 1 hoppa over (valfri prøve mot ekte nett). Nye:
  - `SmartShelfTest` (14): lagring, periodar, Home og bibliotek per hylle, «Både», barnefilter
  - `SmartShelfQueryTest` (14): spørjinga, «Både», per-bibliotek-sjangrar, reserveveger og feillinje, tålmodig transport
  - `HomeLayoutTest`: nøklar og plass for hyller på framsida
  - `LibraryPresentationTest`: rekkjefølgja på biblioteksida
- Lint: 0 feil, 185 åtvaringar og 1 hint, same tal som for beta6.
- Isolert TV 5566, klasse for klasse:

  | Testklasse | Resultat |
  | --- | --- |
  | `SmartShelvesTest` | 15/15 |
  | `MediaRefinementTest` | 5/5 |
  | `HomeMediaRowsTest` | 6/6 |
  | `HomeLayoutUiTest`, `HomeHeaderTest` | 3/3, 5/5 |
  | `LibraryPresentationUiTest` | 9/9 |
  | `LibraryBrowserUiTest` | 3/3 |
  | `DesignRefreshUiTest` | 22/22 |
  | `SeasonalTouchesTest` | 8/8 |
  | `WideNavigationSettingsTest` | 6/6 |
  | `FocusOutlineTest`, `TvNavigationIntegrationTest`, `LanguageResourcesTest` | 3/3, 2/2, 6/6 |
  | `TvRefinementUiTest` | 37/38 |

  `SmartShelvesTest` dekkjer mellom anna: filmar og seriar kvar for seg, mindre omslag, regel før
  lagring, forslag berre frå tenaren, «eller» og «og», plassering, Home-rader per tenar og skjult
  rad, og fokus utan tastatur.
- Kjende feil, utan samband med endringane: `TvRefinementUiTest.libraryEditorGivesARemoteItsFirstFocus`,
  `PersonalizationTest.wideMediaReachesTheEdgeButProfileKeepsItsInset`, `TvLibraryRegressionTest` 0/1,
  `SheetKeyboardFlowTest` 1/6 (same fem feil på uendra beta6-kjelde) og `ReelstackSmokeTest`, der
  `@Before` ikkje kjem til Heim på TV.
- Ekte data på lagra TV-profil 5564 (Jellyfin og Emby), med signert bygg av greina:
  - Halloween-malen gav 44 filmar og 18 seriar med «eller» og 3 filmar og 1 serie med «Både».
  - Heile byggjaren gjekk med fjernkontroll utan at tastaturet opna seg. OK opna det, skriving
    gav «Legg til «chri»», og BACK lukka det.
  - Med «På framsida» på stod «Halloween · Jellyfin» og «Halloween · Emby» som eigne rader ovanfor
    «Nye filmar». Utan hyller var raden i biblioteket borte.
  - Sjangrar kom på både Jellyfin og Emby etter at lista vart henta per bibliotek.
  - Testhylla vart sletta, kjelda sett tilbake til Jellyfin, og profilen køyrer att den offisielle
    beta6-APK-en med same SHA-256 som arkivet. Appen startar utan krasj.
- **Ein krasj fanga av oppstarten på TV, ikkje av testane.** Eit nytt felt i `ReelstackViewModel`
  stod under `init`, så det var `null` då første oppdatering køyrde, og appen kræsja ved start.
  Feilen kom berre fram når ei hylle hadde ei rad på framsida, så den første installasjonen starta
  fint. Han hadde krasja alle med Halloween-malen frå 20. oktober. Einingstestane bygger ikkje
  ViewModel og såg det ikkje. Feltet står no over `init`, og ei Halloween-hylle på framsida starta
  appen utan krasj.

### Etter beta7

- Einingstestar: 959, 0 feil, 1 hoppa over. Nye: `ShelfAnswerCacheTest`, `ShelfBringIntoViewTest` og
  fleire i `SmartShelfTest` og `SmartShelfQueryTest`.
- Lint: 0 feil, 185 åtvaringar, same som før.
- Isolert TV 5566:

  | Testklasse | Resultat |
  | --- | --- |
  | `SmartShelvesTest` | 21/21 |
  | `SteadyRemoteRowsTest` | 1/1 |
  | `LibraryPresentationUiTest`, `LibraryBrowserUiTest` | 9/9, 3/3 |
  | `DesignRefreshUiTest` | 22/22 |
  | `HomeMediaRowsTest`, `SeasonalTouchesTest` | 6/6, 8/8 |
  | `SetupAndDiscoverTest`, `UiConsistencyTest`, `ViewerExperienceTest` | 5/5, 3/3, 3/3 |
  | `FocusOutlineTest`, `TvNavigationIntegrationTest` | 3/3, 2/2 |

- Dei andre klassane feila likt med debug- og testbygget frå 8. oktober, som er eldre enn endringa:
  - `RequestHistoryUiTest` 4/7: `performScrollTo` finn ikkje knappar rutenettet ikkje har lagt ut.
  - `ActivityAdaptiveTest` 3/4 i dette bygget og 5/6 i det eldre: tilbaketrekkingsmenyen tek ikkje imot trykk.
  - `HomeSearchNavigationTest` 0/4: `@Before` kjem ikkje til Heim på TV.
- Ekte data på TV-profil 5564, med signert bygg:
  - Ei hylle frå Halloween-malen opna med heile toppfeltet synleg og fokus på «Overrask meg».
  - Seks trykk ned i tredje kolonne heldt kolonna, med neste rad synleg under. Opp att kom heile
    toppfeltet fram. Same gjekk med raske trykk, både på hyllesida og i Filmar-biblioteket. I Filmar
    hoppa fokus før rettinga til første kolonne.
  - Testhylla vart sletta. Profilen køyrer att den offisielle beta7-APK-en, med same SHA-256 som arkivet.
- Barnemodus-rutenetta har ingen UI-test. Dei har fått same tillegg som dei andre, og
  `SteadyRemoteRowsTest` prøver sjølve åtferda.

## Avgrensingar

- Stikkord blir berre føreslegne når tenaren har nokon. Ein tenar utan TMDB-nøkkelord gir berre sjangrar.
- Kvar av dei to spørjingane hentar opptil 60 titlar til hylla og 24 til ei rad på framsida.
- Regelen kjenner «eller» eller «både», men ikkje «ikkje».
- Hyllene ligg på eininga per profil og følgjer ikkje kontoen til ei anna eining.
- Ei rad på framsida lastar når framsida opnast, og blir oppfrisk etter ti minutt, ved henting og
  når regelen endrar seg. Ei rad som feilar beheld det ho viste.
- Telefonen er ikkje prøvd med ekte data. AVD-en 5562 hadde full lagring.
