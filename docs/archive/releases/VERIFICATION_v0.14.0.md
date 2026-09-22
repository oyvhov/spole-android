# Verifisering – Spole 0.14.0

## Endring

Publiseringsrunden frå `docs/PUBLISHING_READINESS.md`, pluss dei to opne punkta frå
testgjennomgangen 8. september: den utdaterte innstillingstesten og dei utydelege søketreffa.

Dei tre endringane med størst risiko:

- **Einings-ID er ikkje lenger `Settings.Secure.ANDROID_ID`.** Jellyfin registrerer ei eining mot
  ID-en i `Authorization`-hovudet, så ein ny verdi ved oppgradering ville gitt alle eksisterande
  brukarar ei duplisert einingsoppføring — og i verste fall ei avvist innlogging. `DeviceIdentity`
  les difor den gamle verdien éin gong og kopierer han inn i eiga lagring. Berre nye installasjonar
  får ein tilfeldig UUID. Dette er testa ende-til-ende på ei ekte produksjon-til-produksjon-
  oppgradering, sjå under.
- **22 `runCatching`-blokker i ViewModel er bytte til `attempt`.** Kvar av dei kastar no
  `CancellationException` vidare i staden for å behandle han som ein feil. Ni av dei gjorde det
  allereie manuelt; dei manuelle sjekkane er fjerna sidan hjelparen eig regelen no.
- **Bibliotek-søket dedupliserer på verket, ikkje på kopien.** Søkeførespurnaden ber no om
  `ProviderIds` og `PremiereDate`, som ikkje vart henta før.

## Automatiske kontrollar

- **225/225 JVM-testar** bestod (var 203 før denne utgåva).
- **82/82 Android-testar** bestod på isolert emulator 5562, i éi rein køyring på 168,8 sekund.
  `ReelstackSmokeTest.homeSectionsCanBeEditedInSettings` er retta og bestod.
- Lint: **0 feil, 18 åtvaringar** (var 26). `HardwareIds` ×3 og `InsecureBaseConfiguration` er
  borte; `UnusedResources` gjekk frå 4 til 0.
- `testDebugUnitTest lintDebug assembleRelease assembleDebug assembleDebugAndroidTest` med
  Windows-Gradle og JDK 21.

22 nye testar dekkjer: omdirigeringsmeldinga med og utan `Location`, med relativ adresse og med
eksplisitt port; at tilkoplingstesten rapporterer omdirigeringa i staden for statusnummeret;
`Retry-After` i eintal og fleirtal; at berre `ServiceMessage` reknast som tekst skriven for
brukaren; at den versjonerte tilrådingsstien blir spurd først og at 404 — men ikkje 500 — fell
tilbake; at same serie på to tenarar blir eitt treff både med og utan `ProviderIds`; at episodar
forsvinn når serien deira er med, men overlever når han ikkje er det; at to ulike verk med same
namn begge blir verande; og at krasjloggen fjernar adresser og tilgangsteikn men lèt klasse- og
metodenamn stå.

## Emulator

Oppgraderingstesten som `docs/AI_INSTRUCTIONS.md` krev vart utført på `Spole_Review` (5560), som
hadde ekte innlogga kontoar på Jellyfin, Emby og Seerr frå gjennomgangen tidlegare same dag.

1. Installert versjon før: `app.reelstack`, versionCode 38, versionName 0.13.3.
2. Produksjons-APK 0.14.0 installert med `adb install -r` over den.

Resultat etter oppgraderinga:

- Appen opna direkte i Heim. Førstegongsoppsettet kom ikkje tilbake.
- **Profilbiletet lasta.** Det er den avgjerande kontrollen for `DeviceIdentity`: profilkallet er
  autentisert, så migreringa av einings-ID-en heldt innlogginga i live.
- «Hald fram å sjå» viste ekte framdrift, og Jellyfin- og Emby-radene stod framleis kvar for seg
  med ekte plakatar. Ingen demodata.
- Oppdag lasta ekte Seerr-innhald.
- **Søk etter «Silo» gav no eitt treff under «I biblioteka dine»**, der gjennomgangen tidlegare
  same dag rapporterte fleire nesten identiske kort. Dette er den opne saka frå
  testgjennomgangen, stadfesta mot dei same levande tenarane som fann henne.
- Innstillingar viste v0.14.0, tre HTTPS-tilkoplingar, og det nye attribusjonskortet.
  Krasjrapportrada var ikkje synleg, som ho skal vere når ingen krasj er registrert.

Ingen kontoar vart nullstilte, ingen instrumenteringstestar køyrde på 5560, ingen førespurnader
sende eller trekte tilbake, og inga avspeling endra.

## 16 KB-sidestorleik

Begge halvdelane er kontrollerte, ikkje berre den eine:

- `zipalign -c -P 16 -v 4` → «Verification successful». Alle fire `.so`-filer ligg på
  16 KB-grense i APK-en.
- ELF-programhovuda i kvar `.so` er lesne direkte: alle tre LOAD-segmenta i alle fire ABI-ar har
  `p_align = 16384`.

Appen har berre eitt innfødd bibliotek, `libandroidx.graphics.path.so`, som følgjer med AndroidX.

## Produksjonsfil

- Versjon 0.14.0, versionCode 39, pakke `app.reelstack`, minSdk 26, targetSdk 36.
- Storleik: **2 235 340 byte**, ned frå 7 139 645 i 0.13.3. Ein reduksjon på 69 %.
  5,1 MB av det gamle bygget var tre demo-PNG-ar; dei er no 197 KB WebP.
- WebP-konverteringa er målt: PSNR 43,1 / 43,5 / 46,9 dB (luma) for dei tre bileta. Over 40 dB
  reknast som visuelt uskiljeleg.
- Signatur SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` — same
  produksjonsnøkkel som 0.13.3, stadfesta med `apksigner verify --print-certs`. APK-en kan
  installerast som ei vanleg oppdatering, og det er faktisk gjort i oppgraderingstesten over.

SHA-256 for publiserings-APK-en:
`cce537528fa41f1c80573cb35520b122fae3f64af9066f7147821a9faec9f59b`.
Publiseringskontrollen køyrde bygg og lint på nytt (uendra oppgåver brukte byggcache),
og stadfesta produksjonssignaturen. R8-mappinga følgjer GitHub-releasen.

## Ikkje verifisert

- **Omdirigeringsmeldinga mot ein ekte omvend proxy.** Åtferda er einingstesta med fabrikkerte
  301/302-svar, men ikkje køyrd mot ein tenar som faktisk sender `Location`.
- **Krasjloggen etter ein ekte krasj.** Skrubbinga er einingstesta og rada er bygd, men ingen
  krasj er framprovosert på ei eining for å sjå heile flyten.
- **Widgeten** er ikkje plassert på ein heimeskjerm og målt mot det nye 8-sekunders-budsjettet.
- **Retry på GET** er ikkje observert mot eit nett som faktisk mistar det første forsøket.
- **`v1/recommendations.json` finst ikkje enno** i `oyvhov/spole-recommendations`. Appen fell
  tilbake til den gamle stien, som er akkurat det reserven er til for, men den versjonerte stien
  bør opprettast før dette blir gitt ut.
- **Radarr og Sonarr** er ikkje tilkopla på review-emulatoren, så kalender og køar er framleis
  ikkje kontrollerte mot levande tenester.
