# Verifisering av Stova-lenkjer

Oppdatering 25. oktober 2026: signert lokal beta21 er installert på Shield. Etter at brukaren logga inn, er ekte varm og kald detaljopning for «One Battle After Another» stadfesta med `spole://open` og `action=details`. Sjå [den nye lokale verifiseringa](VERIFICATION_v1.0.0-beta21-local.md) for resultat og grenser. Funksjonen er framleis ikkje offentleg publisert.

Historisk status før lokal Shield-oppdatering: kjeldekode implementert og JVM-testar, debug-bygg og lint grøne. Instrumentering og manuell TV-kontroll var ikkje køyrde, etter brukaren sitt uttrykkelege val om å hoppe over Task 4 steg 2 og 3. [Kontrakten](LAUNCHER_LINK.md) skil forventa åtferd frå faktisk einingsbevis.

## Omfang og arbeidsmappe

- Grein: `feat/spole-launcher-link` i `C:/JellyBin/spole-launcher-link`.
- Task 1: `1e83fbb` — streng parser.
- Task 2: `7c525a6` — oppløysing mot kontoar og barnemodus.
- Task 3: `212de78` — mottakar, ventande opning, detaljar og spelarruting.
- Android sitt forbod mot ikkje-ASCII-teikn i byggestien blokkerte den opphavlege arbeidsmappa, som hadde slike teikn i stien. Bygg og implementasjon vart difor gjorde i den isolerte ASCII-arbeidsmappa over. Lokal `local.properties` vart kopiert frå `C:/JellyBin/reelstack-android`; hovudarbeidsmappa til brukaren vart ikkje endra.
- Appversjonen er uendra: `versionCode = 150`, `versionName = "1.0.0-beta13"` (debug har suffikset `-debug`). Ingen versjonsauke, push eller release.

Task 4 endrar berre dokumentasjon. Ingen nye Gradle-køyringar vart utførte for dokumentasjonsendringa; tala nedanfor kjem frå kjeldekodeverifiseringa ved `212de78` og vart kontrollerte mot eksisterande loggar og test-XML.

## TDD og automatiske resultat

| Steg | RED før implementasjon | GREEN / resultat |
| --- | --- | --- |
| Task 1, parser | Kompilering feila på manglande typar. | 12 av 12 testar bestod. |
| Task 2, kontoval | Kompilering feila på manglande oppløysar. | 18 av 18 testar bestod. |
| Task 3, åtferd | 19 testar, 14 feila. | Retta i integrasjonen. |
| Barnedetaljar | 4 testar, 4 feila. | Eige barneskal på plass. |
| Stor skrift, kontrollar og kunst | 7 testar, 2 feila. | JVM-kontroll av skriftstorleik 2.0, minst 64 dp kontrollar og kunstreglar. |
| Heim ved ekstern opning | Éin feilande regresjonstest. | Heim og detaljnavigasjon retta. |
| Heim i barneskalet | Éin feilande regresjonstest. | Barnet sin lokale navigasjon blir nullstilt. |
| Sesonglenkjer | 4 testar, 1 feila. | Foreldreserie og etterspurd sesong blir brukte. |
| Endeleg målretta Task 3-køyring | — | 31 av 31 bestod, ingen feil eller hoppa over. |

Den endelege kjeldekodekontrollen brukte denne PowerShell-kommandoen:

```powershell
$env:TEMP = 'C:/JellyBin/.gradle-tmp'
$env:TMP = $env:TEMP
./gradlew.bat --gradle-user-home C:/JellyBin/.gradle-home testDebugUnitTest assembleDebug lintDebug
```

Resultat: `BUILD SUCCESSFUL in 3m 13s`, avsluttingskode 0.

| Kontroll | Resultat |
| --- | --- |
| Før endringa, JVM-baseline | 1028 totalt: 1027 bestod, 1 hoppa over, 0 feil. |
| Endeleg JVM-testpakke | 1089 totalt: 1088 bestod, 1 hoppa over, 0 failures, 0 errors. |
| Lint | 0 feil, 197 åtvaringar, 1 hint. |
| Debug-APK | `app/build/outputs/apk/debug/app-debug.apk` vart bygd og finst lokalt. |
| Diff-kontroll | `git diff --check` rein. |

Den eine testen som vart hoppa over er `app.reelstack.data.network.LanServerDiscoveryTest.realNetworkWhenAskedFor`, ein eksisterande nettverkstest som krev eksplisitt aktivering. Han vart også hoppa over i baseline; dette er ikkje ein ny feil eller ein utført nettverkskontroll.

JVM-dekninga omfattar parser, kontoval, Robolectric-testar av mottakar og ventande opning, øktgrenser og seine svar, detaljfeil, cachetreff med kjelde og ekstern ID, lasting utan varm cache, barnet sine enkle detaljkontrollar og rein sesongruting. Robolectric og Compose-testar på JVM er ikkje Android-instrumentering, skjermbilete eller fysisk TV-verifisering.

Lokale bevis (ikkje Git-artefaktar):

- `C:/JellyBin/.gradle-tmp/spole-launcher-baseline.log`.
- `spole-launcher-task1-red.log`, `spole-launcher-task1-green.log`, `spole-launcher-task2-red.log` og `spole-launcher-task2-green.log` i same mappe.
- `spole-launcher-task3-red.log`, `spole-launcher-task3-child-red.log`, `spole-launcher-task3-polish-red.log`, `spole-launcher-task3-home-red.log`, `spole-launcher-task3-child-home-red.log` og `spole-launcher-task3-season-red.log` i same mappe.
- `spole-launcher-task3-green.log` (31 målretta testar) og `spole-launcher-task3-full.log` (endeleg bygg, totalsummar og lint) i same mappe.
- `app/build/test-results/testDebugUnitTest/TEST-*.xml` og `app/build/reports/lint-results-debug.html` i ASCII-arbeidsmappa.

## Ikkje køyrt og attståande verifisering

Etter brukaren sitt val vart ingen emulator starta, ingen APK installert og ingen ADB-kommando køyrd. `emulator-5560`, `emulator-5562` og `emulator-5564` vart ikkje brukte. Ingen instrumentering, manuell TV-prøve, skjermbilete eller fysisk einingsprøve dokumenterer denne funksjonen. Debug-APK-en er ikkje publisert og kan ikkje oppdatere produksjonspakken.

Desse punkta står opne til ei seinare uttrykkeleg autorisert økt:

- [ ] Køyr relevante Android-instrumenteringstestar på den isolerte testprofilen etter prosjektreglane; dokumenter tal og resultat.
- [ ] Frå Stova: opne `details` ved kald og varm appstart, både med og utan tittel i cache. Kontroller Heim, metadata, skeleton, ordinær feiltilstand og tilbakesteg på TV.
- [ ] Opne `play` for ei påbyrja avspeling og kontroller faktisk vidareføring av posisjon med den eksisterande spelarflyten.
- [ ] Opne ein annan tittel medan noko spelar. Kontroller byte av tittel, stoppa førre økt, framdrift og tilbakehistorikk utan stabla spelarar.
- [ ] Prøv hovudkonto, kjend barnekonto og ukjend konto. Inne i barnemodus skal eigen konto opne, medan vaksenkonto og anna barn blir stoppa med rett melding; kontroller at inga vaksenflate eller andre legitimasjonar blir brukte.
- [ ] Prøv serie-/sesonglenkje og episodeval i barnet sitt eige skal. Kontroller fjernkontrollfokus og skriftstorleik 2.0 visuelt på TV.

Ingen av dei opne punkta er markerte som verifiserte av bygg- eller JVM-resultata over. Eventuell seinare testing med ekte data må følgje [emulatorreglane](EMULATORS_WITH_REAL_DATA.md) og bevare dei eksisterande innlogga profilane.
