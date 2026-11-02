# Rørslespråk, rader som kjem inn, profilflukt og toppmeny på TV

1. november 2026. Bygd oppå beta30. Idéane kjem frå Swiftly Player (MPL-2.0); tal og åtferd er
lånte, koden er skriven her.

## Status 2. november 2026: ferdig til beta31

Alt i planen er bygt: rørslespråket (`fd03154`), hopp-knappen, radinngangen og toppmenyen
(Codex, sjå [overleveringa](HANDOFF_CODEX_2026-11-02.md)), og profilflukta (`d96bc8a`,
`ui/ProfileSwitchMotion.kt` og `ui/ProfileFlightLayer.kt`). Avvik frå planen under 3: flukta
startar frå menyen sin eigen `Popup`, så rada melder skjermkoordinatar som laget reknar om; utan
kjent sirkel, med redusert rørsle, via PIN eller frå Stova er det berre vanleg byte. Testbevis
står i `VERIFICATION_v1.0.0-beta31.md`. Resten av dokumentet er den opphavlege planen.

## Kvifor

Spole har 55 animasjonar i 30 filer med 18 ulike lengder, og alt blir kutta til 0 ms når rørsle er
slått av. Ingenting flyttar seg mellom skjermar, bortsett frå søkjefeltet. Swiftly kjennest heil
fordi fire prinsipp blir følgde overalt: ting flyttar seg i staden for å blinke, ingenting kjem før
det er klart og så kjem det i tur og orden, eitt rørslespråk, og same rytme uansett kor fort ein
trykkjer. Eigaren vil i tillegg kunne velje Swiftly sin toppmeny på TV i staden for sidemenyen.

## Mål

- Fem namngjevne rørsler i éi fil, og alle UI-animasjonar brukar dei.
- Redusert rørsle (systemval, «Redusert rørsle» eller lettvekts-TV) gir ein 100 ms toning, ikkje
  eit kutt.
- Heimesida sine rader kjem inn éi og éi når innhaldet er klart: etter oppstartsbiletet, når ein kjem
  tilbake til Heim, og etter profilbyte.
- Profilbyte frå menyen i hjørnet: biletet du vel, glir opp til profilsirkelen og landar der; det
  gamle innhaldet forsvinn med ein gong, det nye kjem inn i tur og orden.
- «Toppmeny» som tredje val for TV-menyen, ved sida av «Sidemeny» og «Skjult sidemeny».
- Hopp-knappen i spelaren svarar på OK på TV.

## Ikkje-mål

- «Kven ser?» ved oppstart, bakgrunn som følgjer fokus, fast 500 ms radbyte, forstørring som
  einaste fokusmerke. Vurderte og lagde til side.
- Telefon og nettbrett får ikkje toppmeny. Breie vindauge har sidenavigasjon (docs/LAYOUT.md).
- Barnemodus har ingen meny og får ingen.
- Spelaren sin OSD held dei korte tala sine (90/140 ms); ein spelar er si eiga verd.
- `StableSheetDialog` held kurva si (ingen fjør/overshoot) og tek berre lengdene frå tokena.

## 1. Rørslespråk · `ui/theme/SpoleMotion.kt`

| Rørsle | ms | Bruk |
|---|---|---|
| `Focus` | 140 | fokus, trykk, små brytarar, ring |
| `Change` | 280 | innhald som endrar seg på staden, sidebyte i faner, tonar |
| `Sheet` | 320 inn / 200 ut | ark og panel (same som `StableSheetDialog` i dag) |
| `Land` | 450 | noko som landar: profilbiletet, kort som kjem inn |
| `Reduced` | 100 | alt, når rørsle er redusert; lineær toning |

- `motionTween(ms, easing)` gir `tween(ms, easing)`, eller `tween(100, LinearEasing)` når
  `LocalMotionEnabled` er av. Ingen stad skriv lenger `tween(if (motion) X else 0)`.
- `landSpring()` er `spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)`:
  utan overskyting, kan avbrytast.
- Rader som kjem inn: 80 ms mellom kvar, toning 220 ms lineær, glid 14 dp og 96 % storleik med
  `landSpring`. Berre `graphicsLayer`, aldri oppsettet, så fokus flyttar seg ikkje.
- Lengder som ikkje er UI-rørsle (årstidspynt, stjernehimmel, skimmer, 1 100 ms og oppover) er
  urørte.

**Test:** `SpoleMotionTest` (Robolectric): tokena har dei tala over; med redusert rørsle gir
`motionTween` 100 ms og lineær kurve.

## 2. Rader som kjem inn · `ui/components/RowEntrance.kt`

- `Modifier.rowEntrance(index, presentSince: Long?)`: `presentSince` er tidspunktet (ms) radene
  fekk lov å kome. Ei rad som blir komponert *seinare* enn sitt eige vindauge (`index × 80 + 220`
  ms etter `presentSince`) kjem utan rørsle; berre dei som er på skjermen i augneblinken får
  inngangen. `null` held rada usynleg (alpha 0).
- Heimesida tel seksjonane sine: heroen er 0, kvar rad som faktisk blir registrert får neste tal.
- `presentSince` blir sett av `ReelstackApp` når: oppstartsbiletet er borte (`StartupCover`
  seier frå), Heim-fana blir vald att, eller profilflukta har landa. Byte av fane gir inngang berre
  på Heim.
- Når rørsle er redusert: 100 ms toning, ingen forskyving.

**Test:** `RowEntranceTest` (Robolectric, `mainClock.autoAdvance = false`): ved t=0 er rad 1
usynleg, ved 80 + 220 ms er ho synleg og rad 2 på veg; ei rad komponert 2 s etter er synleg med ein
gong; med redusert rørsle ingen `translationY`.

## 3. Profilflukt · `ui/ProfileSwitchMotion.kt`

Tilstand utanfor Compose, i `ReelstackViewModel` sitt livsløp, så menyen (ein `Popup`) og
toppfeltet kan melde seg til same stad:

- `target: Rect` — profilsirkelen melder plassen sin i rotkoordinatar
  (`AccountAvatarButton` og toppmenyen sin sirkel) når han ligg øvst til høgre.
- `start(from: Rect, avatar: Avatar)` når ein profil blir vald i menyen og byte ikkje treng PIN.
  Menyen lukkar seg med ein gong (80 ms). Utan kjent mål, eller med redusert rørsle, ingen flukt:
  berre haldinga.
- Tidsline frå valet: 0 ms det gamle innhaldet tonar ut (120 ms, utan forskyving) og radene blir
  haldne; 150 ms sjølve bytet (`switchProfileNow`) — tidleg i flukta, medan biletet framleis
  rører seg mykje, så lastinga ikkje får landinga til å hakke; 450 ms biletet landar
  (`landSpring`) og sirkelen viser det nye biletet; 480 ms radene får kome. Haldinga varer aldri
  meir enn 1 500 ms uansett.
- Flukta blir teikna i eit lag over alt i `ReelstackApp`: biletet (eller bokstaven) i same storleik
  som i menyen, som glir og krympar til sirkelen. Sirkelen sjølv er usynleg frå valet til landinga.
- Menyen ligg i eit eige vindauge, så startpunktet blir rekna om frå skjermkoordinatar
  (`positionOnScreen`) til rota sine.
- Treng bytet PIN, opnar PIN-arket som før; etter PIN berre haldinga og inngangen, inga flukt.
- Byte frå Stova (`spole://profile`) gir berre haldinga og inngangen.

**Test:** `ProfileSwitchMotionTest` (rein Kotlin): rekkjefølgja over med ei testklokke; tidsgrensa;
inga flukt utan mål; ingen dobbel start. `ProfileFlightUiTest` på 5566: laget viser biletet og det
endar på sirkelen.

## 4. Toppmeny på TV

- `Personalization.tvMenuStyle: TvMenuStyle { SIDEBAR, HIDDEN_SIDEBAR, TOP_BAR }`, lagra som
  `tv_menu_style`. `hide_tv_sidebar = true` utan ny nøkkel blir `HIDDEN_SIDEBAR`. Feltet
  `hideTvSidebar` blir ein avleidd eigenskap, så kallarane ikkje treng vite.
- Utsjånad på TV: «Meny» som val-rad (`SettingsChoiceRow`) med *Sidemeny*, *Skjult sidemeny*,
  *Toppmeny*, der «Skjul sidemenyen»-brytaren står i dag.
- `ui/TvTopBar.kt`: ei line øvst, over innhaldet, 48 dp frå kantane (`TvSafeEdge`), i rot-laget
  i `ReelstackApp`. Innhald frå venstre: Spole-merket, fanene, luft, profilsirkelen.
  Faner: *Heim*, dei festa biblioteka og smarthyllene i same rekkjefølgje som i sidemenyen,
  *Oppdag* (om synleg i menyen), *Søk*. Er ingenting festa, står *Bibliotek* etter Heim.
  Innstillingar har inga fane; han ligg i profilmenyen.
- Fana som er vald, har ei roleg flate bak seg (`settingsSurface`-tonen); fokus får ring som
  elles i Spole. Ein svak skugge øvst (12 % → 0 over 120 dp) under lina på alle sider, så teksten
  er lesbar over bilete og rullande innhald.
- Lina tonar ut (`Change`) når eit ark er ope (`activeSheet != null`), og kjem att etter. Medan ho
  er usynleg, kan ho ikkje få fokus. Globalt søk: lina står, *Søk* er vald, og innhaldet er
  søkjesida.
- Fokus: Opp frå innhaldet går til lina når innhaldet ikkje har noko over seg (same krok som
  Venstre→sidemeny i dag, berre for Opp). Ned frå lina går til innhaldet: først geometrisk, elles
  `contentFocus`. Venstre/Høgre byter fane utan å opne; OK opnar. Tilbake på ei anna fane går til
  Heim som i dag.
- Med toppmeny viser heroen på Heim, Oppdag og Bibliotek ikkje sin eigen profil- eller
  søkjeknapp (ein `LocalTopBarCarriesAccount`), og `showBrand` er av.
- Sidemenyen blir ikkje komponert i det heile med toppmeny.

**Test:** `TvMenuStyleTest` (dekoding og migrering). `TvTopBarTest` (Robolectric TV): fanene over,
Bibliotek-reserven, inga Innstillingar-fane, lina usynleg og utan fokus med ark ope, Opp/Ned.
`TvTopBarUiTest` på 5566: D-pad Opp når lina, Høgre byter fane, Ned går attende til innhaldet.

## 4b. Toppmenyen etter første møte med ei ekte TV (beta32)

Eigaren såg beta31 på Loftet og sa frå: biletet skal gå *bak* menylina, ikkje starte under ho, og menyen skal vere
moderne og fleksibel. Avgjerdene:

- **Biletet bak lina.** Innhaldet er ikkje lenger polstra med lina si høgd. Heltebiletet på Heim og på biblioteksida
  tek høgda som `topInset` (biletet går til toppen, orda får `topInset` ekstra luft over seg). Sider utan helt
  startar under lina (`BelowMenu`, `LibraryScreen`), og søkesida har ei flate over heile skjermen med innhaldet under
  lina. Lina har eigen skugge (0,75 → 0 over 150 dp) så kvit tekst held over lyse bilete.
- **Fleksibel.** Oppføringane kjem frå `visibleMenu()` (`TopMenuEntries.kt`): rekkjefølgja og avslåtte sider frå
  «Sider og meny». Biblioteka står der Bibliotek står. Festa bibliotek held si rekkjefølgje; utan festing viser menyen
  biblioteka til lesaren (`LibraryCoordinator.loadMenuLibraries`, knytt til kontoen), sortert og avgrensa som
  biblioteksida. Innstillingar ligg bak profilsirkelen, søk er alltid sist. Er det mange, rullar kapselen.
- **Moderne.** Fanene ligg i ein halvgjennomsiktig kapsel mellom merket og profilsirkelen; vald side er ei fylt pille
  i aksentfargen, fokus er ein ring.
- **Fokus.** Fokus som landar på lina utan eit Opp-trykk (flisa fokus låg på forsvann) går attende til sida.
- **Bak dette:** «Neste» på biblioteksidene brukar Heim-korta, ikkje det vesle kortet med tekst ved sida.

## 4c. Toppmenyen berre øvst, liten og utan stripe (2. november, kveld)

Eigaren såg beta33 på TV: «Topp baren skulle berre ligg øverst!!!», «Topbaren skulle vere liten og nett slik som
eksempelet eg sendte deg i går» (videoen av Swiftly Player) og «Den skal ikkje lage ein MØRK bar slik som no».

- **Berre øvst.** Lina rullar vekk med sida: `TopMenuLift` (ein `CompositionLocal` frå `ReelstackApp`) får avstanden
  frå toppen av lista til sida som står framme (`LendTopMenuLift` på Heim, biblioteksida, rutenettet inne i eit
  bibliotek og smarthyllene), og lina flyttar seg like
  langt opp i lag-fasen. Når fjernkontrollen kjem til lina, står ho att heilt. Søkesida står stille under lina.
  Fordi lina følgjer sida, går ingenting bak orda, og tonen under lina (`fadeUnderMenuLine`) er fjerna. Heim og
  biblioteksida utan helt har plassen til lina øvst i lista i staden for som polstring utanfor.
- **Liten og stilrein som Swiftly.** Små ord (`titleSmall`) rett på biletet frå venstre kant, profilsirkelen (32 dp)
  til høgre. Ingen kapsel, ingen skugge-stripe og ikkje Spole-merket. Sida du er på har ei kort strek i aksenten
  under ordet; fokus er ei svak flate med ring. Orda har sin eigen mjuke skugge, så eit lyst bilete ikkje tek dei.
- **Står att:** Oppdag, Nedlastingar og Innstillingar startar framleis under lina, som
  då står fast øvst (utan stripe).

**Test:** `TvTopBarScrollTest` (Robolectric TV): lina står øvst i ro, rir opp med sida og kjem att med henne, og
fjernkontrollen hentar ho fram.

## 5. Hopp-knappen i spelaren

`SkipSegmentButton` tek fokus på TV medan han er på skjermen, men seier ikkje frå, så
`remotePlaybackAction` ser OK som «kontrollane er skjulte» og gjer pause/spel. Knappen melder
fokus til `nextHasFocus` på same måte som neste-episode-kortet.

**Test:** i `JellyfinPlayerUiTest` på 5566: med eit aktivt segment og skjult OSD går OK på knappen
til hopp, ikkje til pause.

## Rekkjefølgje

1. Rørslespråk og migrering. 2. Hopp-knappen. 3. Rader som kjem inn. 4. Profilflukt.
5. Toppmeny. 6. Release beta31 (168) etter `docs/RELEASE_WORKFLOW.md`.
