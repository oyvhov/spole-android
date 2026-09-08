# Publiseringsklar? — gjennomgang av Spole 0.13.3

Gjennomgang av `C:/JellyBin/reelstack-android` slik koden stod ved commit `1669b5e`: versionCode 38,
versionName 0.13.3, pakke `app.reelstack`, minSdk 26, targetSdk 36.

> **Status etter 0.14.0.** Rapporten under er originalen, uendra, slik at det går an å sjå kva som
> vart funne. Det meste er no retta — sjå statustabellen rett under. Der eit funn er retta,
> gjeld `docs/release-v0.14.0.md` og `docs/VERIFICATION_v0.14.0.md` framfor teksten her.

## 0. Status etter 0.14.0

| Funn | Status |
|---|---|
| 3.1 `mapping.txt` blir kasta | ✅ Arkiveringssteg lagt inn i `AI_INSTRUCTIONS.md §8` |
| 3.2 Ingen krasjrapportering | ✅ Lokal, skrubba krasjlogg med delingsknapp |
| 3.3 5,1 MB demokunst i APK-en | ✅ WebP. APK 7,14 MB → 2,24 MB |
| 3.4 Manglande juridiske vedlegg | ⚠️ Personvern, TMDB, varemerke og OSS gjort. **LICENSE: valt bort** |
| 3.5 Uversjonert tilrådingssti | ⚠️ Appen spør `v1/`, men fila må lagast i katalogrepoet |
| 4.1 3xx fell gjennom til «status 301» | ✅ Les `Location`, i alle fem statushandterarar |
| 4.2 `runCatching` svelgjer avbrot | ✅ 22 kall gjennom `attempt` |
| 4.3 Tapt keystore-nøkkel → demodata | ✅ Eigen tilstand og eiga melding |
| 4.4 Ingen retry eller backoff | ✅ Retry på GET, `Retry-After`, tak i workeren |
| 4.5 Widget-timeout over vindauget | ✅ 8 s, med eigne kortare tidsavbrot |
| 4.6 `error.message` rett i UI | ✅ `ServiceMessage` + `readableMessage()` |
| 5 `ANDROID_ID` som einings-ID | ✅ UUID per installasjon, med migrering |
| 5 Globalt `cleartextTrafficPermitted` | ❌ **Rådet var feil.** Sjå merknaden under |
| 5 Ingen CI | ✅ GitHub Actions |
| 5 26 lint-åtvaringar | ✅ 18, og 0 feil |
| 5 157 hardkoda strengar | ⏸ Ikkje gjort. Framleis eit medvite val |
| Frå testgjennomgangen: utdatert smoke-test | ✅ Retta, 82/82 i rein køyring |
| Frå testgjennomgangen: dublette Silo-treff | ✅ Deduplisert, stadfesta mot levande tenarar |
| Frå testgjennomgangen: 16 KB uverifisert | ✅ Verifisert på begge aksane |

**Retting av eit råd i denne rapporten.** Punktet i del 5 om å erstatte det globale
`cleartextTrafficPermitted` med ein `<domain-config>` for private område er ugjennomførleg.
Android matchar `<domain>` som eit høgre-anker hansuffiks, slik DNS gjer, og har ingen
CIDR-syntaks: «192.168» matchar ikkje «192.168.1.20». Berre ei uttømmande liste over literale
adresser ville verke, og heile poenget er at tenaren kan stå kvar som helst på brukaren sitt eige
nett. `EndpointValidator.isTrustedLanHost` er difor den einaste staden regelen kan handhevast, og
lint-åtvaringa er no undertrykt med den grunngjevinga i sjølve fila.

## 1. Kort svar

Appen er teknisk sett allereie publiserbar på den kanalen han faktisk brukar i dag:
signert produksjons-APK i ein privat GitHub Release. Signering, R8, ressursreduksjon,
baseline-profil, kryptert tokenlagring og feilmeldingar på nynorsk er på plass, og
testdekninga (203 JVM-testar, 82 Android-testar, 0 lint-feil) er betre enn i dei fleste
hobbyprosjekt.

Det som manglar handlar ikkje om at appen ikkje verkar, men om tre andre ting:

1. **Diagnostikk.** Det finst ikkje éin einaste `Log`-setning i `app/src/main`, ingen
   krasjrapportering, og R8-`mapping.txt` blir ikkje arkivert med releasen. Når ein
   brukar seier «det verkar ikkje», finst det ingenting å sjå på.
2. **Juridiske og formelle vedlegg.** Ingen LICENSE, inga personvernerklæring, ingen
   TMDB-attribusjon, ingen varemerkefråskriving for Jellyfin/Emby/Seerr/Radarr/Sonarr.
3. **Nokre konkrete feilhandteringshol** — sjå del 4. Ingen av dei er krasj; dei er
   tilstandar der appen fortel brukaren feil ting, eller ingenting.

Kor mykje av dette som er nødvendig avheng heilt av kva «publisere» tyder. Det avgjer resten
av rapporten.

## 2. Tre kanalar, tre ulike krav

| | Privat GitHub Release (i dag) | Offentleg GitHub / eiga side | Google Play |
|---|---|---|---|
| Signert APK med stabil nøkkel | ✅ har det | ✅ | AAB i staden for APK |
| LICENSE-fil | ikkje nødvendig | **påkravd i praksis** | ikkje påkravd |
| Personvernerklæring | ikkje nødvendig | tilrådd | **påkravd** |
| Data safety-skjema | — | — | **påkravd** |
| TMDB-attribusjon | tilrådd | **påkravd** | **påkravd** |
| Varemerkefråskriving | — | **påkravd** | **påkravd** |
| Krasjrapportering | tilrådd | tilrådd | **de facto påkravd** |
| Play Console-konto + 12 testarar | — | — | **påkravd** for nye personlege kontoar |
| Nynorsk som einaste språk | greitt | greitt | avgrensar rekkevidde, men lovleg |

Delane under er sorterte etter kva som blokkerer, ikkje etter kva som er lettast.

## 3. Blokkerarar

### 3.1 R8-mapping blir kasta ved kvar `clean`

`app/build/outputs/mapping/release/mapping.txt` finst etter eit bygg, men
`app/build/release-v0.13.3/` inneheld berre APK og `SHA256SUMS.txt`, og `app/build/` er
gitignorert. Første `gradlew clean` etter ein release slettar mappinga permanent.

Konsekvens: ein stack trace frå ein publisert versjon kan aldri lesast igjen. Med
`isMinifyEnabled = true` er ein uleseleg stack trace det einaste ein nokon gong får.

Tiltak: kopier `mapping.txt` inn i `app/build/release-vX.Y.Z/` saman med APK-en, og last
han opp som ein del av GitHub-releasen. Ein linje i punkt 7 i `docs/AI_INSTRUCTIONS.md §8`.
Dette er den billigaste og mest verdifulle enkeltendringa i heile rapporten.

### 3.2 Ingen krasjrapportering og ingen logg

`grep -rn "Log\.\|printStackTrace" app/src/main` gir null treff. Det er eit medvite
personvernval — men det betyr òg at ein krasj hos ein brukar forsvinn sporlaust.

`PRODUCT_PLAN.md` milepæl 5 har allereie formulert kravet riktig: «Crash reporting kept
optional and scrubbed of endpoints/media history». For ein sjølvhosta app er
Firebase Crashlytics feil verktøy (det ville krevje eit Data safety-punkt om
tredjepartsdeling). To alternativ som passar betre:

- **Lokal krasjlogg.** `Thread.setDefaultUncaughtExceptionHandler` som skriv stack trace,
  versjon og einingsmodell til ei fil i app-privat lagring. Ein «Del feilrapport»-knapp i
  Innstillingar opnar ein delingsintent. Ingen nettverkskall, ingen samtykkedialog,
  ingenting å oppgi i eit data safety-skjema.
- **Sjølvhosta Sentry/GlitchTip**, opt-in, av som standard.

Uansett val: URL-ar, tokens og titlar må skrubbast før dei blir skrivne. Dette gjeld særleg
`error()`-meldingane, som i dag interpolerer tenestenamn — men ikkje adresser.

### 3.3 5,1 MB av ein 7,1 MB APK er demokunst

Frå ei utpakking av `app/build/outputs/apk/release/app-release.apk`:

~~~text
3 381 084  classes.dex
1 855 766  res/Vq.png
1 811 964  res/l1.png
1 433 286  res/m2.png
~~~

Dei tre PNG-ane er `desert_arrival`, `kitchen_request` og `session_still` i
`res/drawable-nodpi/` — demokunst som berre blir brukt av `demoDiscover()`,
`demoIncoming()` og `demoSessions()` i `ReelstackViewModel.kt:1480-1700`.
**71 % av produksjons-APK-en er plassholdarbilde ein tilkopla brukar aldri ser.**

Ressursreduksjonen fungerer forresten korrekt: `cinematic_sky.png` (1,5 MB, heilt
urefererte) og `reelune_mark.png` (514 KB, gammal merkevare) blir allereie strippa bort.
Dei tre som står att, står att fordi koden faktisk refererer dei.

Tre måtar å fikse det:

- Konverter til WebP med kvalitet 80. Typisk 80–90 % reduksjon utan synleg tap.
- Skaler ned. Bilda ligg i `drawable-nodpi` i full oppløysing; eit kort i eit rad-oppsett
  treng ikkje 2000 px breidde.
- Flytt demodata til ein eigen `demo`-produktvariant, slik at produksjonsbygget ikkje
  inneheld dei i det heile.

Det tredje er reinast og stemmer best med føringa i `AI_INSTRUCTIONS.md §1`: «Demo-data
skal berre visast i eksplisitt demo-modus.» I dag er demodata standardverdiar i
`ReelstackUiState`, ikkje ein eksplisitt modus.

### 3.4 Manglande juridiske vedlegg

| Manglar | Kvar det høyrer heime | Kvifor |
|---|---|---|
| `LICENSE` | repo-rot | Utan lisens er standard «alle rettar reserverte». Ein offentleg publisert app utan lisens kan ingen lovleg byggje eller vidareformidle. |
| Personvernerklæring | `docs/PRIVACY.md` + lenkje i Innstillingar | Play krev det. Innhaldet er kort her: appen sender data berre til brukaren sine eigne tenarar, pluss `image.tmdb.org` og `raw.githubusercontent.com`. |
| TMDB-attribusjon | Innstillingar → Om | TMDB sine vilkår krev logo og teksten om at produktet ikkje er godkjend av TMDB. Appen hentar `image.tmdb.org/t/p/w500` og `w780` fleire stader utan noka form for attribusjon. |
| Varemerkefråskriving | README + Om-skjermen | Appen brukar Jellyfin- og Emby-logoane (`ic_service_jellyfin.xml`, `ic_service_emby.xml`) og namna til fem prosjekt. «Spole er ikkje tilknytt eller godkjend av …» |
| Tredjepartslisensar | Om-skjermen | APK-en inneheld allereie `META-INF/…/LICENSE.txt` for AndroidX. Ein OSS-lisensskjerm er standard forventning. |

Om-seksjonen finst allereie (`SecondaryScreens.kt:803` `AppIdentity`) og viser namn og
versjon. Det er den naturlege staden for alle fire.

### 3.5 Avhengigheit av eit privat GitHub-repo i køyretid

`RecommendationsClient.kt:19` hentar tilrådingar frå
`raw.githubusercontent.com/oyvhov/spole-recommendations/main/recommendations.json`.

Tre ting følgjer av det:

- Kvar brukar sender IP-adressa si til GitHub. Det må stå i personvernerklæringa og i
  eit eventuelt data safety-skjema.
- Dersom `spole-recommendations` er privat, feilar kallet med 404 for alle andre enn deg.
  Feilhandteringa er riktig (rada forsvinn, cachen held), men funksjonen er daud.
- `raw.githubusercontent.com` har inga oppetidsgaranti og ingen versjonering. For ein
  publisert app bør endepunktet minst vere versjonert (`…/v1/recommendations.json`) slik at
  ein gammal app-versjon ikkje bryt når formatet endrar seg.

## 4. Feilhandtering

Dette er den delen du spurde direkte om. Utgangspunktet er betre enn eg venta.

### Det som allereie er rett

- **`contacting()` i `ServiceContact.kt`** er nettopp riktig mønster: rå
  `UnknownHostException` og `ConnectException` når aldri brukaren, og SSL-feil får ei eiga
  melding fordi det krev eit anna neste steg enn ein utilgjengeleg tenar.
- **`requireSuccess()` i `ServiceClients.kt:927`** dekkjer 401, 403, 404, 408, 429 og 5xx
  med kvar sin nynorske tekst, og skil Seerr frå dei andre.
- **Widgeten** brukar `goAsync()`, timeout og `errorViews()`. Ingen uendeleg spinnar.
- **Keystore-skrivinga er vakta.** `ReelstackViewModel.kt:1289-1299` fangar ei feila
  tokenlagring og seier frå i staden for å melde ei tilkopling appen ikkje kan bruke.
  Kommentaren over står seg.
- **`sessions.single()` (`HomeScreen.kt:470`) og `playing.first()`
  (`NowPlayingWidget.kt:91`)** er begge korrekt vakta.

### 4.1 3xx-omdirigering fell gjennom til «status 301»

`HttpTransport.kt:47` set `instanceFollowRedirects = false`. Det er *riktig* — å følgje ei
omdirigering automatisk kan sende eit token til ein annan vert.

Men `requireSuccess()` har ingen 3xx-gren, så eit svar på 301/302/307/308 endar i
`else -> error("… svara med status $statusCode")`.

Dette er ikkje eit kantfelt. Ein reverse proxy som omdirigerer `http://` → `https://`, eller
som legg på ein skråstrek, er **den vanlegaste oppsettfeilen for sjølvhosta tenarar**.
Brukaren får «Jellyfin svara med status 301» og har ingen idé om kva som er gale.

Tiltak: eiga gren for `in 300..399` som les `Location`-headeren og seier noko i retning av
«Tenaren sender deg vidare til *https://…*. Bruk den adressa i staden.» Headeren finst
allereie i `connection.headerFields`. Dette er truleg den einskildendringa som sparar mest
brukarstøtte.

### 4.2 `runCatching` svelgjer avbrot 20 stader

`ReelstackViewModel.kt` har 29 `runCatching`-blokker. Ni av dei kastar
`CancellationException` vidare (linje 862, 1104, 1135, 1162, 1193, 1231, 1266, 1275, 1297).
Dei tjue andre gjer det ikkje.

`runCatching` fangar `Throwable`, og `CancellationException` er ein `Throwable`. Når ein
korutine blir avbroten — brukaren lukkar eit popupark midt i eit kall, byter fane, eller
`refreshJob?.cancel()` køyrer — går `onFailure`-greina i staden, og brukaren får ei
feilmelding for noko han sjølv avbraut.

Du har openbert oppdaga mønsteret allereie, sidan ni stader er retta. Dette er å fullføre
det same sveipet. To måtar:

- Ein liten hjelpar, `suspend fun <T> attempt(block: suspend () -> T): Result<T>`, som gjer
  rethrow-en éin gong og blir brukt alle stader. Då kan mønsteret ikkje gløymast igjen.
- Eller ein `lint.xml`-regel som avviser bar `runCatching` i `ui/`.

Alvorsgraden varierer: dei som ligg rett i `viewModelScope` er lite risikable, sidan heile
scopet berre blir avbrote når ViewModel-en forsvinn. Dei som ligg inne i `async`, eller som
`refreshJob?.cancel()` kan treffe, er dei reelle.

### 4.3 Tapt keystore-nøkkel viser demodata utan forklaring

`EncryptedTokenStore.get()` returnerer `null` når dekrypteringa feilar
(`runCatching { … }.getOrNull()`, linje 33-42). Det skjer om Android Keystore-nøkkelen blir
ugyldig — etter ei gjenoppretting til ei ny eining, eller etter enkelte OEM-oppdateringar.

Følgjekjeda:

1. `ConnectionRepository.get()` set `token = ""` men
   `state = CONNECTED` og `detail = "Konfigurert"`, fordi URL-en framleis finst
   (`ConnectionRepository.kt:38-39`).
2. Oppfriskinga filtrerer på `token.isNotBlank()`, finn ingenting, og fell tilbake til
   `demoSessions()`, `demoResume()` og resten (`ReelstackViewModel.kt:812-836`).
3. Brukaren ser demokunst på Heim, medan Innstillingar framleis seier «Konfigurert».

Tiltak: skil «ingen URL lagra» frå «URL lagra, men tokenet kunne ikkje dekrypterast». Den
andre bør gi ein eigen tilstand med teksten «Innlogginga på denne eininga kan ikkje lesast
lenger. Logg inn på nytt.» og ei direkte lenkje til innloggingsflyten.

### 4.4 Ingen retry eller backoff

`HttpTransport` prøver kvart kall nøyaktig éin gong. `MediaRefreshWorker` returnerer
`Result.retry()` utan å sjå på `runAttemptCount`, så ein tenar som er permanent nede blir
prøvd på nytt i det uendelege — WorkManager sin backoff dempar det, men gir aldri opp.

`PRODUCT_PLAN.md` milepæl 4 listar «Exponential backoff, rate-limit handling, and
per-service diagnostics» som ikkje gjort, så dette er kjent. For publisering er minimumet:

- Éin retry på `SocketTimeoutException` og `ConnectException` i `HttpTransport`.
- Respekter `Retry-After` når svaret er 429.
- Gi opp i workeren etter eit tak, til dømes `if (runAttemptCount > 5) return Result.success()`,
  slik at ein daud tenar ikkje tappar batteri i det uendelege.

### 4.5 Timeouta i widgeten er høgare enn vindauget receiveren har

`NowPlayingWidget.kt:126` set `SESSION_TIMEOUT_MS = 12_000L`, medan `HttpTransport` brukar
7 s tilkopling + 9 s lesing per kall. Ein `BroadcastReceiver` som held på resultatet med
`goAsync()` bør vere ferdig innanfor rundt 10 sekund; blir kringkastinga levert med
framgrunnsprioritet, avsluttar systemet han for deg.

Dette bør stadfestast på ekte eining før det blir kalla ein feil — men marginen er uansett
feil veg. Ei timeout på 6–8 s, og lågare `readTimeoutMs` spesielt for widget-kallet, gir
brukaren «Fekk ikkje kontakt» i staden for eit kort som blir ståande på «Hentar…».

### 4.6 `error.message` går rett i brukargrensesnittet

Ni stader i `ReelstackViewModel` skriv `error.message ?: "reservetekst"` direkte inn i
`ConnectionDraft.error`. Det fungerer så lenge unntaket kjem frå `contacting()` eller
`requireSuccess()`, som begge er skrivne for å bli lesne.

Men eit kva som helst anna `IllegalStateException` eller `IllegalArgumentException` frå
djupare kode hamnar same stad. `HttpTransport.kt:70` har til dømes
`require(total <= MAX_RESPONSE_BYTES) { "Svaret frå tenaren var for stort" }` — den er
heldigvis lesbar, men det er tilfeldig, ikkje garantert.

Tiltak: eigen unntakstype, til dømes `class ServiceMessage(message: String) : Exception(message)`,
som `contacting()` og `requireSuccess()` kastar. Då kan ViewModel-en skilje «dette er ein
tekst skriven for brukaren» frå «dette er ein feil i appen», og vise ein nøytral tekst pluss
ein krasjlogg for det andre.

## 5. Bør fiksast, men blokkerer ikkje

- **`Settings.Secure.ANDROID_ID` som einings-ID** (lint `HardwareIds` ×3, i
  `AppContainer.kt:14`, `AccountAvatar.kt:53`, `MediaArtwork.kt:56`). ID-en går berre til
  brukaren sine eigne tenarar, så bruken er forsvarleg — men han er ein varig
  einingsidentifikator, og Play sitt data safety-skjema vil spørje. Ein tilfeldig UUID
  generert ved første oppstart og lagra lokalt gjer akkurat same nytten som Jellyfin-DeviceId
  og fjernar heile spørsmålet.
- **`cleartextTrafficPermitted="true"` globalt** (lint `InsecureBaseConfiguration`).
  `EndpointValidator.isTrustedLanHost()` gjer allereie den *reelle* jobben og tillèt HTTP
  berre for localhost, `.local` og literale private adresser — den kontrollen er
  gjennomtenkt, inkludert `nip.io`-fella. Nettverkskonfigurasjonen bør spegle det med
  `<domain-config>` for private område i staden for eit globalt `base-config`, slik at
  plattforma handhevar det same som koden.
- **Ingen CI.** Ingen `.github/`-mappe. Alt bygg og all test skjer manuelt. Ein
  GitHub Actions-arbeidsflyt som køyrer `testDebugUnitTest lintDebug assembleRelease` på
  kvar push ville fanga regresjonar utan at nokon hugsar å køyre dei — og dokumentere
  testtala i `VERIFICATION_*.md` automatisk.
- **157 tekststrengar hardkoda i Kotlin**, mot 4 i `strings.xml`. Nynorsk som einaste språk
  er eit medvite produktval og heilt legitimt. Men når teksten ligg i Kotlin i staden for
  ressursar, er òg *avgjerda* låst: å legge til bokmål eller engelsk seinare blir eit
  fleirvekes arbeid i staden for ei omsetjingsfil. Verdt å vurdere no, medan det er 157
  strengar og ikkje 500.
- **`fallbackToDestructiveMigration(dropAllTables = true)`** i `CacheDatabase.kt:105` er
  riktig for ein cache. Verdt å notere i personvernerklæringa at cachen kan bli sletta ved
  ei oppgradering, slik at det ikkje blir lese som datatap.
- **`ConnectionState.CONNECTED` tyder «URL lagra»**, ikkje «tenaren svarar». Sjå 4.3.
- **26 lint-åtvaringar**, alle godarta: 5 `UseKtx`, 5 `GradleDependency`,
  4 `UnusedResources`, 3 `NewerVersionAvailable`, 3 `HardwareIds`, 1 `OldTargetApi`,
  1 `InsecureBaseConfiguration` og tre andre. Verdt ein opprydding før ein offentleg release,
  særleg dei fire urefererte ressursane.

## 6. Om det skal på Google Play

Berre relevant om det er målet. Utover alt over:

- **AAB, ikkje APK.** `bundleRelease` i staden for `assembleRelease`. Play Signing overtek
  då nøkkelen — men den eksisterande nøkkelen kan lastast opp som upload key, slik at
  sertifikat-SHA-256 `36fa94…` framleis gjeld for direkte APK-ar.
- **Sideloading held fram parallelt.** Play-distribusjon og GitHub Release kan leve side om
  side så lenge det er same signering.
- **Ny personleg utviklarkonto krev 12 testarar i 14 dagar** før produksjonstilgang. Ein
  organisasjonskonto slepp kravet, men krev D-U-N-S-nummer.
- **Data safety-skjema** må dekkje: einings-ID (sjå 5), IP til GitHub og TMDB, og at
  påloggingsdata blir lagra kryptert lokalt og aldri sende til utviklaren.
- **Butikkoppføring:** ikon 512×512, funksjonsgrafikk 1024×500, minst to skjermbilde per
  formfaktor. `docs/images/` har allereie tre gode skjermbilde frå 0.13.2 som kan brukast.
- **Skjermbilde må ikkje vise ekte tenaradresser eller kontonamn.** Dei tre i `docs/images/`
  er tekne med demodata og er trygge.

## 7. Framlegg til rekkefølgje

Sortert etter verdi delt på innsats, ikkje etter kapittel.

**Ein ettermiddag — gjer at ein publisert versjon kan feilsøkjast i det heile:**

1. Arkiver `mapping.txt` med kvar release (3.1).
2. 3xx-gren i `requireSuccess()` som les `Location` (4.1).
3. Reduser widget-timeouta til 8 s (4.5).
4. Tak på `runAttemptCount` i `MediaRefreshWorker` (4.4).

**Ein dag — fjernar dei verste overraskingane:**

5. Lokal krasjlogg med delingsknapp i Innstillingar (3.2).
6. Fullfør `CancellationException`-sveipet med ein `attempt`-hjelpar (4.2).
7. Eigen `ServiceMessage`-unntakstype (4.6).
8. Eigen tilstand for udekrypterbart token (4.3).

**Ein dag — gjer releasen formelt publiserbar:**

9. LICENSE, `docs/PRIVACY.md`, TMDB-attribusjon, varemerkefråskriving og OSS-lisensar i
   Om-skjermen (3.4).
10. Versjonert endepunkt for tilrådingar (3.5).

**Når det passar:**

11. WebP eller eigen demo-variant — kuttar APK-en frå 7,1 MB til rundt 2 MB (3.3).
12. UUID i staden for `ANDROID_ID` (5).
13. `<domain-config>` i staden for globalt `cleartextTrafficPermitted` (5).
14. GitHub Actions for bygg og test (5).
15. Strengar ut i `strings.xml` (5).

Punkt 1 til 4 er små, isolerte og lette å verifisere med eksisterande testar. Dei er den
naturlege neste releasen.

## 8. Det som allereie held mål

Verdt å seie tydeleg, sidan rapporten elles berre listar manglar:

- Signering er stabil og verifisert på tvers av releasar, med same sertifikat sidan 0.11.7,
  og `app/build.gradle.kts:22-28` blokkerer eit produksjonsbygg utan den rette nøkkelen.
- R8 og ressursreduksjon fungerer — verifisert ved at daud merkevare frå «Reelune» faktisk
  er borte frå APK-en.
- Baseline-profil ligg i `assets/dexopt/`, så oppstarten er AOT-kompilert frå første køyring.
- Éin dex-fil, ingen multidex, fire ABI-ar, signaturskjema v2/v3.
- `EndpointValidator` er den beste enkeltfila i prosjektet. Kommentaren om `nip.io` og
  `fcbarcelona.com` viser at nokon faktisk har tenkt gjennom kva ein «privat» vert er.
- Tokenlagring bruker Android Keystore med AES-GCM og tilfeldig IV per skriving, og er
  ekskludert frå både sky-backup og einingsoverføring.
- Varselkanalar er delte per hendingstype, slik at ein kan tie ned «nedlasting feila» utan
  å miste «klart å sjå».
- 203 JVM-testar og 82 Android-testar, med 0 lint-feil, og dokumenterte verifiseringar per
  versjon i `docs/VERIFICATION_*.md`.
- Ingen hemmelegheiter i Git. `signing.properties`, `local.properties`, `*.jks` og `*.apk`
  er alle ignorerte, og eit søk gjennom sporinga stadfestar at ingen av dei har sneke seg inn.
