# Verifisering · 11. september 2026

Ikkje ei utgåve. Ingen versjonsauke. Alt under er lokale endringar i arbeidsmappa.

## Kva som vart gjort

### Barnemodus: prototypa og fjerna att

BM-1 til BM-4 frå [barnemodusplanen](BARNEMODUS_PLAN.md) vart bygde og deretter fjerna på
brukaren si melding same dag. Koden kompilerte og 356 einingstestar var grøne, men ingenting vart
sett på ei eining.

Reverten er kontrollert: ingen treff på `profileRepository`, `KidProfile`, `ProfileSwitcher`,
`publicUsers`, `connectionKeyPrefix`, `activeProfileId`, `refreshProfiles`, `ProfilePinDialog`,
`kidsMode` eller `PublicUser` i `app/src`. Sju nye filer er sletta, og sju eksisterande er stilte
tilbake. `ConnectionRepository`, `ServiceClients`, `ServicePayloadParser`, `ReelstackViewModel`,
`ReelstackApp`, `ReelstackSheets` og `AppContainer` er som før.

Det som vart lært, står i planen, slik at neste forsøk slepp å finne det på nytt.

### Lint køyrd for første gong

`lintDebug` hadde aldri vore fullført i dette prosjektet.

| | Åtvaringar | Feil |
| --- | --- | --- |
| Første køyring | 58 | 0 |
| Etter retting | 46 | 0 |

**Retta:**

- Ni ubrukte strengressursar fjerna frå begge språk: `activity_following`, `activity_requests`,
  `tablet_library_feature`, `media_view_details`, `media_play_in_spole`, `library_empty`,
  `personal_artwork_note`, `flow_details`, `tv_menu_help`. Alle kontrollerte for referansar først.
- `ConfigurationScreenWidthHeight` i `TabletLibraryFeature`. Koden las
  `Configuration.screenHeightDp < 650`, altså **skjermen**, ikkje vindauget. I delt skjerm fekk
  Heim-funksjonen difor det høge oppsettet inne i eit halvt vindauge. Les no
  `LocalWindowInfo.containerSize`, og terskelen er flytta til
  `WindowLayoutPolicy.useCompactFeature` saman med dei andre. Dette var eit sjuande brytpunkt som
  slapp unna opprydinga i designgjennomgangen.

**Dokumenterte unntak** — begge grunngjevne i koden, ikkje berre undertrykte:

- `VectorRaster` på `tv_banner.xml`. Lint vil ha vektorar under 200 × 200 fordi dei fleste er ikon;
  eit launcher-banner **må** vere 320 × 180 dp. Det blir teikna éin gong, av launcheren, frå nokre
  få baner.
- `PluralsCandidate` på `home_resume_description`. «percent» bøyer seg ikkje i engelsk og «prosent»
  ikkje i nynorsk. Ein fleirtalsressurs her ville vore to identiske strengar.

**Attståande 46**, ingen av dei åtferdsfeil: `InlinedApi` (13), `GradleDependency` og
`NewerVersionAvailable` (16 avhengnadsoppdateringar), `UseKtx` (6), `ModifierParameter` (6), og
fem einskildfunn.

### Automatisk omsetjingskontroll

`TranslationParityTest` dekkjer veikartpunktet om automatiske kontrollar av manglande omsetjingar,
parameter og fleirtalsressursar. Fire kontrollar les ressurs-XML-en direkte:

1. Kvar engelsk streng har ein nynorsk motpart.
2. Ingen nynorsk streng manglar engelsk original.
3. Formatparameter er like i begge språk. `%1$s` i eitt og `%s` i det andre er eit krasj ved
   køyring, i det språket utviklaren ikkje såg på — ikkje ein skrivefeil.
4. Fleirtalsressursar finst i begge språk med same mengdeformer.

Testen er kontrollert ved å leggje ein streng inn i berre eitt språk og sjå han feile med namnet på
strengen. Han er ikkje ein test som ikkje kan feile.

## Verifisering

- `compileDebugKotlin`: grøn.
- `testDebugUnitTest`: **373 testar, 0 feil, 0 error.**
- `lintDebug`: **0 feil, 46 åtvaringar.**
- Bygd frå WSL med eigen Linux-byggkatalog. Gradle frå Windows-skalet feilar framleis med
  «Unable to establish loopback connection» på denne maskina.

## Sett på Google TV med ekte data

Brukaren starta TV-emulatoren att, og bygget vart installert over den signerte appen med same
nøkkel. Kontoar, bibliotek og OCEAN-aksenten overlevde installasjonen.

**Verifisert visuelt:**

- **BOLD-fokus er TV-standard (K3).** Fersk installasjon utan lagra val gir ei tydeleg tjukkare
  ramme enn dei gamle 3 dp. Frå tre meters hald er det skilnaden på å sjå kvar du er og ikkje.
- **Dei nye glyffane teiknar rett (T6).** Seerr-lupa, Sonarr-skjermen og Radarr-rutene står i same
  opne strek som navigasjonsglyffane, medan Jellyfin og Emby korrekt held sine ekte merke.
- **«Spelar no» er retta.** To aktive økter ved sida av kvarandre: kantlaus 16:9-kunst, tittel over
  botnen, framdrift på kunstflata si eiga kant, og **begge korta med nøyaktig same høgd**. Det var
  dei ujamne botnane og den grå plata som vart meldt inn.
- Spel- og pauseglyffane i økt-knappane er dei nye.

**Ein observasjon, ikkje ein feil:** under den 8-sekunds rotasjonen i Heim-funksjonen er to titlar
og to undertekstlinjer lesbare samtidig i nokre rammer. Overgangen set seg reint etterpå. Verdt å
sjå på, men ikkje noko som står att i ro.

**Om R8:** release-bygget vart gjort **utan** minifisering, fordi WSL hadde 2 GB ledig og to
emulatorar køyrde. R8 er minnesluket som velta emulatorane tidlegare i økta. `build.gradle.kts` er
stilt tilbake til `isMinifyEnabled = true` etterpå — APK-en som er installert, er difor ikkje
representativ for storleik eller for korleis R8 handterer koden.

### Ikon, former og helten

- `SpoleIcons` gjekk frå 7 til 26 glyffar, og 93 Material-ikon vart bytta ut i 18 filer. Forholdet
  er snudd: 26 eigne mot 48 attståande Material-ikon, som er dei utan noko som svarar til dei i
  familien. Jellyfin og Emby held sine ekte merke.
- `MaterialTheme.shapes` er sett for første gong, med ein femstegs `ReelShapes`-skala. Tre radiusar
  som ikkje løyste noko naboen ikkje alt løyste (13, 15, 18 dp) er snappa til skalaen.
- **Helten hoppa mellom logo og tittel.** Logo-greina var `heightIn(max = 56.dp)` og fekk høgda si
  frå biletet sitt eige storleiksforhold; tekst-greina var alltid to linjer à 36 sp, altså ~72 dp.
  Verre: `logoFailed` startar som `false`, så eit logo som feila *etter* layout bytta den eine
  greina mot den andre. Begge greinene deler no éi reservert høgd, målt som dei to tekstlinjene ved
  gjeldande skriftskala, så dei er like også ved 2x tekst. Verifisert på TV med ekte data.

### Bibliotekvising: modell og lagring

`LibraryDisplay` med vising, kortstorleik, bilettype og titlar av/på, lagra per bibliotek-id.
Seks einingstestar dekkjer rundtur, standardverdiar, uleselege verdiar frå ein annan versjon og at
`LibraryArtType.ratio` svarar til kva biletet faktisk er. **Flata er ikkje bygd** — sjå veikartet.

### Bibliotek: filter og vising ut av popupen

Meldt inn: filtera skulle ikkje vere ein popup med vala langs sida, og biblioteket skulle kunne
endre vising, storleik og bilettype som i Jellyfin, lagra.

- **Årsaka til tastaturproblemet.** Filterdialogen leidde med eit `OutlinedTextField`, og ein dialog
  gir fokus til sitt første fokuserbare barn. På TV spratt difor tastaturet opp og dekte halve
  dialogen i det du opna henne. Inline er det ingenting som stel fokus, så problemet forsvinn ved
  rota i staden for å bli demma opp med eit fokus-flagg.
- Filter og vising er no to kompakte `FlowRow`-grupper på sida, i same chip-språk. Chipsene er så
  breie som etiketten, ikkje strekte til lik breidd — første utkast tok heile skjermhøgda og vart
  gjort om.
- Ingen Bruk/Avbryt. Kvart val slår inn med ein gong; rutenettet bak er stadfestinga.
- `LibraryDisplay` (vising, kortstorleik, bilettype, titlar av/på) blir lagra per bibliotek-id.
  Seks einingstestar dekkjer rundtur, standardverdiar, uleselege verdiar frå ein annan versjon og at
  `LibraryArtType.ratio` svarar til biletet.
- Bilettypen blir bytta ved å skrive om den ferdige biletadressa på klientsida, ikkje ved ein ny
  runde gjennom datalaget. `AUTO` lèt adressa stå som tenaren bygde henne.

**To ting som vart fanga før dei gjekk ut:** søket kalla `onApply` på kvart tastetrykk og ville ha
lasta biblioteket på nytt per bokstav — det skriv no lokalt og sender ved Enter eller fokustap. Og
første versjon fjerna søket heilt saman med dialogen, som er ein regresjon og ikkje ei forenkling.

### Grafikk oppdaterte seg aldri etter endring på tenaren

Meldt inn: klarlogoen endra seg ikkje sjølv etter oppdatering i Jellyfin og refresh i appen.

Årsaka var ikkje cachen, men adressa. Parseren las `ImageTags` berre for å avgjere *kva type*
bilete som fanst, og la aldri taggen inn i URL-en. Jellyfin sin tagg er ein innhaldshash, så
adressa var bit-identisk før og etter at biletet vart bytt — og Coil nøklar på adressa. Det gamle
biletet vart difor servert til cachen tilfeldigvis vart kasta ut.

Taggen går no gjennom heile kjeda: `LibraryArtwork` og `RemoteLibraryItem` ber han, og både
`artworkUrl` og `logoUrl` legg han på. Han blir henta frå den staden som svarar til kva adressa
peikar på — elementet sin eigen `ImageTags`, seriens `SeriesThumbImageTag`/`SeriesPrimaryImageTag`/
`SeriesLogoImageTag`, eller `ParentLogoImageTag`. Ein tagg som høyrer til eit anna element enn
adressa peikar på, er verre enn ingen tagg.

Ni nye einingstestar (`ImageTagTest`) dekkjer kvar kjelde, små og store bokstavar (Emby og Jellyfin
er ueinige), manglande tagg, og det som er sjølve poenget: to versjonar av same element med ulik
grafikk gir ulik adresse. Ein eksisterande test låste fast den gamle adressa **utan** tagg — altså
feilen — og er oppdatert med grunngjeving i koden.

**Dette er grunnen til at appen ikkje treng ein «tøm biletcache»-knapp.** Ein slik knapp ville vore
symptomdemping; cachen held seg sjølv oppdatert når adressa er rett, slik ho gjer i Jellyfin sine
eigne klientar.

## Kva som framleis ikkje er verifisert

- Instrumenterte testar er ikkje køyrde. `AdaptivePageTest` og `AdaptiveDialogTest` er dei mest
  relevante, sidan brytpunkta er rørte igjen.
- Endringa i `TabletLibraryFeature` gjeld delt skjerm og er ikkje prøvd i delt skjerm.
- Ingen minifisert release-APK er bygd eller prøvd.
- Telefonen er ikkje sett; berre Google TV.
- Sett/favoritt-skrivinga (S3) har inga brukarflate enno, så ho er berre dekt av einingstestar.
- Bibliotekfilteret og visingskontrollane er installerte på Google TV, men **ikkje gjennomgåtte på
  skjerm**: listevisinga (`LibraryView.LIST`) er ikkje sett, og om rulleposisjonen held seg ved
  skifte av bilettype er ikkje prøvd.
- Appen på emulatoren er `0.16.0-alpha15`, bygd **utan R8**.

## Merknad om bygg og emulator

Ikkje køyr eit release-bygg samtidig med emulatorane. WSL-VM-en når minnetaket sitt, og
OOM-drapsmannen tek både Gradle og begge emulatorprosessane. Det skjedde i denne økta.
Bygg først, emulator etterpå — sjå [EMULATORS_WITH_REAL_DATA.md](EMULATORS_WITH_REAL_DATA.md).
