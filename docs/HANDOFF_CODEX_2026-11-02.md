# Overlevering til Claude · 2. november 2026 kl. 00:50

Arbeidsmappe: `C:/JellyBin/spole-sesong`, grein `oppdag-samla`. Utgangspunktet var
Claude sin reine commit `fd03154` (rørslespråk), oppå offentleg beta30. Brukaren bad
Codex arbeide vidare fram til kl. 00:50 og levere tilbake til den eksisterande Claude-økta.

Kodecommit-ar: `c1e8f83` (hopp-knappen), `a597f75` (toppmeny og radinngang) og
`123b4c9` (rolegare søk, vidareført etter at brukaren bad om meir av Swiftly-lista).

## Vidareført

- **Swiftly punkt 2, delvis:** gamle treff blir ståande under ny skriving og nettlast,
  både i globalt søk og det gamle Oppdag-søket. Dei eigne bibliotektreffa blir publiserte
  straks dei kjem, utan å vente på Seerr. Ny søking nullstiller paginering og sperrar «fleire».
  Personar og plakatar blir heller ikkje erstatta av skjelett medan dei gamle treffa finst.
  Avbrot og kontroll av gjeldande søk hindrar at gamle svar overtek. **Historikk først ved
  inngang til resultata står att**; dagens historikklagring er ikkje endra.
- **Hopp-knappen på TV:** eiget fokusflagg held OK unna pause/spel-handteraren også når
  OSD er skjult. Fokusflagget blir rydda når segmentet forsvinn. Ein ny instrumenteringstest
  feila før rettinga (knappen mista fokus), og består etterpå med eitt hopp og ingen pause.
- **Heimradene:** `RowEntrance.kt` bruker 80 ms mellom radene, 220 ms toning og landing
  frå 14 dp / 96 %. Redusert rørsle er berre 100 ms toning. Berre teiknelaget flyttar seg.
  `StartupCover` gir startsignal etter at uttoninga er ferdig. Retur til Heim og ny økt gir
  nytt starttidspunkt; rader som blir komponerte etter sitt vindauge, er synlege straks.
  Ei rad som alt er på veg inn, startar ikkje på nytt om ei anna rad lastar inn over henne.
- **Toppmeny på TV:** `TvMenuStyle` og `tv_menu_style`, med migrering frå `hide_tv_sidebar`.
  «Meny» under utsjånad vel Sidemeny / Skjult sidemeny / Toppmeny. Heim, festa bibliotek og
  smarthyller (same innbyrdes rekkjefølgje som sidemenyen), Oppdag når synleg og Søk. Bibliotek
  er reserve når ingenting er festa. Profilsirkelen opnar den eksisterande profilmenyen.
  Fanene rullar vassrett ved plassmangel; profilen står fast. Fokus flyttar seg utan å opne
  fana; OK opnar. Opp/Ned knyter meny og innhald saman. Ark gøymer og deaktiverer menyen.
  Globalt søk har eiga valt fane. Telefon og nettbrett held den eksisterande navigasjonen.
  Søkjefeltet handterer Opp uttrykkeleg, sidan tekstfeltet elles et piltrykket. Når søk ligg
  over sida, får ikkje innhaldet under søk fokus eller tilgjengesemantikk.

`LocalTopBarCarriesAccount` hindrar doble profil-/søkjeknappar på Heim og Oppdag.
Biblioteket sitt tenesteval er bevart: dette er eit kjeldeval, ikkje ein ekstra profilknapp.
Alle nye tekstar finst på nynorsk, bokmål og engelsk.
Menyhøgda blir målt, og innhaldet får tilsvarande innrykk ovanfrå også ved stor tekst.
`RowEntranceTest` og `TvTopBarUiTest` er instrumentering med Compose-testklokke/fjernkontroll,
medan migreringa blir testa i Robolectric. Skjermbileta er testdata, ikkje ekte tenarkontoar.

## Attstår frå den godkjende planen

1. **Profilflukta** i `RORSLE_OG_TOPPMENY_2026-11-01.md` er ikkje implementert. Heimradene har
   startsignalet, men inga halding til landing eller avatarflukt. Ver merksam på at
   `ProfileCoordinator.switchProfileGuarded` endrar `ConnectionRepository.activeProfileId`
   **før** han kallar `switchProfileNow`; å forseinke berre callbacken gir ikkje eit forseinka
   kontobyte. Bevar PIN- og øktvernet. `MainActivity` nøkklar dessutan heile vaksen-/barneskalet
   på økta, så fluktlaget må leve utanfor det som blir kasta ved bytet.
   Start lesinga i `ProfileCoordinator.kt:48`, `ReelstackViewModel.kt:216` og
   `MainActivity.kt:96`. `synchronizeSession()` stengjer dei gamle koordinatorane og tømer
   førre profil sine medielister; hald denne grensa intakt. Ny profil bør få sitt eige
   starttidspunkt etter landing, også når rørsle er redusert eller byte kjem via Stova.
2. Punkt 1–7 og kosmetikken i `SWIFTLY_LAN_2026-11-01.md`: punkt 2 er delvis ferdig som over,
   og den særskilde hopp-feilen i punkt 4 er retta. Resten står att.
3. Beta31/168 etter at resten er ferdig og verifisert. Versjonen er framleis beta30/167.
   Ingen push, tag, publisering eller installasjon på ekte kontoar i denne runden.

## Verifikasjon

- Tillegget for rolegare søk: **24/24 målretta JVM-testar bestod**, inkludert fire nye
  koordinatortestar og ein ny UI-test. Dekker gamle kort under lasting, bibliotek før sein
  Seerr, feil i Seerr, sletting av søket og eit seint svar som ignorerer avbrot.
  Logg: `app/build/codex-calmer-search-tests.log`.
- Samla JVM-runde etter søkjetillegget: **1 389 totalt, 1 388 bestått, 0 feil, 1 hoppa over**.
  `lintDebug`: **0 feil**, 215 åtvaringar og 1 hint. Debug, Android-test-APK og signert
  `assembleRelease` bestod samla på 5 min 31 s. Logg: `app/build/codex-calmer-search-final-validation.log`.
- Ny TV-kontroll etter søkjetillegget: **7/7 bestått**, `TvTopBarUiTest` og
  `TvNavigationIntegrationTest`. Logg: `app/build/codex-calmer-search-tv.log`.
- Endeleg kontroll-APK og mapping: `app/build/codex-verification-123b4c9/`, 28 102 512 byte,
  SHA-256 `e6c434fe55194525466e141a19d067e20a5ddb73885d6c8f6c736788eb3c8187`.
  Same faste eigarsignatur, `app.reelstack`, beta30/167, ikkje debuggable. Native-/lisenskontroll
  bestod igjen. Dette er berre lokal verifikasjon, ikkje ein ny distribuert beta30.
- Tala og release-artefaktet nedanfor gjeld den ferdige TV-/rørsleblokka før søkjetillegget.
- Endeleg `testDebugUnitTest`: **1 384 totalt, 1 383 bestått, 0 feil, 1 hoppa over**.
- `lintDebug`: **0 feil**, 215 åtvaringar og 1 hint.
- `assembleDebug` og `assembleDebugAndroidTest`: bestått.
- Omsetjingskontroll: 1 686 nøklar, berre den eine tillatne standardnøkkelen utan omsetjing.
- Første breie Android-runde: 96/97. Den einaste feilen, Opp frå søkjefeltet, vart retta;
  ny målretta navigasjonsrunde bestod 3/3.
- **Endeleg Android-runde: 97/97 bestått**, 241,875 sekund. Klassane var `TvTopBarUiTest`,
  `TvNavigationIntegrationTest`, `RowEntranceTest`, `StartupRevealTest`, `JellyfinPlayerUiTest`,
  `HomeMediaRowsTest`, `HomeVisualPolishTest`, `TvRefinementUiTest`, `TvSettingsRedesignTest`
  og `AccountPanelsTest`. Logg: `app/build/codex-final-fixed-instrumentation.log`.
- Release-koden kompilerte og R8/lintVital bestod. Første pakking stoppa fordi arbeidsmappa
  mangla `signing.properties`. Ei ignorert lokal fil peikar no til den eksisterande nøkkelen
  i hovudprosjektet; ingen ny nøkkel er laga. **Ny `assembleRelease` bestod** på 1 min 6 s.
  Logg: `app/build/codex-release-signed-build.log`. APK-signaturen er den faste
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK-en er `app.reelstack`, beta30/167, ikkje debuggable, 28 102 512 byte.
  SHA-256: `ecbcffe0bb610e39f56ccdb6ea29258d30091453b6a5dcc1e2bf675e9d37e243`.
  `check-native-subtitles.py` bestod: fire ABI-ar, 16 KB-justering for 64-bit og åtte lisensnotis.
  APK og tilhøyrande R8-mapping er arkiverte i `app/build/codex-verification-a597f75/`.
  Dette er berre eit lokalt kontrollartefakt; det er ikkje distribuert og må ikkje publiserast
  som beta30. Bygg ny beta31/168 når resten av oppdraget er ferdig.

Bygglogg: `app/build/codex-final-fixed-validation.log`. Test-APK med mørk bakgrunn i den
isolerte stor-tekst-fixturen: `app/build/codex-final-test-apk.log`.

Dei innlogga AVD-ane 5560/5564 og fysiske TV-ar er ikkje endra. Ingen nye AVD-ar er laga,
og ingen brukardata er sletta. Instrumenteringa køyrer berre i den eksisterande testprofilen.
Skjermbilete frå syntetiske testdata og byggloggar ligg lokalt under `app/build/`, ikkje i Git.
`review-top-menu-home.png`, `review-top-menu-search.png` og `review-top-menu-large-text.png`
er kontrollerte visuelt (inkludert vald/fokusert søkjefane og dobbel tekststorleik).
TV-testane brukar den eksisterande `Spole_TV_Instrumentation` på 5566, med pakken
`app.reelstack.debug`. Mobilinstrumentering og fysisk TV er ikkje køyrde i denne runden.

Bygg frå denne arbeidsmappa med PowerShell og Java 21:

```powershell
$env:TEMP='C:\JellyBin\.gradle-tmp'
$env:TMP=$env:TEMP
.\gradlew.bat --gradle-user-home C:\JellyBin\.gradle-home --max-workers=2 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease
```

WSL-ADB ligg i `/home/oyvhov/Android/Sdk/platform-tools/adb`; instrumenteringsrunneren er
`app.reelstack.debug.test/app.reelstack.SpoleTestRunner`. Bruk aldri denne på dei innlogga
5560/5564. Dei aktuelle klassane står i den endelege instrumenteringsloggen.
