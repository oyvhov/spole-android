# Hyller, spor og rekkjefølgje — 11. september 2026

Ni punkt frå brukaren, alle bygde og kontrollerte på den innlogga TV-emulatoren
(`Spole_GoogleTV_Test`, `emulator-5564`) med ekte Jellyfin- og Seerr-data. Dei tre siste punkta
kom undervegs, etter at brukaren såg dei første seks på skjermen.

Den siste APK-en er **minifisert** — `isMinifyEnabled = true`, R8 og ressurskrymping på. Det er
første gongen ein R8-bygg er køyrt med emulatoren oppe; han overlevde, men marginen på minne var
liten, så det er ikkje noko som bør gjerast som vane.

## 1. Søkefeltet i Oppdag skal vere der, men ikkje ta fokus

Feltet var heilt borte på TV etter førre runde. Det løyste tastaturproblemet, men tok bort
det brukaren faktisk ville ha.

Årsaka til at feltet tok fokus er ikkje feltet: skalet kallar `contentFocus.requestFocus()`
kvar gong ein fane blir opna, og det landar på det første fokuserbare elementet i innhaldet.
Å verne om snarvegen eller la filterrada be om fokus først tapte begge mot dette.

På TV er boksen no ein **knapp** med nøyaktig same form, fyll, kant og plassering som feltet.
Å fokusere ein knapp hentar ikkje tastaturet. Vel du han, byter han til det verkelege feltet,
fokusert og med tastatur — det einaste tidspunktet ein fjernkontrollbrukar vil ha noko av det.
Enter i feltet lukkar det igjen, så tastaturet følgjer med ut.

Boksen ligg ved sida av overskrifta på ein brei TV-topp, altså langt til høgre for filterchipsa.
Ingenting ligg rett over dei, så eit Opp-trykk fann ingenting i det heile. `focusProperties { up }`
peiker no på boksen, og boksen peiker ned att på filterrada.

Kontrollert: `mInputShown=false` ved opning av Oppdag, fokus på «Alt».

## 2. «Sjå førespurnadshistorikk» på same linje som filtera

Han låg på si eiga linje over «Alle · 104 / På veg · 25 / Klare · 79», som ein `TextButton`
mot tre `FilterChip`. To knappetypar på to linjer for det som er éi rad med kontrollar.

No: same chipfamilie, same høgd, same form, på same linje — men etter ein breiare glipe og med
ein pil. Filtera endrar sida; denne forlèt ho. `AppNavigationChip` i `AppControls.kt`.

## 3. Bibliotek i menyen kan flyttast

`libraryShortcuts` var ei liste, men rekkjefølgja kom frå tenaren:
`libraryChoices.filter { it.id in shortcuts }`. Uansett kva ein festa når, kom menyen ut i
Jellyfin si rekkjefølgje.

`saveLibraryChoices` tek no `List<String>` i staden for `Set<String>` og bruker den lista ho får.
Dialogen «Vel bibliotek» viser «Rekkjefølgje i menyen» med opp/ned-pilar når to eller fleire er
festa — same mønsteret som menyrekkjefølgja i Innstillingar, som brukaren kjenner frå før.

## 4. Biblioteka har eigne hyller

Heim blandar alle biblioteka og tek tolv kort ut av heile haugen. Det er rett for Heim og feil
for ei bibliotekside: opnar du Filmar, vil du sjå filmane du ikkje har sett ferdig, ikkje tre av
dei bak to episodar frå eit anna bibliotek.

`MediaServerClient.libraryShelves` gjer same førespurnaden som feeden, med **eitt** `view`.
Kvar bibliotekside hentar sine eigne «Hald fram å sjå» og «Neste episode» når ho blir opna, og
berre på rota av eit bibliotek — inne i ein serie er rutenettet allereie episodelista.

Hyllene er skjulte når eit filter er aktivt. Ei filtrert side er eit søkeresultat, og «kva såg du
sist» er ikkje ein del av svaret på eit søk.

### Handlingane på korta

Hald inne eit kort (fingeren, eller OK på fjernkontrollen) for:

| Val | Kva som skjer |
| --- | --- |
| Fjern frå Hald fram å sjå | `PlaybackPositionTicks = 0` på tenaren |
| Legg til / fjern favoritt | `UserFavoriteItems` |
| Marker som sett / usett | `UserPlayedItems`, og kortet forlèt hylla |

Alt går til Jellyfin først og skjermen følgjer etter. Ei lokal «skjult»-liste ville vore usamd
med alle andre klientar og forsvunne ved ny installasjon.

`clearResume` prøver tre former i rekkjefølgje, og berre «denne tenaren har ikkje det
endepunktet» (400/404/405) går vidare til neste: `UserItems/{id}/UserData` (Jellyfin 10.9),
`Users/{user}/Items/{id}/UserData` (eldre Jellyfin og Emby), og til slutt ei stopp-melding på
posisjon null — nøyaktig det ein klient som spelte fila ferdig ville sendt. Ei avvist innlogging
betyr det same på alle tre; sju testar i `ResumeShelfTest`.

Hald inne er den eine rørsla ein fjernkontroll og ein finger deler. Ho kostar ikkje eit einaste
fokusstopp på TV, og hylla har ei linje som seier frå.

## 5. Elementsidene på TV

Sida sa kor lenge filmen varte og kva kodek han hadde, men ikkje kva språk han var på. Spørsmålet
ein husstand faktisk stiller kunne berre svarast ved å starte filmen og opne spelarmenyane.

- **Lydspor**, **Undertekstar** og **Versjon** står på sida, med tenaren sitt eige straumnummer.
  Valet følgjer med inn i `JellyfinPlayerActivity` som ekstra, og gjeld første `prepare` — etter
  det er spelaren sine eigne menyar sjefen.
- Ein film med 43 tekstspor står som éi linje med den valde verdien. Sjå punkt 9.
- **Marker som sett** og **Legg til i favorittar** står på sjølve sida. Spole kunne filtrere på
  begge frå før og setje ingen av dei.

### To feil som kom fram undervegs

**Eit kort utan tittel.** Jellyfin kan svare med `"SeriesName": ""`, og tom streng er ikkje eit
namn: `series ?: name` gav tom tittel, så eitt kort på hylla — og heile tittelsida hans — stod
utan tittel. Blankt tel som fråverande no, og då fell tittelen tilbake på namnet til elementet.

**Året i episodelinja.** Produksjonsåret vart lagt til når det ikkje fanst noko serienamn. Med
retten over gav det «Sesong 19 · Episode 9 – 2025», som les som om året var episodenamnet. Året
høyrer til ein film eller ein serie, aldri til ein episode.

## 6. «S19 E09» skrive ut

Helten fekk dette i førre runde. Kortlister, rutenett og detaljsider stod framleis med
filnamnet. `episodeLine(season, episode, subtitle)` i `EpisodeLabel.kt` er no felles for alle
fire, og helten brukar same `episodeTitle` til å ta av prefikset.

Ein episode importert utan tittel heiter «Episode 9» hos Jellyfin, og ein med tittel heiter ofte
«Episode 9 - Getaway Sticks». Begge gjentek eit tal linja alt har sagt, så prefikset går av —
men berre når det er nøyaktig det talet, elles ville «Episode 9» ete starten på «Episode 90».
Ni testar i `EpisodeLabelTest`.

Resultat på skjermen:

| Før | Etter |
| --- | --- |
| `S19 E09 · Episode 9 - Getaway Sticks` | `Sesong 19 - Ep 9` |
| `S03 E01 · Episode 1` | `Sesong 3 - Ep 1` |
| `S06 E11 · Hvilken Side Er Du På? – Del 1` | `Sesong 6 - Ep 11 · Hvilken Side Er Du På? – Del 1` |

Første forsøk skreiv «Sesong 3 · Episode 1» heilt ut. Brukaren ville ha det kortare: sesongen og
episoden høyrer til kvarandre og blir bundne med bindestrek, namnet er ei anna sak og får sitt eige
merke. Helten har plass og skriv framleis «Episode 13» i fullt ord på si eiga linje.

## 7. Ingen ring rundt helten

Heile helten var klikkbar, og på TV var det den som tok fokus. Den einaste måten å vise det på var
ein ring rundt heile biletet — altså nøyaktig det som fekk framsida til å sjå ut som eit merkt felt
i eit rekneark i staden for eit stykke kunst.

På TV er ikkje flata klikkbar lenger, så ho kan ikkje ta fokus. «Sjå meir»-knappen inni er
fokusmålet: fjernkontrollen når den same tittelen gjennom ein ring på storleik med ein knapp. På
berøring opnar eit trykk kvar som helst på helten framleis tittelen.

## 8. Mindre på tittelsida

Sida var tretten stabla blokker. Etter: tittel, éi linje, éi handlingsrad, éi metadatalinje, to
sporlinjer, omtale.

| Før | Etter |
| --- | --- |
| Full knapp «Hald fram», eiga framdriftslinje og eiga «70 % sett · 16 min att» | «Hald fram · 16 min att» med framdrifta inni knappen |
| To chips med «Marker som sett» og «Legg til i favorittar» | To runde ikon: hake og hjarte |
| Sju opphøgde plater: `Episode` `S03 E01` `2026` `51 min` `720p` `H264` `EAC3 5.1` | Éi dempa linje: `2026 · 51 min · 720p · H264 · EAC3 5.1` |
| «Valt her, brukt når avspelinga startar.» | fjerna |
| «Lydspor» som ein markert chip det ikkje gjekk an å trykkje på | «Lydspor  English - Dolby Digital Plus …» som tekst |
| 230 dp episodebilete mot ein meter svart | 340 dp, avgrensa av `maxWidth * .34f` |

Grunngjevinga bak kvar av dei: framdrifta høyrer til kontrollen ho gjeld, og ei linje med prosent
under ein knapp som alt heiter «Hald fram» seier det same to gonger på den høgaste plassen på sida.
Eit hjarte er ein favoritt over alt — forma er etiketten, og teksten høyrer til skjermlesaren.
Medietypen og episodekoden stod alt to gonger lenger oppe. Eitt spor er ein opplysning, ikkje eit
val: ein einsleg markert chip ser ut som noko ein skal trykkje på og gjer ingenting.

Hjarte finst i to former i `SpoleIcons` — `Heart` med strek og `HeartFilled` fylt. To former, ikkje
éi form i to fargar: på ein mørk side er ein aksentfarga strek og eit aksentfarga fyll same kulør,
og då er nokre piksler blekk alt som skil «er favoritt» frå «kan bli det».

## 9. Undertekstlista er éi linje

Silo har 43 tekstspor. Første forsøk viste sju og resten bak «37 til»; andre forsøk fem og «39 til».
Begge fylte framleis to rader med plater ingen les, og brukaren sa rett ut at det ikkje var godt nok.

Svaret på «kva språk er dette på» er éin verdi. Linja seier den verdien og held resten bak den eine
kontrollen som spør etter dei:

```
Undertekstar   English - SUBRIP   [Endre]
```

«Endre» opnar ei liste med alle 43, der den valde har ein hake i aksentfargen i staden for eit
fyll — ei liste på førti skal lese som ei liste, ikkje som førti knappar i to fargar. Same mønster
som sjanger- og årsvala i bibliotekfiltera, som allereie er prøvde med fjernkontroll.

Lydsporet får den same behandlinga når det finst meir enn eitt; med berre eitt står det som tekst,
fordi eitt spor er ei opplysning og ikkje eit val.

## Kontroll

- 400 einingstestar i 49 klassar, 0 feil. 27 nye: `ResumeShelfTest` (7), `MediaTrackTest` (11),
  `EpisodeLabelTest` (9).
- Lint: 0 feil, 46 åtvaringar — same tal som før denne bolken. `tv_progress` og
  `tv_progress_percent` er fjerna saman med blokka som brukte dei.
- Kontrollert på `emulator-5564` med ekte data: Oppdag utan tastatur, historikkchipen på
  filterlinja, rekkjefølgja i «Vel bibliotek» (avbroten utan å lagre), hyllene i Seriar,
  hald-inne-menyen, ein favoritt sett og teken bort att, og tittelsida til Silo med lydspor og
  43 tekstspor.

## Står att

- Instrumenterte testar er framleis aldri køyrde.
- Listevisinga (`LibraryView.LIST`) og rulleposisjon ved skifte av bilettype er framleis ikkje
  kontrollerte på skjerm.
- Versjonslinja på tittelsida viser kva versjonar som finst, men kan ikkje brukast til å velje
  mellom dei enno.
