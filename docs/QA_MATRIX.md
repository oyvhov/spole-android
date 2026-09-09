# Spole — testmatrise og oppstart av veikartet

Oppretta 8. september, oppdatert 9. september 2026. Første dokumentasjonsrunde brukte 0.15.1 / 43, commit `265321c`. Nattrunden implementerer appfunksjonar i **0.16.0-alpha01 / 44**. [Gjeldande veikart](../ROADMAP.md).

## Nyaste oppfølging — minimerbar sidemeny, alpha04 / 47

[Sidemenyrapporten](SIDEBAR_ALPHA04.md) dokumenterer fullhøgd ikonrad/utvida meny, lagra breiddeval og fjernkontrollførebuing. Teststatus blir ført der; fysisk TV er framleis ope.

## Førre oppfølging — personleg flyt og nettbrett, alpha03 / 46

[Alpha03-rapporten](ROADMAP_BATCH_ALPHA03.md) er nyaste testkjelde: fjerna nettbrettsnarveg til søk, sidestilte episodedetaljar, engelsk konto-/førespurnadsflyt og ingen strekk ved scrollkanten inne i popupane. Utvida med aksent/biletstorleik, automatisk framhald, kant-til-kant mediarader, brei sidemeny, todelte innstillingar og immersive-video. Den gamle halden-peikar-testen er køyrd uendra og fullfører no på nettbrett. Endelege tal og attståande fysisk verifisering står i rapporten.

## Førre oppfølging — nettbrett og engelsk, alpha02 / 45

[Nettbrettrapporten](TABLET_POLISH_2026-09-09.md) dokumenterer den nye bibliotekframhevinga, større omslag/detaljplakatar, 54 fleire språkressursar og lokale skjermbilete med ekte kontoar. Desse resultata kjem etter alpha01-runden nedanfor. Fysisk nettbrett og full engelsk omsetjing er framleis ikkje godkjende.

## Førre implementeringsrunde — 9. september, alpha01 / 44

Resultata for alpha01 er samla i [nattrapporten](OVERNIGHT_REPORT_2026-09-09.md). Tabellane og sorteringa nedanfor bevarer den første baseline-kontrollen; dei skal ikkje lesast som siste teststatus.

- Språkval, engelsk førehandsvising, ressurskontroll og migrering er implementerte. Full engelsk kjerneflyt er enno ikkje godkjend.
- Nettbrettgrunnlaget har breiare mediesider, sentrerte panel og eigne Android-testar for vindaugsskifte, sein metadata, tastatur og stor skrift.
- **BUILD-01/02/03:** 269/269 einingstestar bestod i ny køyring; lint 0 feil / 28 åtvaringar; full Android-runde 138/138 bestod.
- **TABLET-01, deldekning:** 46/46 ekstra testar bestod i emulatorvindauge 1600 × 1000 ved 160 dpi. Heim og sentrert detaljpanel er visuelt kontrollerte med syntetisk innhald. Full rotasjons-/multivindaugsflyt og fysisk nettbrett står att.
- **INSTALL-01/LANG-01, deldekning:** Signert alpha01 vart installert over 0.15.1 på review-emulatoren med bevarte ekte Jellyfin-/Emby-/Seerr-kontoar. Språkbyte til engelsk overlevde full prosessavslutting; nynorsk vart deretter gjenoppretta og kontoane var framleis tilkopla.
- Ny signert lokal førehandsversjon brukar eksisterande produksjonsnøkkel. Ingen GitHub-/Play-publisering i denne runden.
- Fysisk telefonlangtest, fysisk nettbrett, TV og ekte tokonto-/førespurnad-til-varsel-test står framleis opne.

## Reglar for avkryssing

- **Bestått:** eit konkret scenario har eit observert resultat og ei identifisert testkjelde.
- **Brukartesta:** brukaren har opplyst at testing er utført; omfang og resultat må ikkje utvidast utover opplysningane.
- **Tidlegare bestått:** dokumentert i ein tidlegare verifiseringsrapport, ikkje køyrt på nytt denne runden.
- **Ikkje prøvd / ikkje stadfesta:** skal stå ope. Ein emulator erstattar ikkje fysisk måling av yting, kodekar eller fjernkontroll.

## Einingar og testgrunnlag

| Eining | Kjelde og status | Kva manglar |
| --- | --- | --- |
| Pixel 9 Pro XL | Brukaren stadfestar fysisk mobiltest 8. september 2026. | App-/Android-/tenarversjon, scenario, resultat og varigheit. |
| Truleg OnePlus, modell ukjend | Brukaren stadfestar andre fysisk mobiltest; merket er usikkert. | Eksakt modell og dei same testdetaljane. Ytingsklasse er ikkje stadfesta. |
| Isolert Android-emulator, 5562 | Tidlegare full releasekontroll: 127/127 på 0.15.1. | Ny køyring etter relevante kodeendringar. |
| Review-emulator, 5560 | Tidlegare releasekontroll av oppdatering og ekte Jellyfin-/Emby-/Seerr-kontoar. | Ny manuell kontroll ved vidare endringar; ingen instrumentering her. |
| Nettbrettemulator | Ikkje prøvd i denne runden. | Dedikert profil og vindaugs-/rotasjonstestar. |
| Fysisk nettbrett | Ikkje stadfesta. | Modell, Android-versjon og full kjerneflyt. |
| Android TV-/Google TV-emulator | Ikkje prøvd; TV-grensesnitt er planlagt. | TV-startpunkt, fjernkontroll og eigne testscenario. |
| Fysisk Android TV / Google TV | Ikkje stadfesta. | Eining, OS, fjernkontroll og framtidig TV-bygg. |

Android-/tenarversjon kan registrerast utan tenaradresse, brukarnamn, passord eller tilgangsteikn. Private bilete og rå nettverksloggar høyrer ikkje heime i Git.

## Testscenario

| ID | Scenario og godkjenningskrav | Status / bevis |
| --- | --- | --- |
| BUILD-01 | Einingstestane køyrer utan feil eller hoppa-over-testar. | **Bestått på nytt:** 253 testar, 26 testklassar, 0 feil, 0 hoppa over. |
| BUILD-02 | Lint avsluttar utan feil; åtvaringar blir vurderte. | **Bestått med åtvaringar:** 0 feil, 28 åtvaringar. Uendra analyse gjenbrukt av Gradle; sortering nedanfor er ny. |
| BUILD-03 | Full Android-testpakke. | **Tidlegare bestått:** 127/127, [releasekontroll](VERIFICATION_v0.15.1.md). Ikkje køyrt på nytt no. |
| INSTALL-01 | Signert 0.15.1 over 0.15.0 bevarer kontoar og innstillingar. | **Tidlegare bestått på review-emulator:** sjå releasekontrollen. Ikkje ein attest på oppdatering via Play. |
| PHONE-01 | Appen er prøvd på to fysiske mobiltelefonar. | **Brukartesta:** Pixel 9 Pro XL og truleg OnePlus. Konkrete resultat er ikkje oppgjevne. |
| AUTH-01 | Vanleg Seerr-brukar ser berre eigne økter og førespurnader. Administrator får berre tillatne fellesvisingar. | Automatiske tilgangstestar er med i baseline; **ekte tokonto-scenario ikkje stadfesta**. |
| AUTH-02 | Utgått/tilbakekalla tilgang og kontobyte fjernar førre kontoen sitt innhald, utan feilaktig identitet ved sending. | Automatisk deldekning; **ekte tenarprøve ikkje stadfesta**. |
| PLAYER-01 | 60 min samanhengande video på begge telefonane utan krasj eller fastlåst spelar. | **Ikkje stadfesta.** |
| PLAYER-02 | Direkteavspeling og omkoding; pause, spoling, OSD, Tilbake, rotasjon, lyd og tekst. | **Tidlegare automatiske testar bestod**; konkret fysisk gjennomføring ikkje stadfesta. |
| PLAYER-03 | Nettbrot og nettbyte gir forståeleg feil/gjenopptaking; ingen uventa start etter bakgrunn eller kontobyte. | Automatisk deldekning; **fysisk nettbyte og prosessavslutting ikkje stadfesta**. |
| REQUEST-01 | Godkjend ekte testførespurnad følgjer rett sesong og brukar heilt til bibliotek og varsel. | **Ikkje stadfesta.** Krev eigargodkjend testinnhald før handlingar som kan starte nedlasting. |
| REQUEST-02 | Delvis tilgjengeleg/returnerande serie kan følgjast utan duplikatførespurnad. | Automatiske testar finst; **ekte gjennomgåande scenario ikkje stadfesta**. |
| FEED-01 | Digital utgjeving innanfor 28 dagar + faktisk bibliotekkopi; gammal nyimportert film blir ikkje ny utgjeving. | Automatiske testar finst; **full ekte katalogkontroll ikkje stadfesta**. |
| FEED-02 | Radarr/Sonarr-kalender viser heimeutgjevingar/episodar, ikkje kinodato; tenarsvikt blir ikkje forveksla med tom kalender. | Automatiske testar finst; **ny ekte kalenderkontroll ikkje utført**. |
| MOTION-01 | Minst 20 første/gjentekne popup-opningar med sein metadata, utan ytre høgdehopp eller flytta lukkeknapp. | Tidlegare automatiske popup-testar bestod; **ny fysisk måling ikkje utført**. |
| TABLET-01 | Heim, søk, detaljar og innstillingar i ståande/liggjande og delt vindauge bevarer tilstand og lesbarheit. | **Ikkje prøvd som eiga nettbrettmatrise.** |
| TABLET-02 | Spelar, tastatur, mus og skriftstorleik 2.0 på fysisk nettbrett. | **Ikkje stadfesta.** |
| TV-01 | Innlogging → søk → detaljar → førespurnad/avspeling fungerer med berre fjernkontroll. | **Ikkje implementert/testa som TV-flyt.** |
| TV-02 | Fokus blir bevart; ingen fokusfeller; Tilbake, play/pause og spoling fungerer på fysisk TV-eining. | **Ikkje prøvd.** |
| LANG-01 | Heile kjerneflyten på nynorsk og engelsk, med lagra språkval og lange tekstar. | **Planlagt.** Full engelsk språkstøtte er ikkje levert enno. |

## Første utførte arbeidsrunde

- [x] Les releasebevisa og skil eksisterande testresultat frå gjenståande faktisk bruk.
- [x] Opprett denne testmatrisa (QA-01).
- [x] Registrer brukaren sine to mobiltestar utan å anta scenario eller resultat.
- [x] Køyr einingstestar på nytt med gjenbruk av testresultat deaktivert: 253/253 bestod.
- [x] Køyr lint-oppgåva og vurder dei 28 åtvaringane etter kategori.
- [x] Kartlegg eksisterande nettbrettgrunnlag og manglande TV-startpunkt i kjelda.
- [ ] Utfør ekte AUTH-01/AUTH-02 med godkjende testkontoar og dokumenterte roller.
- [ ] Legg til og køyr særskilde nettbrett-/TV-testar når den aktuelle implementasjonen er på plass.

### Reproduserbar lokal kontroll

Den første testkommandoen henta resultat frå byggmellomlageret. Deretter vart einingstestresultata rydda gjennom Gradle og testane køyrde på nytt med `--no-build-cache`; den endelege `:app:testDebugUnitTest` vart faktisk utført.

```powershell
$env:TEMP = 'C:/JellyBin/.gradle-tmp'
$env:TMP = 'C:/JellyBin/.gradle-tmp'
./gradlew.bat --gradle-user-home C:/JellyBin/.gradle-home cleanTestDebugUnitTest testDebugUnitTest --no-build-cache --console=plain
./gradlew.bat --gradle-user-home C:/JellyBin/.gradle-home lintDebug --console=plain
```

Testtal er summerte frå `app/build/test-results/testDebugUnitTest/TEST-*.xml`. Lint er lese frå `app/build/reports/lint-results-debug.xml`. Rapportane er lokale byggfiler. Ingen emulator var tilkopla då denne runden sjekka ADB; inga ny ekte konto- eller instrumenteringsprøve er utført her, og ingen kontoar er endra.

## Kodekontroll — første sortering

Dette er ei oppfølgingsliste, ikkje ein tryggleiksgaranti eller godkjenning av alle åtvaringane. Ingen nye lint-undertrykkingar eller avhengnadsoppgraderingar er gjorde.

| Kategori | Tal | Prioritet og neste steg |
| --- | --- | --- |
| `OldTargetApi` | 1 | P1 før Play: vurder gjeldande mål-API og åtferdsendringar. Ikkje hevde at ei «nyare API finst»-åtvaring åleine betyr at appen blir avvist. |
| `GradleDependency` / `NewerVersionAvailable` | 13 | Planlagd kompatibilitetsrunde. Vurder endringsloggar og støttekrav; Media3-endringar krev ny spelarregresjon. Oppgrader ikkje alt berre for å få null åtvaringar. |
| `ModifierParameter` | 5 | P2 API-opprydding i HomeScreen, SecondaryScreens og LoadingSkeletons. Kontroller kallstader før namn/rekkjefølgje blir endra. |
| `UnusedAttribute` | 1 | P2 widget-førehandvising: `previewLayout` gjeld nyare Android. Test eldre launcher-fallback og vurder eiga førehandvising utan å fjerne nyttig nyare støtte. |
| `ObsoleteSdkInt` | 1 | P2 ressursopprydding: overflødig v26-kvalifikator når minimum er 26. Ikkje flytt ikonressursar utan å samanlikne vanleg og tematisk ikon. |
| `UseCompoundDrawables` | 1 | P3 mogleg forenkling av widget-header; vurder ikonstorleik og layout før endring. |
| `UseKtx` | 6 | P3 stilforenkling. Behald synkron/asynkron lagringsåtferd; ein automatisk omskriving av `commit()` må ikkje bli `apply()`. |
| **Totalt** | **28** | Ingen lint-feil; funksjonelle risikoar blir vurderte gjennom testscenarioa over. |

## Nettbrett og TV: kartlagd grunnlag

- `ReelLayout` brukar 640 dp som grense for sidenavigasjon og 840 dp som maksimal innhaldsbreidd. `ReelstackApp` vurderer vindaugsbreidda, ikkje berre einingsmodellen. Dette gjer nettbrettarbeidet til vidareutvikling og testing, ikkje ein ny app frå null.
- Det gjeldande manifestet har vanleg mobilstartpunkt, men ikkje TV-startpunkt eller TV-banner. Det er heller ikkje erklært ei full TV-flyt utan krav om berøringsskjerm. Mobilinstallasjon på ei TV-eining ville ikkje dokumentert TV-støtte.
- Nettbrett er krav før 1.0. TV får eiga planlagd leveranse mot 1.1, med felles tenestelogikk og ei fjernkontrolltilpassa presentasjonsflate. Ingen TV-kompatibilitet blir annonsert før eigne testar er bestått.
