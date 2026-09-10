# Release-flyt for APK og oppdatering i Spole

Dette er gjeldande arbeidsflyt frå alpha13. Ho gjeld framfor eldre publiseringseksempel i andre dokument. Publisering krev oppdrag frå brukaren; ei vanleg kode- eller dokumentasjonsendring skal ikkje automatisk bli ein release.

## Kontrakten appen forventar

- Offentleg repo: `oyvhov/spole-android`. Appen les `https://api.github.com/repos/oyvhov/spole-android/releases?per_page=100` utan GitHub-token.
- Produksjonspakke: `app.reelstack`, aldri `.debug`.
- `versionName` og tag må samsvare, til dømes `0.16.0-alpha13` og `v0.16.0-alpha13`. Bruk numeriske alpha-/beta-/rc-versjonar som `ReleaseVersion` støttar.
- `versionCode` må vere høgare enn alle APK-ar som alt er distribuerte, også lokale APK-ar. Alpha12 = 55; alpha13 = 56.
- Same eksisterande Spole-signatur. Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Releasen må vere publisert, ikkje draft, og innehalde **nøyaktig éin universal release-APK**. Legg ikkje ved ekstra ABI-/split-/debug-APK-ar.
- GitHub må oppgi `digest: sha256:...` for APK-asseten. Appen krev denne og kontrollerer filstorleik, hash, pakkenamn, minimum Android-versjon, versjon og signatur før installasjon.
- Legg også ved `SHA256SUMS.txt`, R8-mappinga for akkurat dette bygget og `SOURCE_COMMIT.txt`. Desse er ikkje APK-ar og påverkar ikkje asset-utvalet.

Alpha/beta/rc skal merkast som prerelease og ikkje erstatte den stabile `latest`-utgåva. Brukaren må ha «Testutgåver» på; det er standard når installert versjon er alpha/beta. Eldre appar utan oppdateringsfunksjonen må installerast manuelt éin gong.

## 1. Frys og kontroller kjelda

1. Les `git status`, diff og gjeldande prosjektinstruksar. Ta vare på eksisterande arbeid; ikkje bruk reset/clean for å få eit reint bygg.
2. Kontroller siste publiserte release, lokale taggar og noverande `versionCode`. Vel ei ubrukt, høgare utgåve.
3. Oppdater `app/build.gradle.kts`, `CHANGELOG.md`, release-notat og verifikasjonsrapport. Release-notata må beskrive det som faktisk blir publisert.
4. Gå gjennom alle endringar som skal med, også endringar som låg i arbeidsmappa frå før. Ikkje publiser ufrosne endringar medan ein annan prosess framleis skriv til kjelda.
5. Ingen signeringsfiler, passord, kontoar, emulator-data eller skjermbilete med ekte persondata skal leggjast til Git eller release-assets.

## 2. Bygg og test

Frå `C:\JellyBin\reelstack-android` i PowerShell 7:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot'
$env:GRADLE_USER_HOME='C:\JellyBin\.gradle-home'
$env:TEMP='C:\JellyBin\.gradle-tmp'
$env:TMP=$env:TEMP
$env:JAVA_TOOL_OPTIONS='-Djava.io.tmpdir=C:\JellyBin\.gradle-tmp'
./gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease --console=plain --max-workers=2
if ($LASTEXITCODE -ne 0) { throw 'Bygget feila; ikkje publiser.' }
```

Vent på **BUILD SUCCESSFUL og avslutta prosess**. At APK-fila finst er ikkje bevis for at test/lint er ferdig. Ikkje køyr `gradlew --stop` medan bygget framleis arbeider.

Dersom eit tidlegare Linux-bygg har etterlate absolutte Linux-stiar i KSP/Gradle-mellomlageret, køyr dei same oppgåvene med `--no-build-cache --rerun-tasks '-Pksp.incremental=false'`. Hald Windows- og Linux-byggmapper skilde framover. Ikkje bruk ein generell `clean` som slettar gamle APK-ar, mappingar og verifikasjonsloggar under `app/build`.

Køyr Android-testane på isolerte profilar (mobil 5562, TV 5566). Riktig runner er `app.reelstack.debug.test/app.reelstack.SpoleTestRunner`. Bruk aldri instrumentering på 5560/5564 med ekte kontoar. Dokumenter faktisk tal køyrde/beståtte/hoppa-over testar, lint-feil og eventuelle avgrensingar.

Start review-profilane med [emulatorskriptet](EMULATORS_WITH_REAL_DATA.md). Før publisering: installer den signerte nye APK-en med `adb install -r` på éin review-profil og kontroller at kontoar og preferansar blir bevarte. Behald den andre på førre utgåve for å prøve den ekte GitHub-oppdateringsflyten etter publisering.

## 3. Kontroller og arkiver artefaktane

```powershell
$version='0.16.0-alpha13' # Endre ved neste release
$tag="v$version"
$out="app/build/release-$tag"
New-Item -ItemType Directory -Path $out -Force | Out-Null
$apk="$out/Spole-$tag.apk"
$mapping="$out/mapping-$tag.txt"
Copy-Item app/build/outputs/apk/release/app-release.apk $apk
Copy-Item app/build/outputs/mapping/release/mapping.txt $mapping
$sdk="$env:LOCALAPPDATA/Android/Sdk/build-tools/36.0.0"
& "$sdk/apksigner.bat" verify --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'Ugyldig APK-signatur.' }
& "$sdk/aapt.exe" dump badging $apk
if ($LASTEXITCODE -ne 0) { throw 'Kan ikkje lese APK-identitet.' }
$hash=(Get-FileHash $apk -Algorithm SHA256).Hash.ToLower()
"$hash  $(Split-Path $apk -Leaf)" | Set-Content "$out/SHA256SUMS.txt" -Encoding ascii
```

Kontroller at sertifikatet er det faste sertifikatet over, at pakken er `app.reelstack`, og at versjon/versjonskode er rette. `application-debuggable` skal ikkje finnast. Skriv storleik og hash i verifikasjonsrapporten. Arkiver mappinga før eit nytt bygg eller `clean`; mapping frå ei anna utgåve kan ikkje tyde krasj frå denne APK-en.

## 4. Commit, tag og kladd

Stage berre dei gjennomgåtte filene. APK, mapping og loggar skal ikkje inn i Git. Etter commit skal ingen kjeldeendringar stå att for byggartefaktet. Dersom kjelda blir endra etter bygget, må du byggje og verifisere på nytt.

```powershell
git diff --check
git commit -m "Release Spole $version"
if ($LASTEXITCODE -ne 0) { throw 'Commit feila.' }
$commit=git rev-parse HEAD
$branch=git branch --show-current
if (!$branch) { throw 'Vel ei eksplisitt release-grein før push.' }
$commit | Set-Content "$out/SOURCE_COMMIT.txt" -Encoding ascii
git tag -a $tag -m "Spole $version"
if ($LASTEXITCODE -ne 0) { throw 'Tag finst eller kunne ikkje opprettast. Ikkje flytt han.' }
git push --atomic origin $branch $tag
if ($LASTEXITCODE -ne 0) { throw 'Push feila; ikkje publiser.' }
gh release create $tag $apk "$out/SHA256SUMS.txt" $mapping "$out/SOURCE_COMMIT.txt" --repo oyvhov/spole-android --verify-tag --draft --prerelease --latest=false --title "Spole $version" --notes-file "docs/release-$tag.md"
if ($LASTEXITCODE -ne 0) { throw 'Release-kladd feila.' }
```

Dette eksempelet er for ei testutgåve. For ei ferdig stabil utgåve: ikkje bruk `--prerelease`, og vel `--latest=true`. Aldri flytt ein publisert tag eller byt ut APK-en bak same versjon; lag ny versjon og versjonskode ved feil.

Kontroller kladden med `gh release view $tag --repo oyvhov/spole-android --json tagName,isDraft,assets`: nøyaktig éin APK, `state=uploaded`, riktig byte-storleik og `digest` lik `sha256:$hash`. Tag-endepunktet i REST kan returnere 404 for ein kladd. Dersom du treng rå REST-data, finn den numeriske release-ID-en i den autentiserte release-lista og bruk `gh api repos/oyvhov/spole-android/releases/ID`. Stadfest også at taggen peikar på `$commit`. Dersom GitHub ikkje har gitt asseten ein digest enno, vent og les metadata på nytt før publisering.

## 5. Publiser og prøv den ekte oppdateringa

```powershell
gh release edit $tag --repo oyvhov/spole-android --draft=false --prerelease --latest=false
if ($LASTEXITCODE -ne 0) { throw 'Publisering feila.' }
```

1. Hent release-lista **utan Authorization-header** og stadfest at ho inneheld denne utgåva, `draft=false` og rett asset-digest.
2. Last ned den offentlege `browser_download_url` til ei eiga kontrollfil. Samanlikn SHA-256 med den bygde APK-en. Dette avdekkjer både feil ved opplasting og feil nedlastingslenkje.
3. På review-emulatoren som framleis har førre oppdateringsdyktige utgåve: opne **Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no** (på TV: **Notifications and updates → App updates → Check now**).
4. Stadfest ny versjon, vel Last ned, vent på kontroll og vel Installer. Dersom Android ber om «Tillat frå denne kjelda», opne løyvet for Spole, gå tilbake og vel Installer igjen. Godkjenn Android sin installasjonsdialog.
5. Opne appen att og kontroller installert versjonskode, kontoar og ekte bibliotekinnhald. Dokumenter feil eller grenser nøyaktig. Ikkje kall ein `adb install -r`-test ein test av nedlasting gjennom appen.

Automatisk sjekk skjer ved opning/retur og er avgrensa til éin gong per tolv timar, også etter feil. **Sjekk no** går utanom ventetida. «Seinare» skjuler berre varselet for den aktuelle utgåva. Appen lastar ikkje ned eller installerer stille; brukaren startar nedlastinga og Android godkjenner installasjonen.

## 6. Sluttrapport

Returner direkte APK-lenkje, release-lenkje, versjon/versjonskode, commit, kort endringsliste og testresultat. Oppdater verifikasjonsrapporten med faktisk resultat frå oppdateringstesten. Ei etterfølgjande rein rapport-commit kan dokumentere dette utan å flytte release-taggen eller byggje om den publiserte APK-en.
