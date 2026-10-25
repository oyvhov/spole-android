# Spole stabil 1.0

Spole skal bli ein stabil offentleg Android-app med dagens mørke design og ferdig kjerne i
barnemodus. Beta5 rettar minnebruk, bilettoken og profilgrenser før større
funksjonsutvidingar. Kontrollen legg vekt på bygg, relevante regresjonstestar og vanleg bruk på mobil/TV.

## Samla avspelaroppgradering · 17. oktober 2026

Fjerde lokale runde samlar avanserte val bak éin OSD-inngang og legg til motorstyrt fart
og forsinking, mobilrørsler, ASS/SSA-rendering, SUP-sidefiler og Jellyfin-trickplay.
Fråkopla nedlastingar med lagra tenargrunnlag brukar no den varige framdriftskøa.
[Arbeidslista og verifiseringa](PLAYER_UPGRADE_2026-10-17.md) skil implementert funksjon
frå format og fysisk maskinvare som framleis treng prøving.

Dei eldre bolkane nedanfor dokumenterer kvar si runde. Omtalen av manglande fråkopla
opplasting og native ASS-rendering er historisk etter denne oppgraderinga.

## Målte Compose-rettingar · 17. oktober 2026

Tredje lokale oppgraderingsrunde i `spole-sesong`, framleis utan ny release.

- Fokusløftet returnerer no `State<Float>` frå `animateFocusScale`. Verdien blir lesen i
  `graphicsLayer`, der skaleringa faktisk blir brukt. Dei sju brukarstadene er oppdaterte:
  TV-spelarknappar, Oppdag-plakatar, «sjå alle», hald-fram-kort, bibliotekskort og dei to
  smarte hylleflatene. Ei fokusendring oppdaterer framleis innhaldet; kvart animasjonssteg
  treng ikkje lenger å komponere heile kortet/knappen. Varigheit, skalering og trykkprioritet
  er bevarte. Ingen ny stabilitetsannotasjon eller cache skjuler tilstandsendringar.
- Mobilheroen tek rulleposisjonen gjennom ein funksjon som blir kalla i biletlaget.
  Heim treng ikkje lenger å lese posisjonen under komposisjon av hero-elementet.
  Redusert rørsle set no parallax til null; når rørsle blir slått på att, blir den nyaste
  rulleposisjonen brukt. Handlingar og tekst flyttar seg ikkje saman med bakgrunnsbiletet.

| Kontrollert overgang | Før | Etter |
| --- | --- | --- |
| Eitt fokusløft, 12 observerte animasjonsrammer | 10 komposisjonar i verten til den felles fokusfunksjonen | 1 komposisjon; animasjonen når framleis 1,035 og målestorleiken er uendra |
| 12 rulleposisjonar i mobilheroen, 0–96 px | 12 komposisjonar i hero-verten | 0 komposisjonar; biletlaget flyttar seg 96 px |
| Redusert rørsle etter ei rulleendring | Parallax held fram | Biletlaget går til null og blir der gjennom nye rulleendringar |

Målingane brukar Robolectric API 34 med native teikning, kontrollert Compose-klokke og lokal
biletfikstur. Dette er tal på komposisjonar ved dei nemnde grensene, ikkje prosentvis betre
FPS, RAM eller oppstartstid på fysisk TV. Fokusprøva kontrollerer òg trykk, avbrot og redusert
rørsle. Heroprøva samanliknar faktisk teikna bilete, mål og handlingsposisjon, og lagrar bilete i
`app/build/outputs/motion-review/`. Utgangsmålingane ligg lokalt i `app/build/focus-baseline.xml`
og `app/build/parallax-baseline.xml`; dei feila på dei tilsikta regresjonskrava før retting.

### Kontroll av Compose-rettingane

- **1170 av 1171 einingstestar består**, éin medvite hoppa over (`realNetworkWhenAskedFor`).
  Fire nye prøver dekkjer fokusløft, trykk/redusert rørsle, parallax og endra rørsleval.
- **20/20 Android-prøver består på endeleg APK**, API 36, eksisterande test-TV 5566:
  `FocusOutlineTest` (3), `TvPlaybackControlsTest` (14), `TvNavigationIntegrationTest` (2)
  og `TvLibraryRegressionTest` (1). Dei dekkjer D-pad, fokusretur, spoling, pause/buffering,
  synlege fokusrammer, navigasjon og dobbel skriftstorleik.
- `assembleDebug` og `assembleDebugAndroidTest` er ferdige. **Lint: 0 feil, 204 åtvaringar og
  eitt tips**, same tal som før denne runden. Ingen lint-funn i dei nye måleprøvene,
  `FocusOutline.kt` eller `MobileLibraryFeature.kt`. Samla Gradle-køyring gav `BUILD SUCCESSFUL`.
- Bileta `app/build/outputs/motion-review/hero-before.png`, `hero-scrolled.png` og
  `hero-reduced-motion.png` er laga og visuelt kontrollerte. Det siste er byteidentisk med
  det første; rullebiletet viser flytta bakgrunn og uendra handling/tekst. Dette er syntetisk
  innhald med engelsk testlokale. Mobil er prøvd i Robolectric, ikkje på fysisk telefon.
- Språkressursar, versjonskonsistens, workflow-YAML og `git diff --check` består. CI tek no
  med `FocusOutlineTest` i TV-utvalet og lagrar dei syntetiske rørslebileta som testartefaktar.
  Den endra workflowen er ikkje køyrd på GitHub enno.

Debug-APK: **30 937 585 byte**, SHA-256
`4e30b2d19bddd6633e64cf784707750d4cd091f2be32622c145731d5a438a9d6`.
Loggar: `app/build/motion-final-gradle.txt` og `app/build/motion-final-tv.txt`.
Test-TV-en er avslutta normalt med data bevarte. Review-profilar og produksjonspakke er
ikkje endra. Ingen versjonsauke, commit eller publisering.

## Varig framdriftskø · 17. oktober 2026

Andre lokale oppgraderingsrunde på `oppdag-samla` i `spole-sesong`. Ingen ny release.

- Nettspelaren skriv no eit sjekkpunkt på disk før rapportering. Køa held siste posisjon per
  profil, tenar, brukar og innlogging; ho lagrar ikkje token, tenaradresse eller avspelingsøkt.
  Ho er avgrensa til 64 titlar totalt og 30 dagar. Metadata-cache og utkø er separate, og
  utkøa er unnateken backup/overføring til ei anna eining.
- Ein WorkManager-jobb prøver att ved oppstart, etter lukking av spelaren og via den eksisterande
  bakgrunnsoppdateringa. Nettverk er påkravd, Wi-Fi-valet blir respektert og mellombelse feil får
  eksponentiell ventetid frå 30 sekund. Aktive nett- og fråkopla spelarar held gjentakinga tilbake.
- Køa brukar brukarframdrift, ikkje gamle `Sessions/Playing/Stopped`-kall. Før skriving blir
  posisjon, sett-status, teljar og sist-spelt-verdi samanlikna med sist kjende tenarsvar.
  Ei endring får prioritet, også tilbakespoling eller «ikkje sett». Eit tapt HTTP-svar blir
  handtert ved å lese att; ein allereie lagra posisjon blir ikkje skriven på nytt.
- Skriving blir stadfesta med eit nytt tenarsvar. Ein sein kvittering kan berre fjerne sitt
  eige sjekkpunkt. Konto-/profilgenerasjonen blir kontrollert før skriving og før lokale resultat.
  «Fjern frå hald fram», sett-status og tømming av lokal framdrift ugyldiggjer køa òg.
- Rapporteringa er skild ut frå spelarens skjermtilstand. Ventande framdriftsrapportar blir
  slått saman; minnekøa har ei grense på 32 hendingar. Den normale rapporteringa les no
  kanonisk tilstand etter vellukka rapport, slik at neste nettbrot har eit samanlikningsgrunnlag.

Jellyfin 10.11 si [ItemsController](https://github.com/jellyfin/jellyfin/blob/v10.11.0/Jellyfin.Api/Controllers/ItemsController.cs)
stadfestar `POST UserItems/{itemId}/UserData`; den nullable oppdateringsmodellen lèt oss bevare
favorittar og teljarar. Emby sitt [eige API-svar](https://emby.media/community/topic/110432-how-to-set-the-playbackpositionticks-field-for-an-item/)
stadfestar `POST Users/{userId}/Items/{itemId}/UserData`. Emby-payloaden bevarer felta frå ei fersk
lesing. Manglande eller avviste skriveendepunkt blokkerer automatisk gjentaking av sjekkpunktet;
den lokale framdrifta blir behalden. Eit nytt avspelingsforsøk kan erstatte sjekkpunktet.

Avgrensingar: desse API-a tilbyr ikkje atomisk samanlikning-og-skriving. Ei anna eining kan
framleis endre tilstanden mellom GET og POST; dette er konfliktvern, ikkje ein garanti om
global rekkjefølgje. Eit faktisk slutt-signal set «sett» ved gjentaking, ikkje ein gjetta
tenarprosent. Berre sjekkpunkt som er skrivne på disk overlever brå prosessdød; siste hending
som enno ventar i minnet kan gå tapt. Ny innlogging med anna token arvar ikkje ei gammal kø.
Automatisk gjentaking krev lagra brukar-ID og eit gyldig samanlikningsgrunnlag frå tenaren.
Fråkopla avspeling får ikkje automatisk serveropplasting i denne runden, sidan nedlastingane
enno manglar eit lagra samanlikningsgrunnlag. Den eksisterande lokale visingsjournalen har
framleis 24 timars levetid. Ekte Jellyfin-/Emby-tenarar er ikkje brukte i dei syntetiske testane.

### Kontroll av framdriftskøa

- Nye einingstestar dekkjer samanslåing, køgrenser, varig lagring, kontoskilje,
  tilbakespoling på ei anna eining, endra sett-status/teljar/dato, tapt POST-svar,
  avvist tilgang, manglande endepunkt og nye lokale sjekkpunkt under ei sein lesing.
- TV-prøva brukar ekte Media3-avspeling og to syntetiske HTTP-tenarar. Rapportering feilar
  medan filmen spelar. Etter lukking må WorkManager skrive siste posisjon gjennom UserData,
  bevare Emby-favoritten og tømme sjekkpunktet etter kanonisk lesing.
- `scripts/test-progress-restart.sh emulator-5566` køyrer same prøve i to separate
  instrumenteringsprosessar, med `force-stop` mellom. Ho kontrollerer ulik prosess-ID,
  lagring gjennom nettbrot og eintydig stadfesting i den nye prosessen. Scriptet nektar
  å bruke review-seriane 5560 og 5564. PR-matrisa har no òg denne prøva.
- Fråkoplaprøva vart hoppa over som tilsikta på TV. På den eksisterande testtelefonen
  `Spole_Instrumentation`/5562 stoppa ho med `STORAGE_LIMIT`: `/data` hadde 280 312 KiB
  ledig, under den eksisterande 512 MiB-reserven. Dette er ikkje ei bestått fråkoplaprøve.
  Ingen lagra brukar-/AVD-data vart sletta for å kome rundt plassgrensa.

| Kontroll | Resultat |
| --- | --- |
| Heile JVM-suiten | **1166 beståtte av 1167**, éin medvite hoppa over (`realNetworkWhenAskedFor`), null feil. 21 nye einingstestar for denne runden. |
| TV, endeleg APK | **15/15 beståtte**: lokal journal, kontobytte, pause/bakgrunn, nettfeil, gjentaking, PGS og HLS. |
| Mobil, endeleg APK | **1/1 bestått**: gjentaking etter lukking mot både Jellyfin- og Emby-fixture. Same prøva er med i TV-talet. |
| Prosessrestart, endeleg APK | **Begge fasar beståtte på både TV og mobil**, med stadfesta nye prosess-ID-ar. Éi ekstra unik prøve; totalt **16 ulike Android-prøver beståtte** i denne runden. |
| Fråkopla nedlasting/avspeling | **Ikkje bestått** på telefonen grunna lagringsgrensa omtalt over; medvite hoppa over på TV. |
| Bygg | `assembleDebug` og `assembleDebugAndroidTest` fullførte. |
| Lint | **0 feil**, 204 åtvaringar og eitt tips. Ingen nye lint-funn i framdriftskøa. Endeleg samla Gradle-køyring gav `BUILD SUCCESSFUL`. |
| Andre kontrollar | Språkressursar, versjonskonsistens, workflow-YAML, bash-syntaks og `git diff --check` består. |

Endeleg debug-APK: **30 677 862 byte**, SHA-256
`8b497f8c9dd7b88934f97e4eb7cb21d55a94ce176e2a3b9538726774acc175c2`.
Loggar ligg lokalt i `app/build/pgs-fixture/progress-final-gradle.txt`, `progress-final-tv.txt`,
`progress-final-phone.txt`, `phone-offline-progress.txt` og `app/build/outputs/progress-restart/`.
Berre debug-/testpakkene på dei eksisterande test-AVD-ane 5562 og 5566 er oppdaterte.
Test-AVD-ane er avslutta normalt med data bevarte. Ingen versjonsauke, commit eller publisering.

## Kvalitetsløft etter beta17

Lokalt utviklingsarbeid 16. oktober 2026, på `oppdag-samla` etter `b33d1cb`. Dette er den første
avgrensa oppgraderinga etter den tekniske gjennomgangen, ikkje ein ny publisert versjon.

- **Stabile TV-handlingar:** heroen sluttar å rotere medan ei handling har fokus. Når fokus
  forlèt heroen, byrjar eit nytt åttesekundsintervall. Redusert rørsle stoppar automatisk
  rotasjon også på TV. Ved retur til Oppdag sluttar fokusgjenopprettinga etter første
  vellukka forsøk; ny tastenedtrykking avbryt eit forseinka forsøk.
- **Lokal PGS:** direkteavspelte MKV-filer kan bruke dei innebygde PGS-spora i Media3.
  Sporvalet matchar berre PGS mot PGS, kontrollerer tal og støtte, og melder utilgjengeleg
  undertekst ved eit tvitydig treff. PGS er på frå første sporval slik at ein tidleg
  undertekst ikkje blir forkasta. VTT ventar framleis på at sidefila er ferdig lasta.
  Eksterne PGS-filer, andre biletformat og HLS/reservevegar brukar framleis tenaromkoding.
- **TV i PR-kontrollen:** den eksisterande API 36-matrisa køyrer no både telefon og TV ved
  pull request. TV-utvalet inkluderer avspelarkontrollar, stor tekst, PGS og kontogrenser.
  Workflowen er endra lokalt; denne endringa har ikkje køyrt på GitHub enno.

### Kontroll av denne oppgraderinga

Kontrollert lokalt med eksisterande, isolerte TV-testprofil `Spole_TV_Instrumentation` på
`emulator-5566` og `app.reelstack.SpoleTestRunner`. Review-einingane med ekte kontoar er ikkje
brukte til instrumentering.

| Kontroll | Resultat |
| --- | --- |
| Heile JVM-suiten | 1146 testar: **1145 beståtte**, éin medvite hoppa over (`realNetworkWhenAskedFor`), null feil. |
| TV-navigasjon, avspelarkontrollar og profilgrense | **17/17 beståtte**, medrekna skriftstorleik 2.0. |
| Avspeling og undertekstar | **7/7 beståtte** på endeleg APK: lokal PGS, HLS/VTT-byte, undertekstar av ved start, feil/treg sidefil og to kontobyteprøver. PGS-prøva køyrer både Jellyfin- og Emby-fixture. |
| Android samla | **24 ulike testar beståtte** i målretta køyringar; ikkje heile Android-testpakken. Seks av spelarprøvene er køyrde om att etter siste spelarendring. |
| Bygg | `assembleDebug` og `assembleDebugAndroidTest` fullførte. |
| Lint | **0 feil**, 204 åtvaringar og eitt tips. Siste samla Gradle-køyring gav `BUILD SUCCESSFUL`. |
| Andre kontrollar | Språkressursar, versjonskonsistens, YAML-parsing og `git diff --check` består. |

Gradle vart køyrd med `TEMP`/`TMP=C:/JellyBin/.gradle-tmp`:

```powershell
./gradlew.bat --gradle-user-home C:/JellyBin/.gradle-home --max-workers=1 :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

Lokale Android-resultat: `app/build/pgs-fixture/tv-regression.txt` og `player-final.txt`.
Debug-APK: **30 020 769 byte**, SHA-256
`e0a8ab28bea1936af9387ff52f922396144caa1af368fd51e128e40741c9881a`.
Berre debug-pakka på test-TV-en er oppdatert. Versjonsnummeret er ikkje endra og ingen APK er publisert.

Den syntetiske `pgs.mkv` er generert med `scripts/build-pgs-test-fixture.py` frå eksisterande
testvideo og eigne bitmapdata; inga film-, font- eller undertekstfil er lasta ned.
Testen må vise den tidlege underteksten før fem sekund, slå han av og vise neste undertekst
etter spoling, utan ny PlaybackInfo-forhandling eller HLS-/VTT-kall.

Dette er avgrensa PGS-støtte. Ved spoling eller byte av spor kan Media3 vente til neste
PGS-displaysett dersom det aktive settet byrja før videonøkkelramma spelaren søkte til.
ASS/SSA med full stilgjengiving, UHD-stresstest, ekte tenarforhandling og HDMI-bitstream er
ikkje stadfesta av denne syntetiske prøva. Inga ny måling av oppstart, RAM eller rammetider
ligg til grunn for oppgraderinga; det blir ikkje hevda ein ytingsgevinst utan måling.

### Neste tekniske steg

1. **Framdrift frå nedlasta filmar:** nettspelaren har no utkøa omtalt over. Neste del er å
   lagre eit samanlikningsgrunnlag med nedlastinga og kople fråkopla avspeling til same flyt.
   Prøv òg ekte støtta serverversjonar, kontobytte og samtidig avspeling på fleire einingar.
2. **Mål svak TV-maskinvare:** fokusløft og mobilparallax har no kontrollerte før-/ettermålingar
   av komposisjon, omtalt øvst. Det står att repeterbare oppstarts-, navigasjons- og spoleprøver med
   rammetider, PSS/heap og same metadata-/mediesett. Bruk funna til å velje konkrete
   Compose- og cacheendringar, med eit dokumentert utgangspunkt før optimalisering.
3. **Avspelarkontrakt og undertekstmatrise:** skil planlegging, sporval og rapportering frå
   skjermtilstanden stegvis. Bygg formatprøver for fleire PGS-spor, ASS/SSA-stilar,
   fontvedlegg, tvungne spor og UHD før val av eventuell ekstra mediemotor.

## Endringar i utviklingsbygget

| Område | Implementasjon | Kontroll |
| --- | --- | --- |
| Katalog og kalender | Fire arbeidarar, kø på 16, projisert metadata i 8 MiB minnecache og kontoavgrensa Room-indeks. Diskindeksen er avgrensa til 32 MiB med seks timars levetid. Filmfrø held ikkje heile bibliotekdetaljar. | Test med 10 000 element og cachebudsjett består. Kort ekte TV-prøve ved 192 MiB utan nytt minnekrasj. |
| Bilettoken | Emby-adresser utan token, autentisering i header etter opphav/base-path-kontroll, Coil utan automatiske omdirigeringar. | Nettverks- og migreringstestar. |
| Oppgradering | Room 4 til 5 tømmer dashboard-cache og opprettar metadataindeksen. Gammalt Coil-namnerom blir fjerna dersom tilgjengeleg; nytt namnerom blir brukt. | Signert oppgradering på eksisterande TV: kontoar, nynorsk og ekte Jellyfin-/Emby-rader bevarte. Oppgradering med ferdige offlinefiler står att. |
| Profil og konto | Felles session med profil, fingeravtrykk og generasjon. Gamle jobbar og spelarar blir tilbakekalla; linked login skriv mot fanga profil. | Byte bort og tilbake, gamle aktivitetar og sein innlogging. |
| PIN | Vanleg deaktivering krev rett PIN; barneprofil kan ikkje deaktivere vernet eller erstatte ein eksisterande PIN gjennom oppsett. | Rett/feil PIN og byte under gjenoppretting. |
| Tidleg heimlasting | Personlege rader ventar på avklart Seerr-identitet. Verifisert vanleg mediekonto kan vise ordinære bibliotekmetadata medan kontrollen ventar. | Forseinka identitet, mismatch og separat Seerr-svikt. |
| Offline | Aktiv nettransport blir kontrollert. Cachen bevarer ferdige filmar, men nye data må halde seg under 32 GiB/80 prosent av volumet og etterlate minst 512 MiB ledig. | Nettbyte, plassmangel, pause og gjenopptaking. |
| Design og språk | Kompakt hero utan kunst, konsekvent arkverktøylinje, språkressursar for foreldreval og verdstema. | Mobil/TV, tre språk og skriftstorleik 2.0. |
| CI | Einingstestar, lint og bygg på branch-push/PR, utan eit ekstra tag-bygg. Korte smoketestar på éin telefon ved PR; utvalde regresjonstestar for telefon/TV ved manuell køyring før release. Jev er valfri rådgjeving berre på manuelt aktiverte køyringar. | Lokal kontroll og første GitHub-køyring består; ingen Jev-kall. |

## Testresultat

Kontrollen før endringane gav 881 beståtte einingstestar, éin hoppa over og null feil.
Lint gav null feil, 188 åtvaringar og eitt tips. Beta4 krasja på TV med minnemangel ved 192 MiB heap.
Sluttbygget 7. oktober 2026 køyrde 903 einingstestar: 902 beståtte, éin hoppa over og null feil.
Lint: null feil, 185 åtvaringar og eitt tips. Debug, test-APK og signert release vart bygde.
Android: 42 ulike mobiltestar og 16 ulike TV-testar er beståtte i dei utvalde pakkene.
Resultatet er samla frå fleire køyringar og målretta omprøver; det er ikkje ei køyring av heile
Android-testpakken. To ekstra oppstartstestar består òg.

| Flate | Testar | Resultat |
| --- | --- | --- |
| Mobil | `ReelstackSmokeTest`, `SheetInteractionTest`, `LoginExperienceTest`, `LinkedLoginTest`, `ProfileSessionBoundaryTest`, `RequestHistoryFlowTest`, `OfflineDownloadsUiTest`, `OfflinePlaybackIntegrationTest` | 42/42 beståtte etter omprøver |
| TV | `LoginExperienceTest`, `ProfileSessionBoundaryTest`, `WatchNextTest`, `TvNavigationIntegrationTest`, dei to spelarprøvene for kontobyte/byte bort og tilbake | 16/16 beståtte etter omprøver |
| Oppstart på mobil | Tilgjenge etter opning og maksimal ventetid ved tregt nett | 2/2 beståtte |

Feila vart undersøkte før omprøving. TV mangla faktisk ein innloggingsknapp ved endring av ei
lagra tilkopling; knappen og fokusflyten er retta. Mobil-AVD-en hadde ein System UI-heng og gamle
skjermoverstyringar. Smoke-fixturen set no sine eigne syntetiske konto-/radval før aktiviteten
startar, rullar til innhald på telefon og kontrollerer den personlege kalenderen frå beta3.
Spelarprøva kontrollerer både tilbakekalling, tømd media-kø og fjerna tittel-ID; standardtittelen
etter tømming er «Spole».

Den siste samla Gradle-køyringa gav `BUILD SUCCESSFUL` for:

```powershell
./gradlew.bat --gradle-user-home C:/JellyBin/.gradle-home --no-daemon --max-workers=2 testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
```

Test-APK-en vart bygd på nytt etter retting av testfixturane. Instrumentering brukte
`app.reelstack.debug.test/app.reelstack.SpoleTestRunner` berre på 5562 og 5566, med klassane
i tabellen og målretta omprøver. Språksjekken godkjenner 1539 standardnøklar og éin tillaten
standardnøkkel utan omsetjing. Versjonssjekk, YAML-parsing og `git diff --check` består.

### Signert lokal kandidat før beta5

Kandidaten er bygd frå arbeidsmappa etter `a9885bd`, med lokale endringar. Versjonen er framleis
`1.0.0-beta4` / kode `141`; dette er eit lokalt utviklingsbygg, ikkje ein ny publisert beta4.

| Felt | Verdi |
| --- | --- |
| Pakke | `app.reelstack` |
| APK-storleik | 11 564 130 byte |
| SHA-256 | `5ecedcafce23d8fe6754ec1a618b469594511c8c13e94f1549b1b6c17719725f` |
| Signatur SHA-256 | `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` |
| Android | minimum 26, target 36 |
| ABI | arm64-v8a, armeabi-v7a, x86, x86_64 |
| Lokal arkivering | `app/build/stable-verification/candidate/Spole-local-candidate.apk` og tilhøyrande `mapping.txt` |

APK-en vart installert med `-r` på eksisterande review-TV, utan ny innlogging eller sletting av
appdata. Ekte kunst, profilbilete og separate Jellyfin-/Emby-rader lasta inn. Ved 192 MiB heap
gjekk total PSS frå om lag 144 til 87 MiB gjennom den korte oppdaterings-/navigasjonsprøva.
Siste Dalvik-allokering var 13,3 MiB. Ingen ny `AndroidRuntime`-krasj vart funnen for den nye
prosessen. Dette er ikkje ein langvarig belastningsprøve. Mobilen med lagra review-data er
halden på førre APK.

Native-biblioteka er uendra. Dei fire ABI-ane har kontrollert `LOAD`-justering på 16 KiB, og
arkivet med FFmpeg-kjelder/relink-materiale finst. R8-mapping frå førre produksjonsbygg vart
teken vare på før bygging.

### Beta5

`1.0.0-beta5` / kode `142` er bygd og signert med den eksisterande nøkkelen. APK, mapping og
FFmpeg-kjelder er arkiverte saman. Signert oppgradering på review-TV bevarte kontoar og val.
Kort ekte Jellyfin-/Emby-avspeling viste 1080p AVC og EAC3 5.1 gjennom FFmpeg utan registrerte
lydavbrot eller droppa bilete i dei observerte prøvene. Offline-integrasjonsprøva består med
pause/gjenopptaking, avspeling utan tenar og profil-/kontobyte. Offentleg oppdatering frå beta4
gjennom appen består på review-mobilen. Den publiserte beta5 har kontrollert offentleg digest;
endelege resultat ligg i
[beta5-verifiseringa](VERIFICATION_v1.0.0-beta5.md).

### Prov for tryggleiksrettingane

| Eigenskap | Endra kode og kontroll |
| --- | --- |
| Bilettoken skal ikkje liggje i URL eller gå vidare ved omdirigering | `ServiceClients`, `ArtworkPolicy`, `MediaAuthHeaders` og image-loaderen. `ArtworkPolicyTest` brukar ekte lokale HTTP-tenarar og kontrollerer både same opphav utanfor base-path og framandt opphav. Migreringstesten tømmer gamle dashboard-rader og bevarer uvedkommande kontodata. |
| Socket-token skal berre sendast i header | `JellyfinSessionSocket`. `SessionSocketTest` kontrollerer handshake-header, URL utan token og tilbakefall til polling etter avvising. |
| Sein innlogging, følgjeval og gamle skjermar skal ikkje skrive/vise ein annan profil | `SessionScope`, `ConnectionRepository`, ViewModel, heimkoordinator og spelarar. `SessionScopeTest` dekkjer byte bort/tilbake, seine linked-login-svar og ei sperra kalenderlagring under profilbyte. `ProfileSessionBoundaryTest` og spelarprøvene dekkjer tilbakekalte skjermar på mobil/TV. |
| PIN kan berre fjernast etter rett kode eller stadfesta vaksenpassord | `ProfileViewModel` og PIN-flyten i hovud-ViewModel. Einingstestar dekkjer feil/rett kode, barneprofil og vern mot å erstatte eksisterande PIN. |

Ein fersk, uavhengig kodegjennomgang fann to regresjonar i kandidaten: utlogging kansellerte
arbeids-scopet før oppryddinga kunne starte, og kalenderlagring las aktiv profil for seint.
Begge er retta. Utlogging er prøvd med eit forseinka historiesvar i `RequestHistoryFlowTest`;
kalenderlagring er prøvd med profilbyte mellom kontroll og lagring. Ingen konkret attståande
regresjon vart funnen i den avgrensa gjennomgangen. Dette er ikkje ei påstand om at heile appen
er fri for tryggleiksfeil.

Instrumentering skal berre køyrast på dei eksisterande isolerte testprofilane. Kontoane og
brukardata på review-einingane 5560 og 5564 skal bevarast.

## Krav før stabil publisering

- Ingen kjende kritiske feil i dei endra flytane. Einingstestar, lint og utvalde mobil-/TV-regresjonstestar skal bestå, utan svekte testforventningar.
- Katalogtesten med 10 000 element skal halde minne-/arbeidarbudsjettet. Prøv oppdatering og navigasjon på TV med ekte bibliotek; dokumenter minnebruk og eventuelle krasj.
- Signert oppgradering frå beta4 skal bevare kontoar, profilar, innstillingar og offlinefiler.
- Kort manuell mobil-/TV-prøve: innlogging, profilbyte, navigasjon og avspeling. Kontroller fokus, tekststorleik 2.0 og tilgjenge der skjermar er endra.
- Prøv avspeling og undertekstar mot dei tilgjengelege Jellyfin-/Emby-tenarane. Dokumenter kva tenarversjonar som er prøvde; fixtures åleine stadfestar ikkje ei ekte tenarprøve.
- Kontroller signatur, APK-digest og mapping for kvart publisert bygg. Kontroller native-kjelder, ABI-ar og 16 KiB-sider når native-avhengigheiter blir endra.
- Publisering skjer først når brukaren ber om ein release. Nokre dagars vanleg bruk av kandidaten er tilrådd, utan ei fast ventetid som release-port.

## Arbeid etter 1.0-kjernen

Daglege skjermtidskvotar, casting, SyncPlay og nye medietypar kjem seinare. Leggetid er ei lokal
familierutine, og barnet sin tenarkonto eig bibliotek- og innhaldsrettane. PIN er ikkje kioskvern.
Vidare oppdeling av skjerm- og ViewModel-ansvar skal skje med regresjonstestar for kvar flyt.

Før stabil 1.0 står kort fysisk mobil-/TV-prøve og lengre prøving av undertekstar, nettbyte og
offlinebruk att. Passordgjenoppretting er gjennomgått i koden; ei prøve med eit ekte vaksenpassord
står att. CI er kontrollert lokalt, og den første GitHub-køyringa består.
Beta5 er publisert med høgare versjonskode og si eiga verifikasjonsfil, og er ei testutgåve.
