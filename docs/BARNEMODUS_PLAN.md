# Barnemodus · arbeidsplan

Status: **ikkje starta.** Ingenting i denne planen er implementert.

BM-1 til BM-4 vart prototypa 11. september 2026 og **fjerna att same dag** på brukaren si melding.
Koden kompilerte og 356 einingstestar var grøne, men ingenting vart sett på ei eining. Det som vart
lært, er teke vare på i planen under:

- Profilprefiks i `ConnectionRepository` fungerer. Tom id gir nøyaktig den gamle nøkkelen, så ei
  oppgradering er trygg. Prefikset må inn i `tokenCache`-nøkkelen òg, ikkje berre i lagringsnøkkelen.
- Cacheskiljet treng inga eiga fil: legg profil-id-en inn i `AppContainer.mediaFingerprint`.
- Feeden byter ikkje av seg sjølv. Tre kall til `connectionRepository.list()` i `ReelstackViewModel`
  må ta profil-id, elles viser barneprofilen forelderen sitt innhald.
- KSP sin inkrementelle cache må tømmast etter at `AppContainer` får eit nytt felt; elles feilar
  bygget med «this and base files have different roots».

Denne fila er arbeidslista for barnemodus. Kvar funksjon har ein ID, ein hake og eit
akseptansekrav. Hak av først når akseptansekravet er oppfylt og verifisert — ikkje når koden er
skriven. Oppdater `Status`-kolonna i tabellen samtidig.

Målgruppe: **barn mellom 4 og 9 år.** Éin modus, ikkje to. Ein fireåring les ikkje, ein niåring vil
velje episode sjølv; utforminga må tole begge utan eit alderval.

---

## Kjerneregelen

> **Barnemodus loggar Spole inn på barnet sin eigen Jellyfin-konto. Appen filtrerer ingenting.**

Eit barnemodus som gøymer innhald i klienten, held berre til ungen finn brytaren. Ein eigen
Jellyfin-konto med eigne bibliotek *har* ikkje det andre innhaldet — omstart, ny app eller
skjermtrykk endrar ingenting, fordi tenaren ikkje svarar med noko anna uansett kven som spør.

Alt anna i denne planen følgjer av det valet. **Ingen funksjon som filtrerer innhald i klienten
skal byggjast.** Finst det ingen barnekonto, skal barnemodus ikkje kunne slåast på.

---

## Verifisert føresetnad i dette oppsettet

Kontrollert 11. september 2026 mot dette prosjektet og denne brukaren sitt oppsett:

- Brukaren har alt **eigne Jellyfin-kontoar for barna** og eigne bibliotek
  (`Barnefilmar`, `Barneseriar`).
- **Spole treng difor ikkje administratorrettar.** Eit tidlegare utkast føreslo å lage kontoen og
  setje policy frå appen gjennom `POST /Users/New` og `POST /Users/{id}/Policy`. Det er fjerna.
  Bibliotekavgrensinga er alt gjord på tenaren, og Spole skal berre logge inn.
- Konsekvens: heile oppsettsvegvisaren fell bort. Att står «vel ein konto og logg inn éin gong».

Dette er den viktigaste forenklinga i planen. Ikkje legg admin-kall inn att.

---

## Funksjonar

| ID | Funksjon | Status | Avheng av |
| --- | --- | --- | --- |
| BM-1 | Profildimensjon i lagringa | ☐ Ikkje starta | — |
| BM-2 | Profilbyte frå ikonet | ☐ Ikkje starta | BM-1 |
| BM-3 | Legg til profil | ☐ Ikkje starta | BM-1 |
| BM-4 | PIN og utgang | ☐ Ikkje starta | BM-2 |
| BM-5 | Barneskalet | ☐ Ikkje starta | BM-1 |
| BM-6 | Framsida for barn | ☐ Ikkje starta | BM-5 |
| BM-7 | Episoderutenett | ☐ Ikkje starta | BM-5 |
| BM-8 | Barnespelaren | ☐ Ikkje starta | BM-5 |
| BM-9 | Verdsromlaget | ☐ Ikkje starta | BM-6 |
| BM-10 | Nettverksinnstramming | ☐ Ikkje starta | BM-5 |
| BM-11 | Skjermfesting | ☐ Ikkje starta | BM-4 |
| BM-12 | Tid og leggjetid | ☐ Ikkje starta | BM-8 |
| BM-13 | TV-tilpassing | ☐ Ikkje starta | BM-6 |

Statusverdiar: `☐ Ikkje starta` · `☐ Ikkje starta` · `☑ Ferdig og verifisert`

---

### BM-1 · Profildimensjon i lagringa

`ConnectionRepository` nøklar alt på tenestetypen — `"jellyfin.token"`, `"jellyfin.user_id"` — og
`list()` er `ServiceKind.entries.map(::get)`. Modellen held éi tilkopling per teneste. Barnemodus
treng fleire Jellyfin-identitetar på same tenar.

**Løysing:** profilprefiks framfor den eksisterande nøkkelen. Tom prefiks for den vaksne
kontoen, slik at alt som alt er lagra held fram uendra. `kid.<id>.` for kvar barneprofil.

**Akseptansekrav**

- [ ] Ein eksisterande installasjon som oppgraderer, mistar ikkje innlogginga si.
      Tom profil-id gir nøyaktig den gamle nøkkelen; dekt av `ProfileStorageTest`.
- [ ] To Jellyfin-teikn mot same `baseUrl` kan lagrast samtidig utan å overskrive kvarandre.
- [ ] `signOut` på ein profil rører ikkje dei andre. Adressa blir framleis hugsa per tenar.
- [ ] Aktiv profil overlever omstart av appen.
- [ ] `MediaSnapshotStore` er skild per profil — løyst ved at profil-id-en går inn i
      `mediaFingerprint`, ikkje ved ei eiga fil. Same verknad, mindre kode. Deler dei fil, blafrar feil innhald
      forbi i eit halvsekund ved profilbyte, og akkurat det halve sekundet er heile poenget.
- [ ] Einingstestar dekkjer prefiksnøkling og migrering frå gammal nøkkel.
- [ ] **Att:** isolert utlogging er koda, men ikkje testa — det krev Android-kontekst, og
      prosjektet har ingen Robolectric. Høyrer heime i `androidTest`.

**Filer:** `data/repository/ConnectionRepository.kt`, `data/repository/MediaSnapshotStore.kt`,
`ui/ReelstackViewModel.kt`

---

### BM-2 · Profilbyte frå ikonet

Profilbiletet øvst til høgre er alt ein 48 dp knapp med fokusramme
(`AccountAvatarButton`, brukt på Heim, Oppdag og i nettbrettfunksjonen). I dag opnar eit trykk
Innstillingar. Det skal i staden opne profilveljaren.

**Åtferd**

- Trykk på ikonet → panel med profilane. Gjeldande profil er merkt.
- Trykk på ein barneprofil → byter med ein gong, ingen kode.
- Trykk på den vaksne profilen frå barnemodus → krev PIN (BM-4).
- Nedst i panelet: `Innstillingar` og `Legg til profil`. Innstillingar er ikkje synleg i barnemodus.
- Byte skal **ikkje** vise ei innloggingsskjerm. Teikna er alt lagra; byte er å skifte kva for eitt
  som er aktivt.

**Akseptansekrav**

- [ ] Byte frå vaksen til barn tek eitt trykk og under to sekund til framsida er teikna.
      **Ikkje målt** — krev eining.
- [ ] Framsida les den aktive profilen sine tilkoplingar. Alle tre
      `connectionRepository.list()`-kalla i ViewModel tek no profil-id.
- [ ] Forelderen sin «hald fram» er uendra når ein byter tilbake — eigne nøklar og eigen cache.
- [ ] Panelet har 64 dp rader, fokusramme og `Role.RadioButton` per rad.
- [ ] Ingen token blir vist i grensesnittet.
- [ ] `AccountAvatarButton` er urørt; berre `onClick` i `ReelstackApp` endra seg.

**Filer:** `ui/components/AccountAvatar.kt` (uendra),
`ui/ReelstackApp.kt` (onAccountClick), nytt `ui/components/ProfileSwitcher.kt`

---

### BM-3 · Legg til profil

Éin gong per barn. Ingen admin-rettar, ingen policy-oppsett.

**Flyt**

1. `GET /Users/Public` mot den lagra adressa gir namn, id og profilbilete for kontoar som ikkje er
   gøymde. Dette er det einaste kallet som verkar **utan** innlogging, og difor rett her.
2. Brukaren vel eit namn frå lista.
3. `HasPassword` i svaret avgjer om passord blir spurt om. Ein barnekonto utan passord gir
   innlogging med ein gong.
4. `POST /Users/AuthenticateByName` lagrar teiknet under profilprefiksen frå BM-1.

**Akseptansekrav**

- [ ] `publicUsers()` og parsinga er på plass, med `HasPassword` og avatar-URL.
      Sju testar dekkjer normalsvar, `Items`-innpakking, deaktiverte kontoar og ugyldig JSON.
- [ ] Spole ber aldri om administratornøkkel for denne flyten.
- [ ] Spole endrar **aldri** brukarpolicy på tenaren.
- [ ] Ein profil kan fjernast utan å røre dei andre (`ConnectionRepository.deleteProfile`).
- [ ] **Att:** sjølve oppsettskjermen er ikkje bygd. Kallet og modellen finst; UI-en manglar, og
      dermed også fallback til manuelt brukarnamn når kontoen er gøymd.

**Filer:** `data/network/ServiceClients.kt` (nytt `publicUsers()`),
`data/network/ServicePayloadParser.kt`, ny oppsettskjerm

---

### BM-4 · PIN og utgang

**Åtferd**

- Inn i barnemodus: eitt trykk, ingen kode.
- Ut: firesifra PIN, kryptert på eininga. Ikkje Jellyfin-passordet.
- Gløymd PIN: Jellyfin-passordet til den vaksne kontoen. Det knyter attkomsten til noko som finst.
- Feil kode: aukande ventetid 5 s → 15 s → 60 s. **Aldri** lås av eininga.
- I barnemodus finst ingen synleg «avslutt»-knapp. Utgangen er profilikonet.

**Akseptansekrav**

- [ ] PIN ligg i `EncryptedTokenStore`, SHA-256-hasha med eit einingslokalt salt — aldri i klartekst.
- [ ] PIN blir aldri logga.
- [ ] Ventetida overlever omstart. Ho ligg i SharedPreferences som eit absolutt tidspunkt, fordi
      den opplagde vegen rundt ei venting er å starte appen på nytt.
- [ ] Skjermlesar får talet på siffer, ikkje sifra.
- [ ] **Att:** Heim og Oversikt kjem framleis ut av appen. Det er BM-11 (skjermfesting), ikkje
      løyst her.
- [ ] **Att:** å *lage* koden første gong har ingen skjerm. `setPin` finst; UI-en manglar.
- [ ] **Att:** «Gløymd kode» via Jellyfin-passordet er ikkje bygd.

**Filer:** `data/security/`, `ui/ReelstackApp.kt`, ny PIN-skjerm

---

### BM-5 · Barneskalet

Eit eige skal ved sida av `ReelstackApp`, ikkje eit vilkår inne i det. Fem faner og ei siderad er
ikkje noko eit barn skal navigere.

**Reglar**

- Ingen botnbar, ingen siderad, ingen faner.
- **Ingen detaljpanel.** Eit trykk på ein plakat spelar. Dette er den viktigaste strukturelle
  regelen i heile planen.
- Maks navigasjonsdjupn 3: Framside → Episodar → Spelar.
- Ingen tenestemerke, ingen omtale, ingen rolleliste, ingen søkefelt.

**Storleikar** (desse er krav, ikkje framlegg)

| Element | Vaksen | Barn |
| --- | --- | --- |
| Minste trykkflate | 48 dp | **64 dp** |
| Korttittel | 14 sp | **17 sp** |
| Seksjonstittel | 21 sp | **26 sp** |
| Spel/pause | 52 dp | **74 dp** |
| Fokusramme på TV | 3 dp, ingen skala | **6 dp + 1,05×** |

Barnemodus er ikkje eit nytt designsystem. Det er ein forhåndsinnstilling av personaliseringa som
alt finst: `ArtworkSize.LARGE`, `ArtworkCorners.ROUND`, `FocusStyle.BOLD` og barnet sin valde
aksent frå `AccentPalette`.

**Akseptansekrav**

- [ ] Ingen skjerm i barnemodus har meir enn tre nivå til spelaren.
- [ ] Ingen kontroll er under 64 dp.
- [ ] Ved 2× skriftskala blir ingenting klipt og ingen linje falle bort.
- [ ] Skalet deler ikkje `AppTab`-enumet med den vaksne appen.

**Filer:** ny `ui/kids/KidsApp.kt`, `ui/theme/` (barnepreset)

---

### BM-6 · Framsida for barn

To rader. Ikkje fleire.

1. **Hald fram** — `UserItems/Resume` slått saman med `Shows/NextUp`. Store 16:9-kort med
   framdriftslinje på kunstflata si eiga botnkant.
2. **Seriane dine** — rutenett, to per rad på telefon.

Ingen tilrådingar, ingen komande utgjevingar, ingen kalender, ingen nye-utgjevingar-rad.

**Akseptansekrav**

- [ ] Framsida har aldri meir enn to rader.
- [ ] Eit trykk kvar som helst i «Hald fram» startar avspeling direkte.
- [ ] Tom tilstand skuldar ikkje på barnet og nemner ikkje tenester eller feilkodar.
- [ ] Rutenettet fungerer med finger og med D-pad.

**Filer:** ny `ui/kids/KidsHomeScreen.kt`

---

### BM-7 · Episoderutenett

For dei større i aldersspennet. Rutenett med stillbilete og store nummer.

**Akseptansekrav**

- [ ] Sette episodar er dempa, den neste er tydeleg markert.
- [ ] Eit trykk spelar. Det finst ikkje eit steg mellom.
- [ ] Nummeret er lesbart for ein sjuåring — minst 15 sp, høg vekt.
- [ ] Ingen sesongveljar når serien har éin sesong.

**Filer:** ny `ui/kids/KidsEpisodesScreen.kt`

---

### BM-8 · Barnespelaren

Tre kontrollar: spol attende, spel/pause, spol fram. Ingenting meir.

**Akseptansekrav**

- [ ] Ingen meny for lydspor, undertekst eller kvalitet. Desse er sette av forelderen.
- [ ] Tidslinja er minst 7 dp høg med eit handtak på minst 19 dp.
- [ ] Autospel av neste episode, men **maks tre på rad**, så eit roleg «Vil du sjå meir?» som krev
      eit trykk.
- [ ] `NativeClientLauncher` er av. Dette er bakdøra ut av barnemodus.
- [ ] Framdrift blir rapportert til Jellyfin på barnet sin konto, ikkje forelderen sin.

**Filer:** `player/JellyfinPlayerActivity.kt` (barneflagg),
`ui/components/NativeClientLauncher.kt`

---

### BM-9 · Verdsromlaget

Temaet er **tapetet, ikkje møblane.** Verdsrommet lever i dei dekorative flatene og aldri i
innhaldet eller kontrollane.

**Kvar det får vere**

- Ein roleg stjernehimmel bak framsida. Låg kontrast, ingen rørsle som stel merksemd.
- Profilbileta er planetar — ein sirkel i barnet sin aksentfarge, éin ring, ein måne.
- Lastskjeletta blinkar som stjerner i staden for å skimre grått. `ShimmerBlock` har alt animasjonen.
- «Ferdig for i dag» er ein rakett som har landa, med måne.
- Tom tilstand er ein tom planet, ikkje ei feilmelding.

**Kvar det ikkje får vere**

- [ ] Ikkje på plakatane. Dei er ekte innhald og skal ikkje pyntast.
- [ ] Ikkje på kontrollane. Spel/pause er spel/pause.
- [ ] Ingen lydeffektar.
- [ ] Ingen maskot som snakkar til barnet.
- [ ] Ikkje ein regnbogepalett. Barnet sin eine aksent held.

**Akseptansekrav**

- [ ] Stjernehimmelen kostar ikkje meir enn 1 ms per ramme og stoppar når skjermen er i bakgrunnen.
- [ ] Dekorasjonen har inga skjermlesarbeskriving.
- [ ] Slår ein av systemanimasjon, står stjernene stille.
- [ ] Kontrasten på tekst over stjernehimmelen er framleis minst 4,5:1.

**Filer:** ny `ui/kids/SpaceBackdrop.kt`, `ui/components/LoadingSkeletons.kt`

---

### BM-10 · Nettverksinnstramming

I barnemodus snakkar eininga **berre med familien sin eigen tenar.**

**Akseptansekrav**

- [ ] Ingen kall til TMDB. Tilrådingsrada finst ikkje.
- [ ] Ingen kall til GitHub. Oppdateringssjekken er av.
- [ ] Ingen Seerr-kall. Førespurnader finst ikkje.
- [ ] Ingen Radarr- eller Sonarr-kall. Kalender og kø finst ikkje.
- [ ] Ein nettverkslogg frå ei barneøkt inneheld berre adressa til mediatenaren.

**Filer:** `data/repository/MediaSyncRepository.kt`, `update/UpdateUi.kt`

---

### BM-11 · Skjermfesting

Utan dette er barnemodus berre eit lag inne i Spole — barnet trykkjer Heim og opnar noko anna.

`startLockTask()` festar appen til skjermen. Brukaren godkjenner det éin gong.

**Akseptansekrav**

- [ ] Festing er eit val, ikkje påtvinga.
- [ ] Å låse opp følgjer same PIN som BM-4.
- [ ] Appen forklarer kva festing gjer før ho blir slått på første gong.
- [ ] Krasjar appen medan festing er på, blir eininga ikkje låst inne.

**Filer:** `MainActivity.kt`

---

### BM-12 · Tid og leggjetid

Begge er **lokale på eininga.** Jellyfin har ingen skjermtid, og vi finn ikkje opp ein
tenarfunksjon som ikkje finst.

**Åtferd**

- Dagleg grense og leggjetid avbryt aldri midt i noko. Gjeldande episode blir ferdig, og så kjem
  «Ferdig for i dag» i staden for neste.

**Akseptansekrav**

- [ ] Ingen avspeling blir kutta midt i ei scene.
- [ ] Teksten i oppsettet seier rett ut at eit barn som kan endre klokka, kan omgå dette. Dette er
      familierutine, ikkje ein tryggingsmekanisme.
- [ ] Grensa blir nullstilt ved midnatt i eininga si eiga tidssone.

**Filer:** `data/repository/AppPreferencesRepository.kt`, `ui/kids/`

---

### BM-13 · TV-tilpassing

**Akseptansekrav**

- [ ] Ingen sidemeny i barnemodus på TV.
- [ ] Fokus er 6 dp og skalerer 1,05×. Dette bryt med vilje den vaksne regelen om teikne-berre
      fokus; frå tre meters hald må fokus vere umogleg å bomme på.
- [ ] D-pad går rett i innhaldet ved oppstart.
- [ ] Fysisk Tilbake på framsida gjer ingenting — det finst ingen skjerm bak.
- [ ] Profilveljaren er første skjerm ved oppstart på TV når meir enn éin profil finst.

**Filer:** `ui/kids/`, `ui/components/FocusOutline.kt`

---

## Reglar som gjeld heile funksjonen

Desse er ikkje til forhandling. Bryt ein av dei, og funksjonen er ikkje barnemodus lenger.

1. **Ingen klientside-filtrering av innhald.** Grensa er kontoen.
2. **Spole endrar aldri brukarpolicy på Jellyfin.** Bibliotek og aldersgrenser høyrer til tenaren.
3. **Ingen admin-rettar.** Verkar funksjonen berre for ein Jellyfin-administrator, er han feil bygd.
4. **Ingen autospelande forhandsvisingar.** Ein heimetenar skal ikkje omkode straumar fordi eit
   blikk stoppa på ei flis.
5. **Ingen belønningar, poeng eller maskotar.** Ein app som prøver å halde barnet lengst mogleg,
   jobbar mot forelderen som installerte han.
6. **Ingen nedlasting.** Ei fil på disk overlever barnemodus.
7. **Ingen ny kantlinje på kort eller rader.** Flatenivå skil behaldarar, slik resten av appen gjer.

---

## Verifisering før ein hake blir sett

- JVM-testar og `lintDebug` køyrer grønt.
- Instrumentert test for det som er endra, når flata er testbar.
- For alt visuelt: køyrt på **både** telefon og Google TV med ekte data, gjennom
  `scripts/Start-SpoleEmulators.ps1`. Sjå `docs/EMULATORS_WITH_REAL_DATA.md`.
- **Ikkje køyr tunge Gradle-bygg samtidig med emulatorane.** Dei to saman sprengjer minnet i WSL og
  drep begge. Bygg først, emulator etterpå.
- Ingen skjermbilete med ekte kontonamn eller bibliotekinnhald skal leggjast i repoet.

---

## Kva som ikkje er avgjort

- Kva som skjer når barnet sin konto ikkje har noko innhald i det heile. Tom tilstand er skissert,
  ordlyden er ikkje bestemt.
- Om profilveljaren skal vise Emby-kontoar i tillegg til Jellyfin. Første versjon er Jellyfin åleine.
- Om widgeten skal skjule seg i barnemodus. Han lever på heimeskjermen utanfor appen, så truleg nei.

---

Framlegget med mockups: sjå den publiserte designskissa. Denne fila er kjelda for kva som skal
byggjast; skissa viser korleis det ser ut.
