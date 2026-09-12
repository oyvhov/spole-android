# Serieside, sesongtema og fart — 12. september 2026

Alt her er bygd og kontrollert på den innlogga TV-emulatoren (`Spole_GoogleTV_Test`,
`emulator-5564`) med ekte Jellyfin- og Seerr-data. Signert, minifisert release-APK (R8 og
ressurskrymping på). **426 einingstestar, 0 feil. Lint 0 feil, 40 åtvaringar** — ned frå 46.

Utsjånaden på emulatoren er sett tilbake til det brukaren hadde før: Skog og Korall.

---

## 1. Ei serieside som viser serien

Ein serie er ei mappe for tenaren og ein **tittel** for eit menneske. Før dette opna eit trykk på
ein serie i biblioteket eit rutenett av sesongplakatar, og tittelsida hans — når ein i det heile
kom til henne — hadde ein knapp som sa «Vel episode» og sende heile jobben vidare til spelaren sin
eigen blalar. Den eine sida som visste kva serie det var, kunne ikkje fortelje kva serien inneheldt.

No opnar ein serie tittelsida si, og sida har:

- **Sesongar** som chips, med episodetal. Spesialar er sesong null og hamnar sist, fordi det aldri
  er der nokon vil byrje.
- **Episodar** med eige stillbilete, nummer, namn, lengd, framdriftsstripe og hake for sett. Kvar
  av dei startar frå sida, på sitt eige gjenopptakingspunkt.
- **Spel av / Hald fram** som peiker på den episoden tenaren seier kjem neste — `Shows/NextUp`
  avgrensa til serien, med `UserItems/Resume` som reserve. Å rekne det ut frå episodelista sjølv
  ville vore feil på tvers av einingar; tenaren veit det allereie.

Andre mappetypar — samlingar, musikk, biblioteka sjølve — opnar framleis som mapper.

### Den tomme episodelista

`Shows/{id}/Episodes` er ruta Jellyfin dokumenterer, og `getItems` stoppar ved den første ruta som
svarar i det heile. Ein tenar som svarar 200 med ingenting inni gav difor ein tom sesong som det
generiske `Items`-kallet listar heilt fint. Eit tomt svar tel no som «spør neste rute»; ein sesong
som verkeleg er tom endar tom frå begge. To testar i `SeriesBrowseTest`.

### Sesongnummeret

Eit sesongelement ber sitt eige nummer i `IndexNumber`, som parseren legg i `episode`; `season` på
eit sesongelement er serien sin eigen indeks og er som regel fråverande. Det var grunnen til at
«Specials» låg først trass sorteringa.

---

## 2. Plassen under mediebiletet

Den venstre kolonnen på TV var eit 230 dp-bilete mot ein meter svart. No:

- Biletet er **340 dp**, avgrensa av `maxWidth * .34f`.
- Under det står det som skildrar objektet i staden for augeblinken: år, lengd, kvalitet, aldersgrense,
  vurdering, status, sjangrar og taglina. Ingen av dei er avgjerder, så dei høyrer ikkje heime i
  lesekolonnen saman med tittelen, handlingane og sesongane.
- Rammer med feil form får eit **uskarpt bakteppe** av same biletet i staden for svarte stolpar. Ein
  serie med eit liggjande «plakat»-bilete mista ein tredel av kolonnen til svart. Under Android 12
  gjer `blur` ingenting og stolpane kjem tilbake, som er den gamle åtferda og ikkje ein feil.

---

## 3. Jul og Halloween

Ein sesong er ei **paring** av stemning og aksent, vald som éin ting. Folk ber ikkje om «aubergine
med gresskaraksent», dei ber om Halloween.

| Sesong | Bakgrunn | Aksent |
| --- | --- | --- |
| Jul | `NOEL` — grankveld, grøn-svart | `HOLLY` — varm raud |
| Halloween | `HALLOWEEN` — aubergine | `PUMPKIN` — gresskar |

Begge halvdelane er vanlege `VisualTheme`- og `AccentPalette`-verdiar, så paringa er ein snarveg og
ikkje ein eigen modus: den som vil ha gran med gullaksent får framleis det, og ingenting i appen må
vite om ein sesong går.

**Fargane bøyer ikkje regelen.** Spole held aksentane på 7:1 mot blekket. Grønt dominerer relativ
luminans, så ein metta postkasseraud kan rett og slett ikkje nå det talet mot nesten-svart utan å
lysnast. Regelen finst for at aksenttekst skal vere lesbar, og ei pynt er det siste som skal få bøye
han. `AccentContrastTest` og `SeriesBrowseTest` seier begge frå om nokon prøver.

**Ornamentet.** Snø som fell i desember, glør som stig i oktober, over framsidefeltet og ingen andre
stader. Det teiknar berre mjuke prikkar på låg dekkevne, ligg over biletet og under kvar kontroll,
ber ingen semantikk for skjermlesarar, og stoppar heilt når stemninga ikkje er sesongbasert, når
brytaren er av, eller når systemet har skrudd ned animasjonar. Partiklane blir laga éin gong frå eit
fast frø, så drifta er den same kvar gong skjermen opnar seg i staden for å stokke seg om under
lesaren.

---

## 4. Spelaren

- **Neste episode.** Ein serie som stoppar dødt på ein svart ramme er den eine tingen alle
  strøymetenester har lært å ikkje gjere, og Spole gjorde det. Kortet blir lasta medan episoden
  framleis går, så det er der med ein gong. Tolv sekund er lenge nok til å lese tittelen og
  bestemme seg; alt lesaren gjer går føre klokka, og eit avbrote nedteljing startar aldri igjen.
  Neste episode blir henta med `adjacentTo`, som svarar med førre, denne og neste i éin førespurnad
  — det er òg rett over ein sesongovergang, der ein klient som talde indeksnummer ville sagt at
  serien var slutt.
- **Hopp over introen.** Dette er den eine tingen folk installerer eit Jellyfin-tillegg for, og
  merka låg ubrukte på tenaren. Jellyfin 10.10 svarar sjølv i ticks; før det gjorde Intro
  Skipper-tillegget det i sekund under ei anna adresse. Begge blir spurde, i den rekkjefølgja.
  Knappen finst berre medan spelehovudet faktisk er inne i eit merkt stykke, så han er aldri ein
  kontroll som leitar etter noko å gjere — dei fleste bibliotek er aldri skanna og ser han aldri.
- **Overskanning.** Ein TV melder ingen innfellingar for ramma rundt sitt eige bilete, og mange
  apparat klipper framleis nokre prosent av kvar kant. `safeDrawingPadding` dekkjer systemfelta ein
  telefon har og ingenting her, som var grunnen til at kontrollane låg heilt nede mot kanten. Fem
  prosent av 960×540 dp er marginen Android TV ber kvar app halde.
- **«S03 E01» er borte.** Spelaren skreiv filnamnet fordi det var den einaste forma han hadde. Med
  tala som tal seier han no det same som resten av appen — og når serien manglar namn på tenaren og
  tittelen fell tilbake på episoden sitt, fell namnet ut av linja under så ho ikkje seier det same
  to gonger.

---

## 5. Fart

Tre ting, i den rekkjefølgja dei kosta mest:

**Eit binderkall per ramme per plakat.** `MediaArtwork` las `Settings.Global.ANIMATOR_DURATION_SCALE`
ved kvar komposisjon. På ein vegg med førti omslag er det førti rundturar til ein annan prosess for
kvar ramme som rører rutenettet — betalt samanhengande medan ein rullar, for ein verdi som endrar
seg omtrent éin gong i året. Temaet les han no éin gong og sender han ned gjennom
`LocalMotionEnabled`.

**Heile treet blei bygd opp att kvart femte sekund.** Avspelingspollinga gjorde
`current.copy(sessions = sessions)` uansett om noko hadde endra seg. Tilstanden er eitt objekt som
blir sendt til kvar skjerm, så ein kopi med identisk innhald sender likevel ut ein ny verdi og
komponerer alt på nytt. Ingenting spelar mesteparten av tida — appen bygde seg sjølv opp att tolv
gonger i minuttet for å kome fram til det same biletet. Same retting for førespurnadspollinga, som
i tillegg ikkje lenger set spinnaren når ingen har bede om oppdateringa.

**Pollinga har fått ei dørklokke.** «Spelar no» blei henta kvart femte sekund så lenge Heim stod
open. Jellyfin sender sjølv ei melding når avspeling startar, stoppar eller flyttar seg, så
`JellyfinSessionSocket` lyttar på den og ber appen hente på nytt. Medan kanalen er oppe går
pollinga ned til eitt minutt; fell han, er ho tilbake på det gamle med ein gong.

Socketen les med vilje **ikkje** sesjonslista ut av meldinga. Kven som har lov til å sjå kva
sesjonar blir avgjort i `MediaServerClient.sessions` mot dei stadfesta rettane til sjåaren, og ein
annan stad som byggjer den same lista frå ei anna melding er ein annan stad å gjere det feil — at
ein vanleg brukar ser dei andre skjermane i huset er akkurat den lekkasjen som skal vere umogleg og
ikkje berre usannsynleg. Så socketen seier «noko endra seg», og den vanlege, rettskontrollerte
førespurnaden svarar kva.

Alt anna på kanalen — biblioteksskanningar, brukaroppdateringar, omstartsvarsel — blir ignorert.
Å reagere på dei ville sett femsekunderspollinga tilbake under eit anna namn. Seks testar i
`SessionSocketTest`.

**Biletlastaren.** Ein eigen `ImageLoader` i staden for biblioteket sine standardar: 30 % av
appminnet til minnecachen, 192 MB diskcache. Spole legg no Jellyfin sin eigen innhaldstagg i kvar
biletadresse, så **URL-en er innhaldshashen** — eit bytt omslag får ei anna adresse. Det gjer
tenaren sine cache-hovud irrelevante og lèt diskcachen halde på eit bilete så lenge det er plass,
i staden for å revalidere over nettet kvar gong ei liste rullar forbi.

Autorisasjonshovudet blir dessutan bygd éin gong per innlogging i staden for éin gong per plakat,
nøkla på sjølve tilkoplinga, så utlogging eller bytt tenar gir ein annan nøkkel og det gamle hovudet
aldri blir brukt om att.

---

## 6. Listevisinga

Ei liste finst for å seie meir per tittel enn eit rutenett gjer. På ein TV er rada nesten to meter
brei, og ein tittel med eit årstal i seg lét dei andre tre fjerdedelane stå svarte. No står fakta og
opninga av omtalen i plassen rutenettet aldri kunne gitt dei, og årstalet blir ikkje gjenteke to
linjer seinare.

---

## 7. Ikonspråket

**Null Material-ikon att.** Det var 140 før denne bolken byrja, 61 då dagen starta, og no ingen:
kvart einaste glyf i appen er teikna i `SpoleIcons` med same 1,8-eininga strek.

Det inkluderer dei som var lette å oversjå — biblioteksikona i sidemenyen, spelaren sine ±10,
palett og tenar i innstillingsmenyen, sky-med-hake på Oppdag-korta, auget i passordfeltet, bjølla
med strek over, skjoldet, blyanten. Eit Material-glyf blant Spole sine eigne er akkurat den slags
detalj ein ikkje ser før ein ser han, og så ser ein ikkje noko anna.

`androidx.compose.material:material-icons-extended` er teke ut av avhengnadene. Det var det siste
som drog eit anna visuelt språk inn i appen. R8 hadde alt stroke det meste, så APK-en krympa berre
16 kB — poenget er kjelda, ikkje storleiken.

---

## 8. Favorittar fører ein stad

Spole kunne setje favorittmerket frå eit kort og frå ei tittelside, og hadde så ingen stad å vise
resultatet — merket gjekk til tenaren og forsvann. No er **Favorittar** ei rad på Heim, henta med
`Filters=IsFavorite` og avgrensa til dei valde biblioteka som alle andre feedar, så eit barnebibliotek
som er halde utanfor Heim held seg utanfor.

Episodar tel med, fordi kortmenyen kan stjerne ein.

Rada er ein ny seksjon, og ein seksjon som blir lagt til etter at nokon har lagra vala sine er
fråverande frå det lagra settet — og fråverande les som «av». `HOME_SECTIONS_VERSION` seier kva
tillegg denne installasjonen har sett, så rada dukkar opp éin gong og kan så slåast av som alle
andre.

## 9. Episodelista

- Lista bur i ein rullande kolonne, så kvar rad ho held blir komponert same om nokon kan sjå henne.
  Tolv er synlege, resten bak «N episodar til». Seksogtjue er ein lang sesong; to hundre er ein
  langtgåande anime, og å komponere to hundre rader for å vise seks er det som gjer ei sideopning
  til ein synleg pause.
- Eit utgjevingsfilnamn er ikkje ein tittel. «71.Grader.Nord.Kjendis.S17E01.NORWEGiAN» seier ingenting
  episodenummeret ved sida ikkje alt har sagt. Alle tre merka må vere til stades — ingen mellomrom,
  tre eller fleire punktum, og ein sesong-episode-markør — så ein ekte tittel som «S.W.A.T.» står
  urørt.
- Stillbiletet krympar når lesaren har bede om stor skrift, så orda ved sida beheld plassen.

## 10. Bibliotek lastar sjølv

Å nå slutten av ei side **er** førespurnaden. Ein knapp under seksti omslag ber lesaren stadfeste
noko dei allereie har gjort ved å rulle dit, og på ein fjernkontroll er han eitt stopp til mellom
dei og neste rad med kunst. Knappen står att for tilfellet der den automatiske lastinga feila.

## 11. Bibliotek er ei side om bibliotek

Rota i Bibliotek var fire mappefliser på svart. På ein TV blei det tre på fyrste rada og ei
åleine under, med høgre tredjedel av ein to meter brei skjerm tom. Fire fliser er ein meny, og ein
meny høyrer heime i sidefeltet — ikkje som heile innhaldet på ei side.

No viser sida kva som er *i* kvart bibliotek: ei overskrift du kan trykke for å opne det, og under
overskrifta dei nyaste titlane det held. Rekkjefølgja er den same og eitt trykk opnar framleis kva
som helst av dei, så ingenting som fungerte slutta å fungere — skilnaden er at lesaren no ser ein
grunn til å trykke.

Kikket kostar éi lita førespurnad per bibliotek (`Items/Latest`), henta éin gong og teken vare på.
Sida blir pila gjennom, ikkje rulla, og ei førespurnad per tastetrykk ville gjort henne verre enn
flisene ho erstattar. Eit bibliotek som feilar held flisa si: eit kikk er eit tillegg, aldri ein
grunn til at sida ikkje opnar.

Rada leier med det du er midt i. Home veit det alt, så det kostar ingenting, og «kvar eg kom
til» er ein betre grunn til å opne eit bibliotek enn «kva som er nytt» — framdriftsstripene på
korta er det som skil dei to. Talet i overskrifta er kappa til seks: eit seriebibliotek med tretti
køande episodar sa «30 på gang» over ei rad som viser åtte, og eit tal lesaren ikkje kan
kontrollere mot skjermen er verre enn ingen tal.

Standardikonet kjem frå kva tenaren seier biblioteket inneheld. Regelen låg to stader i
biblioteksdialogen frå før; han bur no i `LibraryIcon.forCollection` og blir brukt av begge.

## 12. Fyrste rada fekk plass til titlane sine

På Oppdag og Aktivitet var cella 174 dp. Ein 1080p-skjerm tok då fire kolonnar, og fire kolonnar
med 2:3-kunst pluss bilettekstane sine blei høgare enn skjermen — så fyrste rada med titlar blei
kutta av nedre kant. Fyrste rada er den eine rada alle ser.

150 dp gir fem kolonnar. Titlane får plass, og toppen av neste rad blir synleg: den stripa er
også korleis ein lesar med fjernkontroll lærer at det er meir. Oppdag og Aktivitet hadde drive frå
kvarandre her (174 mot 180, 16 dp mot 20) utan at nokon hadde bestemt det; dei deler eitt svar no.

Biblioteksida si eiga overskrift gjekk same vegen: på TV står tittelen og dei tre knappane på
same linje, og brødsmula står berre når ho seier noko sidefeltet ikkje alt seier. Innhaldet
startar 134 px høgare enn før.

## 13. Ei kappa linje skal sjå kappa ut

Statuslinja på eit aktivitetskort slutta midt i eit ord: «Ventar på godkjenning eller at». `Text`
hadde `maxLines` utan `overflow`, så resten forsvann utan teikn. Det same mønsteret låg på tretten
stader i appen. Alle tretten seier no `TextOverflow.Ellipsis`: passar teksten, endrar ingenting;
passar han ikkje, ser lesaren at det er meir.

## 14. Episodesida viser sesongen sin

Ei episodeside var eit avsnitt omtale og ein halv skjerm svart. Det nyttige på ei episodeside er
resten av sesongen, så no lastar ho seriens sesongar og opnar på sin eigen — ikkje på det tenaren
meiner kjem nærast i serien, for lesaren ser på episode 8 i sesong 7, ikkje på serien.

Tre ting følgde med:

- **Sesongstripa rullar til den valde sesongen.** Ein serie med tjueto sesongar viser fire av dei,
  og utan dette såg lesaren sesong éin sine knappar med sesong sju sine episodar under.
- **Rada du er på er merkt.** Utan merket seier ingenting på skjermen kva for ei av fjorten rader
  som er den du kom frå.
- **Omtalen flytta under biletet på TV.** Lesespalta si oppgåve er kva du kan *gjere* med tittelen
  — spele han, velje spor, velje episode — og eit avsnitt prosa midt i det skuva episodelista ut
  av skjermen medan spalta ved sida av biletet stø tom. Ei skildring er ikkje ei avgjerd.

`SeriesBrowse` hugsar kva detaljside han blei lasta for (`openedFor`), så ingen skjerm treng rekne
seg fram til ein serie-ID frå ein episode-ID.

## 15. Innstillingar brukar breidda

Ei rad der samandraget *er* verdien stod som to linjer med høgre halvdel tom. Slike rader er no
éi linje: tittel til venstre, fargeprikk og verdi til høgre, pil til slutt. Fem val får plass der
fire gjorde før, og svaret gøymer seg ikkje lenger under spørsmålet.

Regelen gjeld alle radene der samandraget *er* verdien, ikkje berre i Utsjånad: menyrekkjefølgja
(«Synleg i menyen»), tenestene («Øyvind · Tilkopla») og oppdateringsrada. Seks tenester får plass
der tre gjorde før. Rader med ei forklarande setning — «Administrer bibliotek», «Rader på Heim» —
er framleis to linjer, fordi der er andre linja ikkje eit svar.

Prikken er ein sirkel, ikkje ein avrunda firkant. Halvparten av desse fargane er bakgrunnar, og ein
mørk, avrunda firkant med ramme i ei innstillingsrad ser ut som ein avkryssingsboks som ikkje er
kryssa av.

Førehandsvisinga er tre kort på ei hylle, eitt av dei i fokus. Abstrakte klossar kunne ikkje vise
kva noko av dette gjer: hjørneradius, kortstorleik og fokusramme tyder berre noko på forma dei blir
brukte på, og aksenten tyder berre noko der appen faktisk brukar han.

## 16. Eitt namn på ein episode

«Sesong 6 - Ep 13» står no overalt. Helten skreiv sesongen og episoden på kvar si linje, og
Spelar no-kortet bygde sin eigen «S06 E13» i parsaren — same skjerm, same faktum, tre skrivemåtar.
Tala reiser som tal heilt fram til skjermen, som skriv dei på lesarens språk.

Reglane for kva som er *namnet* på ein episode ligg no i `episodeNameOf` i datalaget, fordi
parsaren treng dei også: når tenaren svarar utan serienamn, fell kortet tilbake på episoden sitt
eige namn, og «Episode 9 - Getaway Sticks» gjentok eit nummer som stod rett under.

## 17. Førespurnadshistorikken

Fem linjer overskrift før eitt einaste omslag, og deretter fire kolonnar der bilettekstane fall
under nedre kant — så sida viste fire plakatar utan namn. På TV står talet og «Oppdater
historikken» på tittelen si eiga linje, og rutenettet brukar same cella som dei andre
omslagsveggane. No står tittel, type og dato under kvart omslag, og neste rad kikkar fram.

## 18. Fart, målt i staden for gjetta

Compose-kompilatoren kan rapportere kva han veit om koden sin, så denne bolken tok målet først.
Rapporten sa at 75 klasser var ustabile, og grunnen var nesten alltid den same: ein `List<String>`.
`LibraryMedia` har to — `facts` og `genres` — så kvart kort, kvar rad og kvar side som tek i eit
`LibraryMedia` stod på identitetssamanlikning.

Det tyder: sterk hopping er på, så ein ustabil parameter hindrar ikkje at ein composable *kan*
hoppast over — han endrar korleis parameteren blir *samanlikna*. Ustabil = samanlikna på identitet,
so ei oppdatering som hentar dei same dataa i nye objekt byggjer opp att alt som rører dei. Stabil =
samanlikna med `equals`, og då hoppar han over.

[`app/compose_stability.conf`](../app/compose_stability.conf) lovar at listene, karta og mengdene i
dette prosjektet aldri blir endra etter at dei er gitt frå seg. Lovnaden er revidert, ikkje antatt:
alle `mutableListOf`/`mutableMapOf` i prosjektet byggjer eit resultat inne i ein funksjon og gir det
frå seg som ein lesekopi, og dei tre menyomstokkingane kopierer lista, endrar kopien og gir frå seg
den einaste referansen med det same.

Målt etterpå: 35 modellklasser flytta frå ustabil til stabil, og 85 argument frå identitet til
likskap. Talet på «skippable» composables flytta seg knapt, og det er venta — sterk hopping dekte
alt den halvdelen.

To andre ting i same bolken, begge mekaniske:

- Tre hyller bygde `cardActions.copy(canRemoveFromResume = false)` på nytt for kvar oppbygging, så
  kvart kort i rada under bygde seg opp att med han. `withoutResumeRemoval()` hugsar kopien.
- Biblioteksida sende `state.resume + state.nextUp` som eit nytt listeobjekt kvar gong, som gjorde
  heile landingssida umogleg å hoppe over.

## 19. Tre ting som viste seg når sida blei full

Å fylle spalta under biletet gjorde tre feil synlege som hadde stått der heile tida.

**Stjerna peika på årstalet.** Faktalinja er ein `Row` med eit stjerneikon først og heile linja som
éin tekst etter. Vurderinga står midt i den teksten, så ikonet stod framfor «2025». Og når linja
brekte til to, sentrerte `CenterVertically` ikonet mellom dei to linjene med ingenting ved sida av
seg. No leier vurderinga linja, og ikonet ligg mot toppen.

**«1 · Episode 1».** Ein episode utan namn heiter «Episode 1» hos Jellyfin, og prefikset skulle
strippast — men tenaren her skriv talet med hardt mellomrom, og `\s` i Java-regex dekkjer ikkje
U+00A0. På skjermen er dei to teikna like. Namnet blir no normalisert før samanlikninga.

**Strippen var for grådig.** Testen for det hardt mellomrommet avdekte at «Episode 1 and a half»
blei til «and a half». Talet er berre eit prefiks når det er heile namnet eller det følgjer eit
skiljeteikn etter det.

**Medverkande** flytta same vegen som omtalen: frå botnen av lesespalta, under eit statuskort og
fjorten episoderader, til spalta ved sida av biletet — som er der ein lesar faktisk ser etter fjes.

## 20. Ei setning éin gong

«Vel bibliotek» sa «Vis i Bibliotek, heimrader og søk» under kvart einaste kort. Fire like
setningar er ikkje fire forklaringar, det er éi setning gjenteken fire gonger — og ho stod i vegen
for det korta faktisk skulle seie: namnet og dei to vala. Forklaringa er flytta til toppen, der ho
blir lesen éin gong, og er skriven om til å dekkje begge vala.

Rekkjefølgja i sidemenyen blei kontrollert i same runden: ho dukkar opp når meir enn eitt bibliotek
er festa, med opp/ned per rad og rett deaktivering i endane. Kontrollen blei gjord ved å feste eit
bibliotek til, sjå at blokka kom, og **avbryte** — brukarens eigne val er ikkje endra.

## Står att

- **Mobil er ikkje kontrollert.** Detaljsida, handlingsrada og veljarane er endra for alle
  formfaktorar, men berre sett på TV. Telefonemulatoren blei ikkje starta: verten hadde under
  2 GB ledig, og å starte han ved sida av TV-en har teke ned den innlogga profilen før.
- **Instrumenterte testar er framleis aldri køyrde**, av same grunn — dei krev dei isolerte
  profilane.
- **Hopp over introen og Neste episode er ikkje sett i drift.** Logikken er dekt av tolv testar,
  men biblioteket på denne tenaren har ingen intromerke, og ingen episode blei spelt heilt ut.
- Versjonsveljaren startar rett fil, men er berre prøvd mot titlar med éi fil.
- Ikonspråket er ferdig: null Material-ikon att, og `material-icons-extended` er ute av bygget.
- **Bibliotek-kikket er ikkje prøvd mot Emby.** `Users/{id}/Items/Latest` er med som andre rute,
  men berre Jellyfin har svart på han her.
- **Spelaren er ikkje opna på nytt i denne bolken.** Endringane i denne delen rører ikkje spelaren,
  og å starte avspeling ville skrive i brukarens eiga historikk på tenaren.
