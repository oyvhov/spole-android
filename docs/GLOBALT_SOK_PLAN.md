# Globalt søk · arbeidsplan

Status 26. september 2026: **påbegynt.** Søket er ein eigen skjerm med eiga spørjing
(`GlobalSearchScreen`, `ReelstackUiState.globalSearch`), og telefonen og nettbrettet opnar det frå eit
ikon ved profilbiletet på Heim. Sjå statustabellen. Dokumentet er arbeidslista; kvar funksjon har ein
ID, ein hake og eit akseptansekrav. Hak av først når akseptansekravet er oppfylt og verifisert på
eining — ikkje når koden er skriven.

**Endra avgjerd 26. september:** brukaren bad om at søket på mobil ikkje skulle hoppe til Oppdag, og
valde «ikon øvst + val i Innstillingar». Søkelina under toppen er difor av som standard og kan slåast
på under Innstillingar → Heim. Regel 2 og GS-4 under er skrivne om etter dette.

---

## Slik står det i dag

Dette er lest ut av koden 19. september 2026, ikkje gjetta:

- Søkefeltet finst **berre inne i Oppdag** (`DiscoverScreen`), som ein rad i det lazy hovudet.
- Heimeskjermen har ein søkeknapp som hoppar til Oppdag og set fokus i feltet — men berre når
  `WindowLayoutPolicy.showHomeSearch` er sann, og den er definert som `!useNavigationRail`.
  **Over 640 dp finst det altså ingen søkeinngang i det heile** utanom å opne Oppdag manuelt.
  Det gjeld både nettbrett og TV.
- Søket dekkjer alt i dag meir enn Oppdag: `searchResults` frå Seerr og `librarySearchResults` frå
  Jellyfin/Emby ligg begge i same tilstand. Omfanget er nesten globalt; **plasseringa er det ikkje.**
- På TV tek feltet med vilje ikkje fokus, fordi ein fjernkontroll ikkje har tastatur og
  skjermtastaturet då legg seg over innhaldet med ein gong Oppdag opnar. Det er rett åtferd og skal
  ikkje reverserast.
- `AppTab` er `HOME, LIBRARY, DISCOVER, ACTIVITY, SETTINGS`. Det finst ingen søkedestinasjon.

Oppsummert: søket er ikkje globalt fordi det bur inne i ei fane, og på dei to flatene med siderad
er det praktisk talt gøymt.

---

## Kjerneregelen

> **Søk er ein destinasjon, ikkje eit felt som høyrer til Oppdag.**

Eit søk som bur i ei fane kan berre svare for den fana. Når resultatet skal kunne vere ein film i
ditt eige bibliotek, ein serie du ikkje har enno, ein episode eller ein skodespelar, må søket vere
ein eigen stad som alle flatene kan opne — og som veit kvar brukaren kom frå, slik at Tilbake fører
dit og ikkje til Oppdag.

Alt anna i planen følgjer av det.

---

## Funksjonar

| ID | Funksjon | Status | Avheng av |
| --- | --- | --- | --- |
| GS-1 | Søkedestinasjonen | ◐ Delvis: eigen skjerm og spørjing; rotasjon/prosessdrap ikkje verifisert | — |
| GS-2 | Éi søkjekjelde i ViewModel | ◐ Delvis: to `DiscoverSearchCoordinator` med kvar sin `SearchSlice` | — |
| GS-3 | Resultat i grupper | ◐ Delvis: bibliotek først, så Seerr, duplikat éin gong; episodar og personar manglar | GS-1, GS-2 |
| GS-4 | Telefoninngang | ☑ Ferdig og verifisert (emulator-5560, 26. september) | GS-1 |
| GS-5 | Nettbrettinngang | ◐ Delvis: ikonet på Heim, ikkje i siderada | GS-1 |
| GS-6 | TV-inngang | ☐ Ikkje starta | GS-1 |
| GS-7 | Tastatur og fjernkontroll | ☐ Ikkje starta | GS-6 |
| GS-8 | Tomme og feila tilstandar | ☐ Ikkje starta | GS-3 |
| GS-9 | Historikk og forslag | ☐ Ikkje starta | GS-1 |
| GS-10 | Barnemodus | ☐ Ikkje starta | GS-1 |

Statusverdiar: `☐ Ikkje starta` · `◐ Delvis` · `☑ Ferdig og verifisert`

---

### GS-1 · Søkedestinasjonen

Ein eigen skjerm, ikkje ei fane. Han blir opna oppå det du heldt på med og lagt att når du er
ferdig, slik at Tilbake alltid fører tilbake dit du kom frå.

**Reglar**

- Søket er **ikkje** ei ny oppføring i `AppTab`. Fanene er stader du bur; søk er noko du gjer.
- Skjermen eig si eiga spørjing, sine eigne resultat og sin eigen rulleposisjon.
- Å lukke søket rører ikkje Oppdag sine filter eller rulleposisjon. I dag deler dei tilstand, og
  eit søk frå Heim nullstiller det du hadde i Oppdag.

**Akseptansekrav**

- [ ] Tilbake frå søk fører til skjermen som opna det, på alle tre flatene.
- [ ] Oppdag sine filter og rulleposisjon er uendra etter eit søk opna frå Heim.
- [ ] Skjermen overlever rotasjon og prosessdrap med spørjing og resultat i behald.

**Filer:** ny `ui/search/SearchScreen.kt`, `ui/ReelstackApp.kt`

---

### GS-2 · Éi søkjekjelde i ViewModel

Søket spør fleire tenester samtidig og skal svare så snart den første svarar.

**Reglar**

- Eige bibliotek (Jellyfin og Emby) og Seerr/TMDB blir spurde parallelt, ikkje etter kvarandre.
- Ei teneste som feilar eller er treg skal ikkje halde tilbake dei andre sine resultat.
- Ny spørjing kansellerer den førre. Debounce, og aldri send ei spørjing på under to teikn.
- Resultat frå ei avbroten spørjing skal aldri skrivast inn i tilstanden.

**Akseptansekrav**

- [ ] Resultat frå eige bibliotek kjem uavhengig av om Seerr svarar.
- [ ] Ei teneste som er nede gir ei linje om akkurat den tenesta, ikkje ein tom skjerm.
- [ ] Rask skriving gir eitt nettverkskall per pause, ikkje eitt per teikn.
- [ ] Einingstestar dekkjer debounce, kansellering og samanslåing av kjelder.

**Filer:** `ui/ReelstackViewModel.kt`, `data/repository/MediaSyncRepository.kt`

---

### GS-3 · Resultat i grupper

Éi liste som blandar «finst hos deg» og «kan bestillast» tvingar brukaren til å lese kvart kort for
å finne ut kva eit trykk gjer.

**Rekkjefølgje**

1. **I biblioteket ditt** — spelbart no. Alltid øvst.
2. **Episodar** — når spørjinga treffer ein episodetittel.
3. **Personar** — skodespelar eller regissør, når tenesta gir det.
4. **Kan bestillast** — Seerr/TMDB.

**Akseptansekrav**

- [ ] Ei gruppe utan treff blir ikkje teikna i det heile — ingen tomme overskrifter.
- [ ] Eit kort seier tydeleg om eit trykk **spelar** eller **opnar ein førespurnad**.
- [ ] Same tittel i både bibliotek og Seerr blir vist éin gong, i biblioteksgruppa.
- [ ] Rekkjefølgja er lik på alle tre flatene.

**Filer:** ny `ui/search/SearchResults.kt`

---

### GS-4 · Telefoninngang

**Frå 26. september:** eit søkeikon ved profilbiletet øvst på Heim er inngangen. Søkelina under
toppen er eit val i Innstillingar → Heim («Søkelina på framsida», av som standard). Begge fører til
søkedestinasjonen (GS-1), ikkje til Oppdag.

**Akseptansekrav**

- [x] Ikonet står ved profilbiletet; søkelina finst berre når ho er vald.
- [x] Eit trykk opnar søket med tastaturet oppe og markøren i feltet.
- [x] Tilbake lukkar søket og fører til Heim, ikkje til Oppdag.

**Filer:** `ui/ReelstackApp.kt`, `ui/screens/HomeScreen.kt`

---

### GS-5 · Nettbrettinngang

I dag forsvinn søkeinngangen fullstendig over 640 dp, fordi `showHomeSearch` er `!useNavigationRail`.
Det er ein feil, ikkje eit val: eit større vindauge har **meir** plass til søk, ikkje mindre.

**Løysing:** søk i siderada, over fanene, med same ikon som i dag. Rada er alt der og har fokus- og
utvidingsåtferd som fungerer.

- Utvida siderad: ikon og ordet «Søk».
- Samanslegen siderad: berre ikonet, med `contentDescription`.
- Søket opnar som eit panel over innhaldet, ikkje som ei ny side, slik at konteksten bak står.

**Akseptansekrav**

- [ ] Søk er nåbart frå kvar fane på nettbrett, utan å opne Opptak eller Oppdag først.
- [ ] Ved 2× skriftskala blir ordet «Søk» ikkje klipt, og rada fell ikkje saman.
- [ ] Eit fysisk tastatur kan opne søket med `Ctrl+F` og lukke det med `Esc`.
- [ ] Panelet held seg innanfor den lesbare kolonnen (`ReelPage`), ikkje strekt over heile breidda.

**Filer:** `ui/ReelstackApp.kt` (`ReelstackNavigationRail`), `ui/layout/WindowLayoutPolicy.kt`

---

### GS-6 · TV-inngang

Same problem som nettbrett, men strengare krav. Frå tre meters hald med ein fjernkontroll er søk
den einaste måten å finne noko spesifikt på; i dag må ein gå til Oppdag og finne eit felt som med
vilje ikkje tek fokus.

**Reglar**

- Søk får ei **eiga oppføring øvst i siderada**, over Heim. Det er den einaste oppføringa som ikkje
  er ei fane, og han skal difor stå åtskild med ei linje under.
- D-pad **Opp** frå den øvste innhaldsrada går til søk. Det er den bevegelsen ein gjer utan å tenkje.
- Skjermtastaturet kjem **ikkje** av seg sjølv. Det er den eksisterande regelen i `DiscoverScreen`
  og den skal bevarast: brukaren vel feltet, og då kjem tastaturet.
- Resultatrutenettet er fokuserbart med D-pad utan å gå gjennom feltet på nytt.

**Akseptansekrav**

- [ ] Søk er nåbart med maks to trykk frå kvar fane.
- [ ] Skjermtastaturet dukkar aldri opp utan at brukaren valde feltet.
- [ ] Fokusramma følgjer TV-regelen i resten av appen, og er aldri usynleg mot ein lys plakat.
- [ ] Tilbake frå resultat går til feltet; Tilbake frå feltet lukkar søket.
- [ ] Fysisk Tilbake lukkar aldri appen frå søkeskjermen.

**Filer:** `ui/ReelstackApp.kt`, ny `ui/search/`

---

### GS-7 · Tastatur og fjernkontroll

**Akseptansekrav**

- [ ] Talknappane på fjernkontrollen skriv teikn der tenesta støttar det.
- [ ] Stemmesøk blir brukt når Google TV tilbyr det, og feltet blir fylt med resultatet.
- [ ] Eit fysisk tastatur på nettbrett kan navigere resultata med piltastar og opne med Enter.
- [ ] Ingen snarveg kolliderer med ein eksisterande snarveg i appen.

**Filer:** `ui/search/`, `MainActivity.kt`

---

### GS-8 · Tomme og feila tilstandar

**Akseptansekrav**

- [ ] Før første teikn: ingen tom liste, men noko nyttig — sjå GS-9.
- [ ] Ingen treff: seier kva som vart søkt etter og foreslår å bestille tittelen, når Seerr finst.
- [ ] Teneste nede: éi linje som namngir tenesta. Dei andre gruppene står.
- [ ] Ingen feilkode og ingen stakksporing i teksten.

**Filer:** `ui/search/SearchEmptyStates.kt`

---

### GS-9 · Historikk og forslag

Eit tomt søkefelt er ein bortkasta skjerm.

- Dei siste søka dine, lokalt på eininga, med ein måte å tømme dei på.
- Under: det du har halde fram med, som snarveg.

**Akseptansekrav**

- [ ] Historikken er lokal og blir aldri send til nokon tenar.
- [ ] Historikken er per profil — eit barn sine søk dukkar ikkje opp hos forelderen.
- [ ] Den kan tømmast, og tømminga er umiddelbar.

**Filer:** `data/repository/AppPreferencesRepository.kt`, `ui/search/`

---

### GS-10 · Barnemodus

BM-5 seier rett ut: **ingen søkefelt** i barneskalet.

**Akseptansekrav**

- [ ] Søkedestinasjonen kan ikkje opnast frå `KidsApp`, korkje med trykk, D-pad eller tastatur.
- [ ] Ingen søkesnarveg er aktiv medan barnemodus er på.

**Filer:** `ui/kids/KidsApp.kt`

---

## Reglar som gjeld heile funksjonen

1. **Søk er ein destinasjon, ikkje ei fane.** Ingen ny `AppTab`.
2. **På telefon er ikonet ved profilbiletet inngangen** (brukaren si avgjerd 26. september). Søkelina
   er eit val, ikkje standard.
3. **Skjermtastaturet kjem aldri av seg sjølv på TV.**
4. **Eige bibliotek står alltid øvst.** Det brukaren alt har, er det mest nyttige svaret.
5. **Ei treg teneste held aldri tilbake ei rask.**
6. **Ingen søkjelogg forlèt eininga.**
7. **Ingen søk i barnemodus.**

---

## Verifisering før ein hake blir sett

- JVM-testar og `lintDebug` grønt.
- Køyrt på **telefon, nettbrett og Google TV** med ekte data. Nettbrett kan emulerast med eit
  vindauge over 900 dp; TV krev den innlogga TV-profilen.
- Skriftskala 2.0, ikkje 1.5.
- Ingen skjermbilete med ekte kontonamn eller bibliotekinnhald i repoet.

---

## Kva som ikkje er avgjort

- Om personsøk skal vere med i første versjon, eller vente til gruppene er stabile.
- Om nettbrettpanelet skal vere modalt eller ein tredje kolonne i landskap.
- Om søket skal dekkje Radarr/Sonarr sine køar, eller berre bibliotek og Seerr.
