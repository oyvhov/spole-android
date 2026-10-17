# Avspelaroppgradering · 17. oktober 2026

Brukaren har bede om å ta heile restlista. OSD skal vere roleg og elegant: transport og
tidslinje ligg framme, medan spor, kapittel og avanserte val får éin samla inngang.

## Arbeidsliste

- [x] Fråkopla framdrift: varig kø, opphavleg tenarstatus, kontogrense, gjenopptaking og konflikthandtering.
- [x] Felles avspelingsval: fart, lydforsinking og tekstforsinking med reell effekt i motoren.
- [x] Mobilrørsler: volum, lysstyrke, dobbeltrykk og zoom utan konflikt med kontrollane.
- [x] Samla meny med trygg TV-fokusretur og lesbar skriftstorleik 2.0.
- [x] ASS/SSA med lokal stil- og skriftrendering, utan VTT-konvertering.
- [x] Breiare lokal bitmaptekst og dokumentert oppførsel etter spoling.
- [x] Førehandsbilete ved spoling utan å flytte tidslinje eller knappar.
- [x] Mindre spelaransvar: eigne kontraktar for innstillingar, spor, framdrift og UI.
- [x] Målretta regresjonstestar, visuell kontroll, full JVM-suite, Android-testar og lint.
- [ ] Oppstart, RAM og frame-time på svak fysisk TV, og format-/HDMI-prøver med mottakar.

Hakar blir sette etter verifisering, ikkje berre etter implementering. Tidlegare lokale
rettingar i arbeidsmappa blir bevarte. Ingen publisering er del av denne oppgåva.

## Implementasjonen

### Eitt roleg OSD

TV har tidslinje og éin inngang til «Avspelingsval». Mobil har transport og den same
inngangen. Lydspor, undertekstar, kapittel, fart, forsinking, kvalitet, biletfylling,
rotasjon, minispelar og statistikk ligg i menyen når funksjonen er tilgjengeleg.
Menysidene deler ein fast, rullbar visingsflate. Tilbake går eitt nivå opp; Lukk avsluttar
heile menyen. TV-fokus går tilbake til rada eller OSD-knappen brukaren kom frå.
Barnespelaren får ikkje dei avanserte kontrollane.

Separate Spel-/Pause-tastar les avspelingsønsket frå Media3 når tasten blir handtert.
Dei brukar ikkje ei gammal Compose-ramme til å avgjere om motoren skal veksle tilstand.
Dette hindrar at to raske Pause-trykk startar filmen att. Ei ny prøve held skjermtilstanden
medvite utdatert; ho feila før rettinga og består etterpå, saman med prøva på ekte video.
Loggar: `app/build/upgrade-remote-baseline.txt` og `app/build/upgrade-remote-fixed.txt`.

Fart er 0,5–2×. Lydforsinking er ±2 sekund, tekstforsinking ±10 sekund, i steg på
100 ms med nullstilling. Valet blir brukt i Media3-motoren og bevarer pause-/spelestatus.
Endring av forsinking forankrar klokkene på nytt på same posisjon. Verdiane høyrer til
avspelingsøkta og overlever skjermrotasjon gjennom modellen.

På mobil justerer vertikal rørsle på venstre side lysstyrken i spelarvindauget; høgre
side justerer medievolumet. Klyping byter mellom tilpassa og fylt bilete. Dobbeltrykk for
spoling er bevart. Rørsler som startar på kontrollar, høyrer til kontrollane; horisontal
rørsle endrar ikkje volum eller lysstyrke. Lysstyrken blir gjenoppretta når spelaren forlèt
vindauget. Avbroten rørsle fjernar òg den mellombelse tilbakemeldinga.

### Fråkopla framdrift og kontogrenser

Nye nedlastingar lagrar det opphavlege tenarsvaret og framdriftsskopet kryptert saman med
nedlastinga. Fråkopla avspeling skriv sjekkpunkt ved pause, spoling, bakgrunn, avslutting
og kvart tiande sekund under avspeling. Den varige utkøa gir gjenopptaking etter at
modellen er oppretta på nytt, og bruker det same konfliktvernet som nettspelaren.
Tenaren si endra framdrift får prioritet ved konflikt.

Modellen eig ein arbeidslås fram til ventande skriving er ferdig. Profil, tenar, brukar og
innlogging blir kontrollerte før skriving og opplasting. Byte bort og tilbake gjer den
gamle spelaren ugyldig. Gamle nedlastingar utan samanlikningsgrunnlag held fram med lokal
framdrift; dei blir ikkje automatisk lasta opp. Manglande brukar-ID eller nytt token gir
heller ikkje ein gammal utkø tilgang til ein ny konto.

### Undertekstar og førehandsbilete

ASS/SSA bruker originaldata og libass på ein eigen bakgrunnstråd. Både eksterne dokument
og MKV-spor kan behalde stilar, teikningar og animasjonar. Fontvedlegg blir henta innanfor
ei avgrensa tid og minnegrense; fråkopla MKV les vedlegg gjennom SeekHead. Systemfont er
reserve. [Native avhengigheiter, lisensar og grenser](NATIVE_SUBTITLES.md) er dokumenterte.

Innebygd PGS og DVD/VobSub bruker Media3. Ekstern PGS/SUP får ein eigen avgrensa parser
når tenaren annonserer ei gyldig filadresse på same tenar. Han kan hente ei allereie aktiv
PGS-rute etter spoling. Den siste runden legg òg til annonserte MKS-filer med PGS/DVD,
som held rett server-ID og språk ved lat lasting. Ein avgrensa privat filcache erstattar
full førehandslasting av bitmap-filer i RAM. Blanda innebygde spor og sidefiler blir
ikkje talde dobbelt ved språkval. Manglande gyldig filadresse bevarer tenarforhandlinga
om omkoding. Separate IDX/SUB-par blir ikkje lesne direkte; ein MKS levert av tenaren
kan dekkje dei. Innebygd PGS kan vente til neste display-sett etter spoling fordi Media3
søkjer til videonøkkelramma; dette er ikkje full Dune-paritet.

Jellyfin-trickplay blir etterspurt med `Fields=Trickplay` for tittelen som spelar. Hentinga
er valfri og startar etter at avspelinga er klargjord. Sprite-ruter blir viste over
tidslinja utan å endre layout. Førespurd avkodingsstorleik er avgrensa til 1,5 millionar
pikslar per sprite; dette er ikkje ei måling av dekodaren sitt mellombelse minnebruk.
Kapittelbilete er reserve når dei finst, mellom anna for Emby. Manglande eller feilande
bilete flyttar ikkje kontrollane. Barnespelaren hentar ikkje trickplay.

### Ansvarsdeling

Fråkopla motor er skild frå aktiviteten. Innstillingar, lokal sporveljing, framdrift,
ASS-rendering, fontlesing, mediekjelder, SUP-parser, rørsler, meny og førehandsbilete har
eigne filer og avgrensa kontraktar. Nett- og fråkopla avspeling deler skjermtilstand,
renderar og innstillingsmotor. Dette er ei gradvis oppdeling av spelaren; nettmodellen
og hovudskjermen er framleis store og er ikkje ei full omskriving av apparkitekturen.

## Verifisering av første oppgraderingsrunde

Tala i denne delen gjeld den første ferdige debug-APK-en. Vidare kontroll etter
MKS-/filcache-endringane er dokumentert i fullføringsdelen nedanfor.

`testDebugUnitTest assembleDebug assembleDebugAndroidTest` gav **BUILD SUCCESSFUL**.
**1188 av 1189 JVM-prøver består**, med éi medvite hoppa over (`realNetworkWhenAskedFor`).
Logg: `app/build/upgrade-verified-build.txt`. Den siste reine test-APK-bygginga ligg i
`app/build/upgrade-final-test-build.txt`.

| Kontroll på endeleg debug-APK | Resultat | Lokal logg |
| --- | --- | --- |
| TV, API 36, eksisterande testprofil 5566 | **68 bestått, 3 hoppa over, 0 feil** | `app/build/upgrade-verified-tv.txt` |
| Mobil, API 36, eksisterande testprofil 5562 | **46 bestått, 0 hoppa over, 0 feil** | `app/build/upgrade-phone-complete.txt` |
| Varig framdriftskø over full prosessavslutting | **Begge fasane består**, med tvungen prosessavslutting mellom lagring og lesing | `app/build/upgrade-verified-restart.txt` |
| Lint, begge modulane | **0 feil, 213 åtvaringar og 2 tips** (210 åtvaringar i appen, 3 i FFmpeg-modulen) | `app/build/upgrade-verified-lint.txt`, `app/build/upgrade-final-lint.txt` |

Dette er 114 beståtte Android-prøvekøyringar på dei to flatene, i tillegg til dei to
restartfasane. Dei tre TV-hoppane gjeld touch, bilete-i-bilete og mobilens skjermbaserte
Tilbake-kontroll; alle tre er prøvde på mobil. TV-runden omfattar òg avbrot i
framdriftsrapportering mot begge tenarfixturane og den felles fråkopla motoren:
nedlasting, lukka testtenar, lokal avspeling, lagring, ny modell med gjenopptaking og
avvising av den gamle eigaren etter kontobyte.

Minispelarprøva ventar no på eit faktisk mindre vindauge og minst 1,5 sekund vidare
avspeling før retur til fullskjerm. Dette fjernar ei kappløping der testen snudde Android
sin PiP-overgang før han var ferdig. Retur krev fullt vindauge, fokus, vidare avspeling og
berre éin startmelding til tenaren. Dei eldre episodetestane er òg tilpassa den eksisterande
kontrakten: neste-episode-tilbod kan visast tidleg, men automatisk nedteljing skal vente
til videoslutt. Avvising av tilbodet skal framleis hindre automatisk oppstart.

Fem native prøver er køyrde på både eksisterande test-TV 5566 og testmobil 5562, API 36:
ASS-teikningar og tekst med seek/av/på/forsinking, tom ASS som tillèt videoslutt, animert
MKV-ASS, ekstern SUP etter spoling inn i ei aktiv rute og innebygd DVD/VobSub. Video,
undertekstar og bilete er syntetiske. Dette viser lokal dekoding på x86_64-emulator,
ikkje ein formatgaranti for all maskinvare og alle dokument.

Menyen er visuelt kontrollert med skriftstorleik 2.0 på begge testflatene. Mobil-OSD har
tre transporthandlingar og éin valknapp; TV-OSD har tidslinje og éin valknapp.
Førehandsbiletet ligg over tidslinja. Endelege syntetiske bilete ligg lokalt i
`app/build/upgrade-review/`: `options-phone-font2-final.png`, `options-tv-font2-final.png`,
`player-phone-final.png` og `trickplay-tv-final.png`. Fargeflata i trickplay-biletet er ei
kjend sprite-rute i prøva.

Språkkontrollen godkjenner **1665 nøklar**, pluss éin tillaten standardnøkkel.
Native-kontrollen stadfestar fire ABI-ar, 16 KB ELF-justering for 64-bit og åtte
lisensdokument i APK-en. Versjonskontroll og workflow-YAML er kontrollerte lokalt;
den endra CI-køyringa er ikkje køyrd på GitHub.

Begge testemulatorane er avslutta normalt utan sletting eller nullstilling av data.
Review-TV 5564 står att; profilane med ekte kontoar og produksjonspakken er ikkje endra.

Debug-APK: **48 376 800 byte**, SHA-256
`1563d246a9b9c3fdcf7a0e13900270297f7ee1496727f379b0c1c669443b2cea`.
Dette er om lag 17,4 MB meir enn før libass vart lagt til. Den universelle APK-en
inneheld alle fire native ABI-ar. Ingen versjonsauke eller publisering er utført.

## Det som framleis krev eiga prøving

- Ingen fysisk TV, HDMI-mottakar eller telefon er tilgjengeleg via ADB i denne økta.
  TrueHD/Atmos/DTS-HD bitstream, A/V-synk med positive og negative lydverdiar, HDR og
  langvarig 4K-avspeling er ikkje verifiserte. Lokal FFmpeg-dekoding er ikkje det same
  som korrekt bitstream til ein mottakar.
- RAM, oppstartstid og frame-time på Chromecast eller ein svak TV-SoC er ikkje målte.
  Tidlegare målte Compose-komposisjonstal er dokumenterte i
  [stabil 1.0-statusen](STABLE_1_0_PROGRESS.md#målte-compose-rettingar--17-oktober-2026).
  Minne-/arbeidsgrenser i koden er ikkje ytingsmålingar på slik maskinvare.
- API-prøvene bruker lokale Jellyfin-/Emby-fixturar. Ekte tenarar, fontvedlegg og
  trickplay-bibliotek må framleis prøvast i ei innlogga review-økt.
- Testmobilen har om lag 293 MiB ledig på `/data`, under den eksisterande tryggleiksgrensa
  på 512 MiB for nye nedlastingar. Full nedlastings-til-aktivitet-flyt på telefon er difor
  ikkje godkjend her. Den felles fråkopla motoren er prøvd på test-TV med nok plass.
  Grensa og brukardata er bevarte.
- Separate IDX/SUB-par, uttømmande ASS-formatdekning og omgåing av den nemnde
  MKV/PGS-søkjegrensa står att. Serverbasert reserve må framleis vere tilgjengeleg.
- Native kjeldearkiv er no pakka og kontrollert; sjå [native-dokumentasjonen](NATIVE_SUBTITLES.md).
  Signert/R8-oppgraderingskontroll er dokumentert i fullføringsdelen nedanfor.

## Ny poengvurdering og fysisk prøving

[Den nye gjennomgangen](PLAYER_REVIEW_2026-10-17.md) vurderer alle sju kategoriar med
konkurrentsamanlikning, svakheiter og krav for 9–10. [Maskinvarematrisa](PLAYER_HARDWARE_QUALIFICATION.md)
og `scripts/measure-android-ui.py` gjer den vidare kvalifiseringa repeterbar.

## Fullføring: MKS, stor bitmap-cache og signert bygg

Den siste runden legg til MKS med DVD/VobSub/PGS, stabil sporidentitet og korrekt
språkval når eit spor finst både inne i MKV-en og som separat fil. Bitmap-filer blir
førehandslasta til privat diskcache i staden for ei stor ByteArray. Taket er to filer /
256 MiB, med 64 MiB ledig reserve. Vanleg tekst og fontar held fram i avgrensa RAM-cache.
Utkasta språk blir henta på nytt ved val. Lukking avbryt førespurnader og fjernar eigne
filer. Restfiler frå prosessdød blir rydda før plassbudsjettet blir vurdert.

Tre nye cacheprøver dekkjer ei 17 MiB-fil, autentiserte førespurnader, byteområde frå
disk utan ny nettverkslasting, feil-/oversize-opprydding, utkasting, to aktive spelarar
og lite diskplass. Den siste prøva feila før rekkjefølgja for restfilrydding vart retta
(`app/build/upgrade-cache-low-space-red.txt`); ho består i sluttkøyringa.

| Kontroll | Resultat | Logg |
| --- | --- | --- |
| Heile JVM-pakka etter siste cacheretting | **1195 bestått, 1 medvite hoppa over, 0 feil** | `app/build/upgrade-completion-verified-debug.txt` |
| Brei TV-regresjon før dei siste spor-/plassrettingane | **70 bestått, 3 mobilspesifikke hopp, 0 feil** | `app/build/upgrade-completion-tv.txt` |
| Sluttkontroll av motoren på endeleg debug-APK, TV 5566 | **9 av 9 bestått**: MKS, native ASS/SUP/DVD, innebygd PGS og treg tekstuthenting | `app/build/upgrade-completion-verified-tv.txt` |
| Mobil 5562 etter sporvalrettinga, før siste rekkjefølgjeretting for diskplass | **48 av 48 bestått**, med PiP, rotasjon, font 2,0, gesture og MKS | `app/build/upgrade-completion-phone-final.txt` |
| Lint etter siste kjeldeendring, begge modulane | **0 feil, 214 åtvaringar og 2 tips** | `app/build/upgrade-completion-verified-release.txt`, lint-XML i dei to modulane |

Siste diskplassendring er dekt av den fulle JVM-pakka og den endelege TV-kontrollen;
heile mobilpakken er ikkje køyrd om att for denne endringa. Den første mobilrunden vart
avbroten fordi emulatorens **System UI** hadde ei ANR-rute over appvindauget. Etter
«Wait» og ny prøvekøyring består alle 48. Ingen kontoar eller brukar-/AVD-data vart sletta.

MKS-søkjeprøva krev no at Media3 først tømmer ruta ved ny posisjon, deretter viser
den aktive bitmapen medan spelaren står pausa på 8 sekund. Ei tidlegare forventning
om at bitmapens `presentationTimeUs` måtte vere 8 sekund var feil: standardrenderaren
bruker [ruta sitt opphavlege endringstidspunkt](https://github.com/androidx/media/blob/release/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/text/TextRenderer.java).
Rett test består på begge flatene. Språkkoden `nor` blir normalt kanonisert til `no`.

Endeleg debug-APK: **48 376 800 byte**, SHA-256
`3c69d08ea980a42275515ca8ecf4dcaadee9a5a19182e01837fcf3c74f5bc10c`.
Instrumenteringa er berre køyrd på dei eksisterande isolerte profilane 5562 og 5566.
Den varige framdriftskøa og dei to restartfasane frå første runde er uendra.

Lint har éi ny `UsableSpace`-åtvaring om alternativet `StorageManager.allocateBytes`.
Bitmap-cachen bruker konservativt den ledige plassen som allereie finst, minus reserve,
og ber ikkje Android frigjere andre cachar for å få plass til valfrie undertekstar.
Åtvaringa er synleg og ikkje undertrykt. Dei andre 213 åtvaringane og to tipsa stod òg
i første runde. Ingen ny lint-feil er innført.

Det komplette native kjeldearkivet er pakka frå ti låste Git-tre, med alle rekursive
submodular og byggje-/relenkingsinstruksar. Innhald og hash er dokumenterte i
[NATIVE_SUBTITLES.md](NATIVE_SUBTITLES.md). Arkivet er kontrollert, ikkje bygd om til
bit-identiske native bibliotek i denne økta.

### Produksjonsbygg og oppgradering

`lintDebug assembleRelease` med R8 gav **BUILD SUCCESSFUL** på endeleg kjelde.
Logg: `app/build/upgrade-completion-verified-release.txt`. Produksjons-APK-en er
`app/build/outputs/apk/release/app-release.apk`, **28 042 752 byte**, SHA-256
`6b2c7392e747427192ab064fedf7aa14bc0ef3d9c6cb7896f74dcc8128c9d723`.
Pakken er `app.reelstack`, versjon `1.0.0-beta17` / kode **154**, minSdk 26 og targetSdk 36.
Dette er eit lokalt verifiseringsbygg med same versjonstal, ikkje ei ny publisert release.

`apksigner verify` stadfestar den eksisterande signaturen:
`36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
APK-en er ikkje debuggable. Fire native ABI-ar, åtte lisensdokument, 16 KB ELF-justering
for 64-bit og APK-justering med `zipalign -c -P 16 4` består. R8-mappinga bevarer alle
seks JNI-bru-klassane frå `ass-kt`. Mappinga ligg i `app/build/outputs/mapping/release/`.

Signert `install -r` over førre produksjonsapp på **review-TV 5564** gav `Success`.
Visuell kontroll viser same lagra profil og eksisterande Jellyfin-/Emby-bibliotek etter
oppgraderinga. Private før-/etterbilete ligg i `app/build/upgrade-review/`; dei er ikkje
i Git. Ingen instrumentering vart køyrd på denne profilen. Ein systemdialog frå Google
TV om å setje opp ny einingslås vart utsett med «Not now»; ingen PIN vart oppretta.

Dette stadfestar installasjon, oppstart og bibliotekvising med R8. Native avspeling er
testa på debug-APK-en, medan JNI-bevaring og binærpakking er kontrollerte statisk i
release-APK-en. Fysisk receiver-/HDR-prøving står framleis att. Ingen commit, push,
versjonsauke eller GitHub-publisering er utført.

### Måling av produksjonsbygget på review-TV

Etter bygginga, med dei isolerte testemulatorane avslutta, vart
`scripts/measure-android-ui.py` køyrt på 5564. Fem kalde prosessoppstartar gav
**809, 863, 699, 703 og 680 ms** til første viste ramme; median **703 ms**.
PSS etter 8 sekund var **137 916 KiB / 134,7 MiB**. Ei sekvens med 28 D-pad-tastar
utan val/avspeling gav **278 UI-rammer, 79 janky (28,4 %)** og p50/p90/p95/p99
**25/44/53/77 ms**. Fokus var i Spole sitt hovudvindauge under kontrollen.

Rådata ligg i `app/build/upgrade-review/release-tv-metrics.json`. Modellen er
`sdk_google_atv64_amati_x86_64`, API 36, med `debuggable=false`. Dette er ein emulator,
ikkje svak fysisk TV-maskinvare. Første viste ramme er ikkje ferdig lasta bibliotek;
`gfxinfo` omfattar ikkje SurfaceView-video. Nettverk, biletecache og vertsmaskina påverkar
resultatet. Ingen samanliknbar førmåling eller måling av konkurrerande appar vart gjort.
Jank-talet blir ståande som eit konkret oppfølgingspunkt i ytingsvurderinga.

## Fullført oppdeling av hovud-ViewModel

Etter den første vurderinga vart `ReelstackViewModel` delt frå 4150 til 457 linjer.
Han eig navigasjon og øktgrenser; fjorten koordinatorar eig funksjonslogikk, jobbar
og mellomlager. Dei gamle `LibraryViewModel`, `RequestsViewModel` og `ProfileViewModel`
er fjerna. Ingen koordinator får heile `AppContainer` eller hovudmodellen.

`ReelstackFeatures` opprettar eit nytt komponentsett for kvar økt. `SessionUiState`
tilbakekallar gamle skrivarar før jobbar blir kansellerte. Det hindrar at også ein
sein callback eller `finally`-blokk publiserer til neste konto. Heim, Oppdag og
innloggingskladden har synkrone, lesbare projeksjonar av same atomiske tilstand.
Detaljlasting blir kansellert ved navigasjon, og bibliotekcachen kastar ut den minst
brukte sida etter åtte sider. OSD og lagringsformat er uendra.

Arkitekturen og vedlikehaldsgrensene er skildra i
[VIEWMODEL_ARCHITECTURE.md](VIEWMODEL_ARCHITECTURE.md). Arkitekturkarakteren er oppdatert
til **8/10**, og snittet for dei sju kategoriane til **7,6/10**.

Verifikasjon av denne endringa:

- JVM: **1208 totalt, 1207 bestått, 1 medvite hoppa over**, ingen feil. Det omfattar
  28 målretta prøver av dei nye funksjonsgrensene. Logg:
  `app/build/viewmodel-refactor/full-tests-build.txt`.
- Mobil 5562: **38/38** prøvde app-/UI-flytar bestått. TV 5566: **19/19** bestått.
  Dette er eit utval for innlogging, kontobyte, PIN-grenser, førespurnader, historikk,
  detaljar og TV-navigasjon; heile Android-testpakken er ikkje køyrd.
- Første Android-runde hadde to mobilfeil og tre TV-feil fordi grensetestane føresette
  eit tomt testmiljø. Testane opprettar no eksplisitt vaksen-/demo-føresetnad og
  gjenopprettar tidlegare syntetiske tilkoplingar i `finally`. Dei tre påverka prøvene
  vart køyrde på nytt på begge einingane: **3/3** på kvar. `app-debug.apk` var den
  same gjennom desse rundane; berre test-APK-en vart endra. Loggar: `phone-tests.txt`, `tv-tests.txt` og
  `boundaries-5562-final.txt` / `boundaries-5566-final.txt` under `app/build/viewmodel-refactor/`.
- Omsetjingar: 1665 nøklar og 1 tillaten nøkkel berre i standardspråket. Release-metadata
  er konsistente. Versjonen er framleis `1.0.0-beta17`, kode 154.
- `lintDebug assembleRelease`: **BUILD SUCCESSFUL**, 17 minutt 13 sekund.
  Lint: **0 feil**, 214 eksisterande åtvaringar (211 i appen, 3 i avspelingsmodulen)
  og 2 hint. Logg: `app/build/viewmodel-refactor/lint-release.txt`.
- Signert produksjons-APK: `app.reelstack`, `1.0.0-beta17` / 154, minSdk 26,
  targetSdk 36, utan debug-flagg. Storleik: **28 042 752 byte**. SHA-256:
  `f5664a46a688d16511a7605e9ef90c781cc1f7e3f92f99b7a6e3ccb598a3e375`.
  Fil: `app/build/outputs/apk/release/app-release.apk`.
- Signaturen er verifisert mot den eksisterande nøkkelen:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
  `zipalign -c -P 16 4` og `scripts/check-native-subtitles.py` passerer. Alle fire
  ABI-ar, 64-bit 16 KB-justering og åtte lisensvedlegg er med. Dei seks ASS-JNI-klassane
  er bevarte i `app/build/outputs/mapping/release/mapping.txt`.
- Debug-APK-en som vart brukt i Android-prøvene har SHA-256
  `78c4196be58b84a745d5eae367301919a7bf9d8da5ff12613fb71927b129b9ff`.
- Signert `install -r` på review-TV 5564 gav **Success**. Appen starta, viste lagra
  profil og ekte Jellyfin-/Emby-innhald. Heim → Bibliotek → titteldetaljar vart prøvd
  visuelt. Private skjermbilete ligg under `app/build/viewmodel-refactor/tv-*.png`
  og er ikkje i Git. Dette er installasjon og UI-kontroll av det nye R8-bygget;
  ytingsmålingane ovanfor gjeld bygget før ViewModel-oppdelinga.

Begge isolerte testemulatorane vart avslutta etter prøvene. Review-TV-en med ekte
kontoar har ikkje vore brukt til instrumentering eller nullstilling. Ingen commit,
push, versjonsauke eller GitHub-publisering er utført.
