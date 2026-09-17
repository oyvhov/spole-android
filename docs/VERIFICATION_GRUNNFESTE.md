# Verifikasjon: 0.17.1 — Grunnfeste, første arbeidsrunde

17. september 2026. Kjeldearbeid etter [gjennomgangen](REVIEW_2026-09-17.md). Ingen ny APK er
publisert, og versjonskoden står framleis på **89 / 0.17.0-beta10**.

## Bygg og testar

Bygd i WSL-kopien med `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`, og
signert release bygd på Windows med `assembleRelease`:

- **560 einingstestar køyrde, 0 feila.** 540 før arbeidet.
- **Lint: 0 feil, 84 åtvaringar.** Kontrollert mot HEAD først: 0 feil, 83 åtvaringar.
- `scripts/check-translations.py`: **868 nøklar** i alle tre språksetta (842 før).
- Release-APK: 4 262 441 byte, sertifikat-SHA-256
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` — same nøkkel som før.

Android-testpakken er **ikkje** køyrd. Ho krev den isolerte emulatoren 5562, som ikkje var oppe;
5564 er review-eining og skal ikkje brukast til instrumentering.

## Kontrollert med ekte data

Signert release installert med `install -r` over produksjonspakka `app.reelstack` på
`emulator-5564`. Kontoane og alle lagra val vart bevarte — appen kom opp med same Jellyfin- og
Emby-innhald, same profil og same aksentfarge, utan ny innlogging.

| Sak | Resultat med ekte bibliotek |
| --- | --- |
| GF-2, éi form per rad | **Bekrefta, etter ei retting.** Jellyfin-rada vart rett med ein gong. Emby-rada avslørte at fleirtalsregelen var feil: fem påbyrja filmar slo fire påbyrja episodar, og kvart episodebilete hamna i ei ståande plakatramme med grå band over og under. `AUTO` er no alltid det breie biletet. Begge radene er jamne. |
| GF-2, radoverskrifter | **Bekrefta.** «Sjå vidare · Jellyfin» og «Sjå vidare · Emby». |
| GF-3, direkte avspeling | **Bekrefta.** Ein H264/AAC-episode frå Emby: «Direkte avspeling frå Emby». |
| GF-3, omkoding med grunn | **Bekrefta.** Same fil som feila i gjennomgangen (H264 / EAC3 5.1): «Full omkoding på Emby. Fordi denne eininga kan ikkje spele lydformatet.» Før sa appen berre «Tilpassa avspeling frå Emby». |
| GF-3, progressiv melding | **Bekrefta.** «Ventar på Emby…» kom medan straumen stod, og loggen viser `action=force-compatible` — fallbacken går no ved 18 s i staden for 45. |
| GF-4, fokus i spelaren | **Bekrefta.** Play-knappen i OSD-en har ei tydeleg ramme og eit verktøytips; spolelinja er 6 dp. |
| GF-5, namn på knappar og kort | **Bekrefta.** Noden for «Spel av» på detaljsida har no `content-desc="Spel av"`. I gjennomgangen var same node tom. |
| GF-8, spel av frå heroen | **Bekrefta.** Heroen viser «Sjå meir» og «Spel av» side om side på ein påbyrja episode. |

Ein tekstfeil vart funnen og retta i same runde: dei to setningane i avspelingslinja vart slegne
saman utan punktum («Full omkoding på Emby Fordi …»).

## Kva som er gjort

### GF-1 · Fokusfelle i popupark

`StableSheetDialog` kallar no `onDismiss()` i `finally`, ikkje etter animasjonen. Ein kansellert
animasjon, ein stoppa frame clock eller ein komposisjon som forsvinn tek arket med seg. Inngangs-
animasjonen har ei grense på 900 ms og hoppar til synleg etterpå, så eit ark kan ikkje bli ståande
usynleg og modalt. Tilbake går utanom `dismissEnabled`; utanfor-trykk og lukkeknappen respekterer
han framleis. Til slutt: blir arket likevel ståande 1 s etter at avvisinga er kalla, kjem det
tilbake på skjermen i staden for å bli verande ein gjennomsiktig fokusfelle.

Tre Compose-testar er skrivne i `SheetEscapeTest` — Tilbake i første ramme, tre raske Tilbake, og
Tilbake medan ei Seerr-sending står på. **Dei er ikkje køyrde** (sjå over).

### GF-2 · Éi form per rad

`ResumeRail` avgjer forma éin gong for heile rada (`resumeRailIsWide`) i staden for per kort.
`RailArtwork` fyller ramma med ein uskarp kopi av biletet når biletet og ramma vender ulik veg —
same mønsteret detaljsida alt brukte. `LibraryCard` bruker same ramme.

Første regel lét fleirtalet av medietypen avgjere. Han vart forkasta etter kontroll med ekte data:
sjå tabellen over. `AUTO` er no alltid det breie biletet, som er forma hylla er *til* — eit bilete
frå der du stoppa, med ei framdriftslinje langs botnen.

### GF-3 · Avspelinga fortel kva ho gjer

`PlaybackPlan.direct: Boolean` er bytt ut med `PlaybackMode` med fire verdiar, utleidd frå
`VideoCodec=copy` / `AudioCodec=copy` i URL-en tenaren sjølv byggjer. `TranscodeReasons` blir lese
frå alle tre stadene serverane legg dei og kartlagt til setningar. OSD og Stats for Nerds viser
nivå og grunn; Stats for Nerds har fått lydspor og dropna bilete og er ikkje lenger hardkoda
engelsk. `PlayMethod` til tenaren rapporterer no `DirectStream` for ein remux i staden for
`Transcode`. Bufringa varslar ved 8 s, prøver enklare avspeling ved 18 s og gir opp ved 45 s — same
totaltid, men ikkje lenger stille. 10 einingstestar.

### GF-4 · Synleg fokus

`focusScale` gir fokuserte kort og OSD-knappar 1,06×, med trykk som framleis dempar til 0,985× og
redusert rørsle som fjernar skaleringa. OSD-knappane bruker same `focusOutline` som resten av appen
i staden for si eiga 1,5 dp-ramme. Spolelinja er 6 dp i staden for 4 dp når ho ikkje er fokusert.

### GF-5 · Namn på fokuserbare kort

`ResumeCard` og `LibraryCard` set `contentDescription` eksplisitt. Merk: `mergeDescendants = true`
etter `combinedClickable` gir ein *ny* fletterot under klikk-noden, og namnet hamnar då på eit barn
ingen skjermlesar landar på. Kontrollert begge vegar med `uiautomator dump` på TV-emulatoren.

### GF-6 · Språk (delvis)

28 nye nøklar × 3 språk for avspelingsnivå, grunnar, bufringsmeldingar og Stats for Nerds. Dei ~69
hardkoda feilmeldingane i data- og nettverkslaget og heile oppsettsarket står framleis att.

### GF-7 · Demoregelen (omformulert)

Gjennomgangen sa at standardverdiane i `ReelstackUiState` kunne vise demodata som ekte innhald.
**Det var overdrive:** `initialState` kontrollerer allereie regelen ved oppstart, og appen merkjer
demomodus synleg med «Førehandsvising med demoinnhald». Standardverdiane fungerer som testfikstur.
Det som faktisk var eit problem, var at regelen stod skriven ut tolv gonger med små skilnader. Han
er no éin funksjon, `demoContent`, med 4 einingstestar.

### GF-8 · Spel av frå heroen

Heroen har fått ein «Spel av»/«Hald fram»-knapp ved sida av «Sjå meir». Han ligg til høgre, ikkje
til venstre, fordi to eksisterande testar held «Sjå meir» til lovnaden om at handlinga aldri flyttar
seg medan heroen roterer — og ein knapp som kjem og går med den valde tittelen kan ikkje leie rada
utan å bryte det. `playableNow()` samlar vilkåret som før var skrive ut kvar gong avspeling vart
tilbydd; kortmenyen kravde framleis Jellyfin etter at Emby fekk spelar i beta04.

### GF-9 · Trygg sone på TV

`ReelLayout.TvSafeEdge` på 48 dp, gitt til alle fanene frå `ReelstackApp`. Aktivitet ignorerte
tidlegare paddinga han fekk og brukte flate 24 dp.

## Kva som står att etter første runde

GF-6 (resten av språkarbeidet), GF-10 (tittel under kortet på Oppdag og Aktivitet), GF-11
(skjermnær tilstand), GF-12 (distribusjonskanal) og GF-13 (polering).

---

# Andre arbeidsrunde — 17. september 2026

Emne: GF-1, GF-6, GF-9, GF-10, kalenderen, og språktilpassinga av datoar, klokkeslett og fleirtal.

## Bygg og testar

- **576 einingstestar køyrde, 0 feila.** 562 før runden. 14 nye.
- **Lint: 0 feil, 87 åtvaringar.** `HardcodedText` er no sett til `error` i `app/build.gradle.kts`,
  og bygget går grønt med regelen på.
- `scripts/check-translations.py`: **1166 nøklar** i alle tre språksetta (1010 før runden, 842 ved
  milepålens start).
- Signert release bygd på Windows. Sertifikat-SHA-256
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` — kontrollert med `apksigner`
  før installasjon, same nøkkel som før. Førre APK er teken vare på som
  `C:/JellyBin/.gradle-tmp/spole-rollback.apk`.
- Android-testpakken er framleis **ikkje** køyrd. Grunnen er ny og konkret: sjå GF-1 nedanfor.

## Kontrollert med ekte data på `emulator-5564`

Signert release installert med `install -r` over produksjonspakka. Kontoane vart bevarte — appen kom
opp på Jellyfin- og Emby-innhaldet utan ny innlogging.

| Sak | Resultat med ekte bibliotek |
| --- | --- |
| Kalenderen, daggrupper og datoar | **Bekrefta på nynorsk og engelsk.** Nynorsk: «17. sep. – 14. okt.», vekedagane «to fr la sø må ty on», «0 utgjevingar». Engelsk: «Sep 17 – Oct 14», «Thu Fri …», «0 releases». Dato- og vekedagsmønsteret kjem frå `getBestDateTimePattern` med lesaren sitt språk, ikkje frå eit fast nn-NO-mønster. |
| Kalenderen, filmutgjeving mot episode | **Ikkje kontrollert med ekte data.** Radarr og Sonarr er ikkje kopla til på denne kontoen, så «Kjem snart» seier «Kalenderkjelda er ikkje kopla til enno.» og kalenderen har null oppføringar. Skiljet — ståande plakat mot breitt stillbilete, filmikon mot skjermikon, og typeordet i tekstfargen med kjelda dempa — er skrive og einingstesta, men ingen har sett det med verkelege Radarr-/Sonarr-data. |
| «I dag» på heimskjermen | **Bekrefta.** Tre kort i «Nyleg tilgjengeleg» viser «I dag» på nynorsk og «Today» på engelsk. Dagordet blir no avgjort mot klokka i det kortet blir teikna; før låg det i den mellomlagra etiketten, som gjorde at ei rad lagra i går framleis sa «I dag» i dag. |
| Språkbyte nynorsk → engelsk → nynorsk | **Bekrefta.** Heile grensesnittet bytte språk: «Innstillingar» → «Settings», «Kjem snart» → «Coming soon», «Kalenderkjelda er ikkje kopla til enno.» → «The calendar source is not connected yet.», «Sist oppdatert kl. 12:50» → «Updated at 12:50». Språket vart sett tilbake til Norsk nynorsk før eininga vart forlaten. |
| Seerr-statusane | **Bekrefta i begge språk.** Anbefalingsrada viser «Delvis i biblioteket» og «I biblioteket ditt» på nynorsk; Aktivitet viser «In your library» på engelsk — frå den same `@StringRes`-funksjonen. |
| GF-10, tittel under kortet | **Bekrefta.** Oppdag og Aktivitet har tittel, type og status under plakaten; plakaten ber berre typemerket og statusikonet. |
| GF-9, trygg sone | **Delvis — og kravet er ikkje oppfylt slik det er formulert.** Sjå nedanfor. |

## GF-9 · Kva målinga faktisk viser

Skjermen er 1920 × 1080 px ved tettleik 320 (2,0×), altså 960 × 540 dp. Målt frå
`uiautomator`-dumpar med appen på ekte data:

| Flate | Venstre (innanfor navigasjonsfeltet) | Høgre | Topp | Botn |
| --- | --- | --- | --- | --- |
| Bibliotek | 40 dp | 24 dp | 31 dp | **18 dp** |
| Innstillingar | 36 dp | 42 dp | 24 dp | 72 dp |

`ReelLayout.TvSafeEdge` på 48 dp finst, men han blir berre gitt som **botnmarg** — éin gong frå
`ReelstackApp` (`screenInsets`) og éin gong i `LibraryHub`. Det var òg det han vart laga for: å
hindre at Aktivitet kutta den andre rada si på midten. Han er ikkje ein felles marg på alle fire
kantane, og formuleringa i veikartet — «felles innhaldsmarg på 48 dp på TV-flatene» — lova meir enn
koden gjer.

Bibliotek er den konkrete mangelen: den nedste hylla står 18 dp frå skjermkanten. Ho er siste
elementet i lista, så botnpaddinga til `LibraryHub` reserverer plass som lista aldri rullar ned til.

## GF-6 · Ferdig for apptekst, ikkje for medielinjer

Dette var hovudarbeidet i runden.

**Før:** 54 setningar i data- og nettverkslaget og 77 i grensesnittet, spelaren og varsla stod
skrivne rett i koden, på nynorsk.

**Etter:** 0. Søket etter norske strenglitteralar i `app/src/main/java` gir null treff.

To ting kom fram undervegs som er verdt å skrive ned, fordi dei ikkje var reine omsetjingsfeil:

**Råd som aldri nådde fram.** Setningar som «Seerr-kontoen er endra. Sjekk innlogginga før du
sender.» og «Vel minst éin sesong.» var kasta som `check()`. Grensesnittet har rett når det
handsamar ein `IllegalStateException` som ein defekt og byter teksten ut med ei generell melding —
dei fleste *er* defektar. Følgja var at rådet vart skrive, sendt ut og aldri lese av nokon. Dei er
`ServiceMessage` no, og `AdviceReachesTheReaderTest` held på at dei ber ei melding — og at ein
verkeleg defekt framleis ikkje blir presentert som råd.

Det same galdt heile spelaren: nitten setningar i `JellyfinPlayback` vart alltid bytte ut med «Fekk
ikkje opna tittelen» eller «Fekk ikkje starta videoen». `JellyfinPlayerModel` les no
`readableMessage` først, så «Denne episoden ligg ikkje i biblioteket enno.» kjem fram.

**Ein feil eg sjølv hadde laga.** `friendlyError` i `MediaSyncRepository` klassifiserte ein feil ved
å søkje etter det nynorske ordet «profil» eller «avviste» i unntaksmeldinga. Då meldingane vart
ressurs-ID-ar i førre runde, slutta det å verke i stillheit, og kvar tenar som feila fall gjennom
til «Fekk ikkje oppdatert Jellyfin». Han spør no `localizedFailure()` i staden.

`MediaSnapshotStore.nynorskLegacyText` er fjerna. Han omsette mellomlagra tekst attende til nynorsk
(«Today» → «I dag», «Direct play» → «Direkteavspeling»), som var rett så lenge orda låg i
mellomlageret — og som ville vore feilen sjølv på ein engelsk installasjon no.

**Det som står att, presist:** medielinjene. `ServicePayloadParser` set framleis saman strengar som
«Film · 2024», «Serie · 2022» og «S07 E01 · tittel», og dei blir *lesne attende* andre stader
(`subtitle.startsWith("Film")`, eit `S\d+ E\d+`-regex) og lagra i Room-mellomlageret. Å rette dette
er ikkje ei strengflytting, men å bere `mediaType`, årstal, lengd, sesong og episode som felt og
setje linja saman i grensesnittet — med ein skjemaendring i mellomlageret. 39 stader i 8 filer.
Språkmerknaden i innstillingane seier no nøyaktig dette i staden for «nokre skjermar».

## Klokkeslett og fleirtal

`MediaSyncRepository` tek imot `use24HourClock` frå `DateFormat.is24HourFormat(appContext)`, så ein
episode som går klokka 20.00 ikkje lenger alltid blir skriven «20:00» for ein lesar som har
12-timarsklokka på. Ei live avspelingsøkt ber no minutt og eit flagg i staden for ferdig skrivne
«20 min att» og «Direkteavspeling»; orda blir valde i `SessionWords.kt` — éin stad, så heimerada,
det kompakte kortet og fjernkontrollarket ikkje kan drive frå kvarandre.

## GF-1 · Kvifor testpakken framleis ikkje er køyrd

Dette er ikkje gløymsle, og ikkje noko eg kan rette frå denne økta.

Emulatoren køyrer under WSL, fordi Windows-emulatoren heng i WHPX-initialiseringa. `/dev/kvm` er
eigd av `root:kvm`, og brukaren `oyvhov` er ikkje med i `kvm`-gruppa — den emulatoren som *står* og
køyrer (`Spole_GoogleTV_Test`, port 5564) er starta som root. `sudo` krev passord, og eg skal ikkje
be om eit passord.

Så `Spole_Instrumentation` (5562) kan ikkje startast herifrå. Å køyre instrumentering på 5564 er
utelukka: det er review-eininga med ekte kontoar.

For å få GF-1 avnaglet treng eg at emulatoren blir starta éin gong — anten ved å leggje brukaren i
`kvm`-gruppa éin gong for alle, eller ved ei enkelt oppstart som root. Sjå [ROADMAP](../ROADMAP.md)
for kommandoane. Når han er oppe, køyrer eg `connectedDebugAndroidTest` mot 5562 og rapporterer
resultatet.

---

# Tredje arbeidsrunde — 17. september 2026

Emne: GF-1 utan emulator, og resten av GF-6.

## Bygg og testar

- **598 einingstestar køyrde, 0 feila.** 576 før runden.
- **Lint: 0 feil, 91 åtvaringar**, med `HardcodedText` på `error`.
- `scripts/check-translations.py`: **1208 nøklar** i alle tre språksetta.
- **Null norske strenglitteralar** att i `app/src/main/java`. Det er heile GF-6.
- Signert release bygd, sertifikatet kontrollert med `apksigner`, installert med `install -r` over
  produksjonspakka på `emulator-5564`. Kontoane bevarte.

## GF-1 · Løyst utan emulatoren

Førre runde sa at GF-1 var blokkert på at den isolerte emulatoren ikkje kan startast utan
rot-passord. Det var rett om vegen, men feil om instrumentet.

Sviket 16. september var ein coroutine som aldri kom vidare: avvisinga stod som setninga *etter*
animasjonen, så ein animasjon som vart kansellert eller venta på ei frame clock som ikkje gjekk,
nådde henne aldri — medan `closing`-låsen svelgde kvart seinare trykk. Det er ein
kanselleringsfeil, og ei **virtuell klokke** reproduserer han nøyaktig, på JVM-en, på millisekund.

Logikken er henta ut som to funksjonar med kvar si oppgåve:

- `sheetShouldLeave(guarded, canDismiss, closing)` — kven som får ta arket. Tilbake er *ugarda*,
  fordi han er den einaste vegen ut av ein fullskjerms modal; eit utanfor-trykk er garda og
  respekterer eit ark som held på å sende noko.
- `runSheetExit(timeoutMs, dismiss, animateOut)` — spelar utgangen og avviser uansett korleis han
  endar. `finally` dekkjer alle tre: ferdig, tidsavbroten, kansellert.

**10 einingstestar**, medrekna dei to endingane ein einingstest på eining berre kan produsere ved
uhell: animasjonen som blir kansellert, og animasjonen som heng forbi grensa. Ein av dei held òg på
at kanselleringa framleis blir meld oppover — `finally` skal ikkje svelgje henne.

Dei tre Compose-testane køyrer no under **Robolectric**, mot eit ekte dialogvindauge, og trykkjer
Tilbake i den første ramma vindauget finst i. `SheetEscapeTest` i `androidTest` står att for
køyringar på eining; milepålen ventar ikkje på henne.

**Akseptansen er dermed oppfylt i kvart bygg**, og GF-1 er ikkje lenger noko du må gjere.

## GF-6 · Resten: medielinjene

Førre runde tok 131 setningar ut av koden og lét medielinjene stå att som ein typeendring. Ho er
gjord.

**Det som var gale var ikkje berre språket.** Parseren skreiv «Film · 2024» og «Sesong 3» fordi ho
kjenner tala — og så las andre lag dei tilbake:

| Kva som las | Korleis | Kva som skjedde i eit anna språk |
| --- | --- | --- |
| Bibliotekrutenettet | `facts.filterNot { it in setOf("Film", "Serie", …) }` | Typeordet vart ståande to gonger |
| Kalenderen | `availability == "Fysisk utgjeving"` | Kvar fysisk utgjeving vart digital |
| Sesongoverskrifta | `season.name == "Sesong $number"` | Overskrifta fall gjennom til rådataen |
| Aktivitetsgrupperinga | bøtta *var* strengen «I GÅR» | Alt hamna under «Tidlegare» |
| Mellomlageret | `nynorskLegacyText()` omsette engelsk → nynorsk på veg ut | Rett medan appen berre snakka eitt språk |

`LocalizedText` ber no òg **tenartekst** (`LocalizedText.raw`). Det er det som gjer at ei liste med
fakta kan reise som avgjerder: «Apple TV+», «★ 8.3» og «12» kom frå ein tenar og tyder det same
overalt, medan «43 min» og «2 sesongar» er ord Spole skuldar lesaren. `MediaLineTest` held på at det
ikkje finst ein tredje slag — kvart fakta er anten ein ressurs eller tenartekst.

Fleirtalsformene er òg ekte fleirtal no: avgjerda ber kvantiteten, og ressursen vel forma. «1
sesong» mot «2 sesongar» var eit `if` i parseren.

**Mellomlageret er knytt til språket.** Radene held tekst, så eit språkbyte endrar fingeravtrykket
og neste synk skriv dei på nytt. Utan det ville heimskjermen stått halvt på nynorsk til noko
tilfeldig oppdaterte henne. Skjemaversjonen er òg bumpa til 4, så ingen rad frå eit eldre bygg ber
ei linje med typeordet baka inn.

## Kontrollert med ekte data

Same skjerm, same ekte Seerr-data, to språk:

| nynorsk | engelsk |
| --- | --- |
| Alle · 105 | All · 105 |
| På veg · 23 | In progress · 23 |
| Klare · 82 | Ready · 82 |
| Sjå førespurnadshistorikk | View request history |
| **Sesong 2** | **Season 2** |
| Førespurd | Requested |
| I biblioteket | In your library |

Heimskjermens heltelinje les «Episode · 2026 · 43m · TMDB 8.3» i begge språk — typeordet kjem frå
`mediaType`, årstalet og vurderinga frå tenaren, og lengda frå ein ressurs med eit tal.

Språket er sett tilbake til Norsk nynorsk før eininga vart forlaten.
