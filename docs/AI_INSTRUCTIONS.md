# AI-instruksar for Spole

Dette dokumentet er arbeidsmanualen for ein AI-agent som skal utvikle, teste, feilsøke og publisere Spole. Det er skrive for dette prosjektet, ikkje som generelle Android-råd.

## 1. Produktet og faste føringar

Spole er ein innfødd Android-app for eit sjølvhosta mediesystem. Appen samlar Jellyfin, Emby, Seerr, Radarr og Sonarr i éi roleg og moderne oppleving.

Følg desse føringane i alle endringar:

- Bruk nynorsk i brukargrensesnitt, feilmeldingar, tomtilstandar og dokumentasjon.
- Bevar Spole-namnet, logoen og den matte, mørke, filmatiske profilen.
- Hald uttrykket moderne og elegant, men unngå generisk glassmorfisme, tunge rammer og pynt utan funksjon.
- Lime er handlingsfargen. Han skal aldri brukast på tal, typemerke, kjeldenamn eller annan passiv status.
- Ingen kontroll skal ha fast høgd som teksten må passe inni. Test alltid på skriftstorleik 2.0, ikkje 1.5:
  Android skalerer store storleikar mindre, så 1.5 skjuler brot som 2.0 viser.
- Alt innhald skal ligge i éin lesbar kolonne (`ReelPage`). Breie vindauge får sidenavigasjon, ikkje ein
  botnbar strekt over heile breidda.
- Bruk ekte data når tenesta er kopla til. Demo-data skal berre visast i eksplisitt demo-modus.
- Heimesida skal kunne vise innhald frå alle kopla tenester utan at brukaren må byte server.
- Jellyfin og Emby skal ha eigne rader for filmar og seriar; dei skal aldri blandast til éi rad.
- Barneseriar og barnbibliotek skal ikkje takast med i standardradene.
- Vanlege brukarar skal berre sjå sitt eige innhald, si eiga aktivitet og eigne førespurnader. Administratorar kan få delte køar og oversikter når serverrettane tillèt det.
- Brukaridentiteten skal kome frå den innlogga brukaren, særleg frå Seerr-profilen når ein sender ei førespurnad.
- Førespurnader skal ha ein tydeleg flyt: "Førespurd" → "Lastar ned" → "I biblioteket".
- Varsel når noko kjem i biblioteket er på som standard, men kan skruast av i førespurnadspopupen.
- Ingenting som spelar skal ikkje ta plass når det ikkje finst aktive avspelingar.
- Ved fleire aktive avspelingar skal talet vere tydeleg, og kvar økt skal kunne opnast separat.
- Siste lagt til, komande innhald, kalender, oppdag, aktivitet og detaljpoppup skal vere innhaldstenlege og bruke riktig kunstformat.
- Filmcover skal ikkje få kunstige botnkanter. Film bruker poster; seriar og episodar bruker brei thumbnail når tenesta tilbyr det.
- Popupark skal ha stabil høgd og mjuk animasjon. Ikkje animer høgda på nytt kvar gong eit bilde eller ein tekstbit lastar inn.

Les også README.md, docs/PRODUCT_PLAN.md, docs/LAYOUT.md, docs/DESIGN_SYSTEM.md, docs/VIEWER_ACCESS.md og docs/REQUEST_FLOW.md før ei større endring.

## 2. Før du endrar noko

1. Stadfest at du står i C:/JellyBin/reelstack-android.
2. Les status og diff før du rører filer. Eksisterande endringar tilhøyrer brukaren og skal ikkje overskrivast.
3. Finn relevant kode med rg, ikkje med langsame rekursive søk.
4. Følg dataflyten frå modell → nettverksklient → repository → ViewModel → Compose UI før du endrar eit felt.
5. Finn eksisterande test for området og utvid den før du legg til ny logikk.
6. Bruk apply_patch for tekstfiler. Ikkje bruk destruktive Git-kommandoar eller slett bygg-/brukardata for å skjule ein feil.

Typiske område:

- app/src/main/java/app/reelstack/data/network/: HTTP, innlogging og tenesteformat.
- app/src/main/java/app/reelstack/data/repository/: lagring, synkronisering og førespurnadsstatus.
- app/src/main/java/app/reelstack/ui/: skjermar, popupark, state og navigasjon.
- app/src/main/java/app/reelstack/ui/components/: gjenbrukbare kort, skeletons, kunst og tenestemerke.
- app/src/test/: einingstestar for mapping, autentisering og datareglar.
- app/src/androidTest/: Compose- og integrasjonstestar på emulator.
- docs/: produkt-, design-, tryggleiks- og release-dokumentasjon.

## 3. Bygging

### Produksjon frå 0.11.7 (gjeld framfor eldre debug-eksempel under)

- Publiser `assembleRelease` → `app/build/outputs/apk/release/app-release.apk`, aldri debug-APK til telefonoppdateringar.
- Pakke: `app.reelstack`. Bevar eksisterande nøkkel i `C:/JellyBin/.spole-signing/spole-release.jks` og lokal, Git-ignorert `signing.properties`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Manglande nøkkel er ein byggblokkering. Ikkje generer erstatningsnøkkel eller byt pakkenamn for å kome rundt ein installasjonsfeil.
- Installer produksjons-APK-en med `-r` over førre produksjonsversjon på testemulatoren før publisering. Sjekk at lokale val er bevarte.
- I Windows-miljøet her brukar Gradle `TEMP`/`TMP=C:/JellyBin/.gradle-tmp` og `--gradle-user-home C:/JellyBin/.gradle-home`.
- AVD-data skal liggje utanfor `app/build` slik at ei normal opprydding av byggfiler ikkje slettar emulatoren.
- Frå review 0.12.0 finst isolert WSL-test-AVD i `C:/JellyBin/.spole-test-avds`, starta med `ANDROID_AVD_HOME=/mnt/c/JellyBin/.spole-test-avds`, Linux-emulatoren og `-gpu swiftshader` på port 5562.
- Den gamle `Tunet_Test`-installasjonen viste førstegongsoppsett under denne gjennomgangen. Ikkje gå ut frå at kontoane framleis er innlogga.

### Vanleg lokal bygging

Frå PowerShell:

~~~powershell
./gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
~~~

APK-en blir laga her:

~~~text
app/build/outputs/apk/debug/app-debug.apk
~~~

### Reproduserbar bygging i WSL

I denne arbeidsstasjonen finst eit Linux-JDK og Android SDK i WSL. Når Windows-Gradle manglar riktig Java-versjon, bruk ei Linux-byggemappe i staden for å endre systemoppsettet:

~~~powershell
wsl.exe -e bash -lc 'cp -a /mnt/c/JellyBin/reelstack-android/app/src/. /home/oyvhov/homereel-v080-qlBgQX/app/src/ && cp /mnt/c/JellyBin/reelstack-android/app/build.gradle.kts /home/oyvhov/homereel-v080-qlBgQX/app/build.gradle.kts && cd /home/oyvhov/homereel-v080-qlBgQX && export JAVA_HOME=/home/oyvhov/.local/share/home-reel-jdk && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew --no-daemon --max-workers=1 clean testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug'
~~~

Kopier berre kjelde og nødvendig byggkonfigurasjon til byggemappa. Kopier aldri hemmelege filer eller emulator-data.

Før ein release må desse vere dokumenterte:

- einingstestar: tal bestått / tal totalt
- Android-testar: tal bestått / tal totalt
- lint-feil
- APK-versjon, pakkenamn, storleik og SHA-256
- signeringssertifikatets SHA-256 dersom APK-en skal oppdatere ei eksisterande installasjon

## 4. Emulatorar og trygg testing

Det finst to ulike emulatorroller. Bland dei aldri.

| Serial | Rolle | Viktig regel |
|---|---|---|
| emulator-5560 | Vanleg review-emulator med ekte brukardata | Installer med -r; slett aldri data; køyr aldri testpakka her |
| emulator-5562 | Isolert instrumenteringsemulator | Kan nullstillast av testane; bruk denne for heile Android-testpakken |

Review-emulatoren `emulator-5560` er den vanlege `Tunet_Test`-AVD-en med lagra brukar-/appdata. Han skal
alltid startast med eksisterande data og oppdaterast med `adb install -r`; ikkje avinstaller, nullstill eller
køyr instrumenteringstestar der. Dersom 5560 ikkje er tilgjengeleg, skal visuell review gjerast med
demodata på den isolerte `HomeReel_Instrumentation` på 5562, og det skal seiast tydeleg i verifiseringa
kva som faktisk vart testa.

Emulatoren må startast i same kommando som du brukar han. Startar du han i ein eigen bakgrunnsjobb, blir
prosessen teken ned når det skallet avsluttar, og neste ADB-kommando finn ingen einingar. Bruk `-gpu
swiftshader`; `swiftshader_indirect` segfaultar på denne maskina.

Start testemulatoren frå prosjektet:

~~~powershell
wsl.exe -u root -e bash /mnt/c/JellyBin/reelstack-android/app/build/test-avds/start-headless.sh
~~~

Sjekk status med den vanlege ADB-tenesta:

~~~powershell
wsl.exe -u root -e bash -lc '/home/oyvhov/Android/Sdk/platform-tools/adb devices'
~~~

Vent til emulatoren står som device, ikkje offline. Ikkje set ANDROID_ADB_SERVER_PORT=5038 i denne arbeidsflyten; det kan lage ein ekstra ADB-tenar og gi falsk offline-status.

### Android-testpakke

Installer den same endelege APK-en og test-APK-en på emulator-5562:

~~~powershell
wsl.exe -u root -e bash -lc '/home/oyvhov/Android/Sdk/platform-tools/adb -s emulator-5562 install -r /home/oyvhov/homereel-v080-qlBgQX/app/build/outputs/apk/debug/app-debug.apk && /home/oyvhov/Android/Sdk/platform-tools/adb -s emulator-5562 install -r /home/oyvhov/homereel-v080-qlBgQX/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk && /home/oyvhov/Android/Sdk/platform-tools/adb -s emulator-5562 shell am instrument -w app.reelstack.debug.test/androidx.test.runner.AndroidJUnitRunner'
~~~

Krav: testen skal avsluttast med OK, og talet skal dokumenterast i verifiseringsfila for releasen.

### Review av ekte UI

Når instrumenteringstesten er ferdig, kan den nye APK-en installerast på emulator-5560 utan å miste kontoar:

~~~powershell
wsl.exe -u root -e bash -lc '/home/oyvhov/Android/Sdk/platform-tools/adb -s emulator-5560 install -r /home/oyvhov/homereel-v080-qlBgQX/app/build/outputs/apk/debug/app-debug.apk && /home/oyvhov/Android/Sdk/platform-tools/adb -s emulator-5560 shell monkey -p app.reelstack.debug -c android.intent.category.LAUNCHER 1'
~~~

Kontroller visuelt:

- toppmargen, statuslinje og botnavigasjon overlappar ikkje innhald
- ved skriftstorleik 2.0 blir ingen ikon klipt, ingen tekstlinje forsvinn og ingen etikett bryt midt i eit ord
- på brei skjerm står innhaldet i éin kolonne, og navigasjonen er ei sideliste
- profilikon og tenestemerke viser riktig innlogga identitet
- ekte posters og thumbnails er skarpe og ikkje kutta feil
- eigne Jellyfin- og Emby-rader er separate
- skeletons har same form og storleik som innhaldet dei erstattar
- popupark opnar éin gong, har stabil storleik og hoppar ikkje når tekst/bilde lastar
- tom tilstand er diskret og ikkje ei stor feilside
- vanleg brukar ser berre si eiga aktivitet og sine eigne avspelingsøkter
- admin ser berre adminfunksjonar når kontoen faktisk har adminrettar

Ta skjermbilde berre til visuell kontroll. Ikkje legg skjermbilde med persondata, tokens eller tenesteadresser i Git.

## 5. Innlogging og tenestereglar

### Jellyfin

- Quick Connect skal vere den lettaste standardflyten.
- Brukar og passord, og avansert tilgangsteikn, skal vere alternative val.
- Profilen kan hentast frå Users/Me etter vellukka innlogging.
- Brukaren skal få ei konkret melding ved feil passord, nettverk, adresse eller serverfeil.

### Emby

- Lokal Emby-konto skal kunne logge inn med brukarnamn og passord.
- Emby-autentisering skjer med Users/AuthenticateByName.
- Bruk User.Id frå det vellukka innloggingssvaret når profilen skal stadfestast.
- Emby har ikkje Jellyfin sin pålitelege Users/Me-flyt i denne integrasjonen. Profilen skal hentast frå Users/{Id}.
- Ikkje konkluder med at passordet er feil når autentiseringa lykkast, men profilkallet feilar. Vis riktig server-/nettverksfeil.
- Reelle passord skal aldri brukast i automatiske testar, loggar, issue-tekst eller release-notat.

### Seerr

- Seerr kan bruke Jellyfin-identiteten gjennom Seerr si eiga innlogging.
- Førespurnader skal sendast på vegner av den verifiserte Seerr-brukaren.
- Serverens Seerr-rettar avgjer kva ein vanleg brukar kan førespørje.
- Adminnøklar er for oversikt/lesing når det er nødvendig, ikkje for å late som ein annan brukar sender førespurnaden.
- Dersom Seerr-session eller CSRF-token går ut, skal brukaren få ein roleg innloggingsflyt tilbake.

### Radarr og Sonarr

- Bruk dei for søk, komande filmar/episodar, førespurnadsstatus og admin-køar der rettane tillèt det.
- Ikkje vis kinopremiere som om tittelen er tilgjengeleg. Komandevisinga skal prioritere innhald som kan bli lasta ned/strauma.
- Del film og serie visuelt, og vis status frå den faktiske kjelda.

## 6. Feilsøking av nettverk og autentisering

Feilsøk i denne rekkefølgja:

1. Valider og normaliser tenesteadressa med EndpointValidator.
2. Test public system-info/helse-endepunkt utan å skrive ut tokens.
3. Kontroller HTTP-status, men ikkje logg svarbody dersom han kan innehalde persondata eller hemmelegheiter.
4. Kontroller at riktig tenesteklient og riktig profil-endepunkt blir brukt.
5. Kontroller at vellukka autentisering faktisk lagrar token og brukar-ID før profilkallet.
6. Lag ein isolert einingstest for feilstatusen.
7. Køyr Android-test på emulator-5562.
8. Prøv reell konto manuelt berre når brukaren sjølv har gjort emulatoren klar for det.

For Emby-feilen som tidlegare viste «Emby er utilgjengeleg» var den viktige kontrollen å skilje mellom:

~~~text
POST /Users/AuthenticateByName
GET  /Users/{User.Id}
~~~

Ikkje byt tilbake til GET /Users/Me for Emby berre fordi stien fungerer for Jellyfin.

## 7. Endringar i UI og produktflyt

Når ein skjerm blir endra, vurder heile flyten:

- lastestatus → innhald → feil → tom tilstand
- første gong → innlogga → oppdatert → utlogga
- vanleg brukar → administrator
- éi aktiv økt → fleire aktive økter
- ikkje førespurd → førespurd → lastar ned → i biblioteket
- film → serie → sesong → episode

Detaljpanelet skal hente så mykje ekte metadata som tenesta tilbyr: samandrag, sjanger, årstal, spilletid, vurdering, sesong/episode, kjelde, bibliotekstatus og relevante handlingar. Manglande felt skal falle roleg bort, ikkje lage tomme bokser.

Alle trykkbare handlingar skal ha:

- stor nok treffflate
- tydeleg trykktilbakemelding
- ventestatus ved nettverkskall
- feiltilbakemelding med neste steg
- ingen dobbel innsending ved raske trykk

## 8. Versjonering, commit og GitHub-publisering

Publiser berre når brukaren ber om publisering, eller når den etablerte oppgåva uttrykkeleg krev ein ny release. Dokumentasjonsendringar åleine treng ikkje ny APK-release.

### Før commit

~~~powershell
git status --short
git diff --check
git diff --stat
~~~

Kontroller at berre relevante filer er med. Bruk ein forklarande commit, til dømes:

~~~powershell
git add <relevante filer>
git commit -m "Fix Emby account login verification"
~~~

GitHub-repoet `oyvhov/reelstack-android` er privat. Release-lenkjer krev difor innlogging som eigar, og
kan ikkje delast som ei open nedlastingslenkje. Ikkje vis til releasen som «offentleg tilgjengeleg».

### Ny app-release

1. Oppdater versionCode og versionName i app/build.gradle.kts.
2. Oppdater docs/release-vX.Y.Z.md og lag docs/VERIFICATION_vX.Y.Z.md.
3. Bygg rein APK og køyr alle testane.
4. Verifiser signering, pakkenamn, versjon og hash.
5. Lag ein annotated tag som samsvarer med versjonen.
6. Push branch og tag atomisk.
7. Last opp APK og SHA256SUMS.txt som ein publisert GitHub Release.
8. Verifiser at releasen ikkje er draft, at APK-en kan lastast ned, og at GitHub-digest samsvarer med lokal hash.

Eksempel:

~~~powershell
$releaseApk = "app/build/release-vX.Y.Z/Spole-vX.Y.Z-debug.apk"
New-Item -ItemType Directory -Force -Path "app/build/release-vX.Y.Z" | Out-Null
Copy-Item -LiteralPath "app/build/outputs/apk/debug/app-debug.apk" -Destination $releaseApk -Force
Get-FileHash -Algorithm SHA256 $releaseApk
git tag -a vX.Y.Z -m "Spole X.Y.Z"
git push --atomic origin main vX.Y.Z
gh release create vX.Y.Z $releaseApk "app/build/release-vX.Y.Z/SHA256SUMS.txt" --repo oyvhov/reelstack-android --title "Spole X.Y.Z" --notes-file "docs/release-vX.Y.Z.md" --latest
~~~

Ikkje legg release-APK i Git dersom app/build/ er ignorert. GitHub Release er nedlastingspunktet.

Fast informasjon som skal returnerast til brukaren etter publisering:

- direkte APK-lenkje
- GitHub Release-lenkje
- versjon og commit
- kort liste over kva som er fiksa
- testresultat
- SHA-256 ved behov

## 9. Tryggleik og personvern

- Passord skal berre sendast til den tenesta brukaren skriv inn, og aldri lagrast.
- Tilgangsteikn og Seerr-sessionar skal lagrast kryptert.
- Tokens skal aldri ligge i URL, logg, skjermbilde, cache, test-fixtures eller commit.
- Ikkje skriv full HTTP-respons til loggen frå autentisering eller profilkall.
- Ikkje hent admin-aktivitet for vanlege brukarar.
- Ikkje bruk ein tidlegare brukars cached dashboard etter innlogging med ein ny brukar.
- Ikkje be brukaren lime inn eit passord i chatten. Be heller brukaren skrive det direkte i emulatoren/appen.
- Bruk HTTPS når tenesta er utanfor privat LAN. Private LAN- og localhost-adresser følgjer prosjektet sine valideringsreglar.

## 10. Ferdig-kriterium

Ei oppgåve er ikkje ferdig før alle relevante punkt er oppfylte:

- funksjonen er implementert i riktig lag
- nynorsk tekst og tom-/feiltilstand er på plass
- eksisterande konto- og tilgangsreglar er bevarte
- einingstest dekkjer den nye logikken
- Android-test eller manuell emulator-review er utført
- ingen token, passord eller persondata er lagt i diff/logg
- git diff --check er rein
- dokumentasjon/release-notat er oppdatert dersom endringa krev det
- brukaren får ei kort forklaring på resultatet og ei direkte lenkje dersom det er publisert APK

Når noko ikkje kan verifiserast, sei nøyaktig kva som er testa, kva som ikkje er testa, og kvifor. Ikkje kall ei innlogging «verifisert» dersom eit ekte passord aldri vart brukt; kall heller kodeflyten og emulator-testen verifisert.

