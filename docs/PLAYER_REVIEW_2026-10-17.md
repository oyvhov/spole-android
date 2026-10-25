# Spole etter spelaroppgraderinga – ny vurdering

Vurdert 17. oktober 2026 i `spole-sesong`, på arbeidskopien over `b33d1cb`.
Arkitekturkarakteren er oppdatert frå 7 til 8 etter oppdelinga av hovud-ViewModel;
[ansvarsgrenser og livssyklus](VIEWMODEL_ARCHITECTURE.md) er dokumenterte separat.
Dette er ei kjeldekode-, test- og emulatorbasert vurdering. Konkurrentane er samanlikna
med sine offentlege kjelder, ikkje målte side om side på same fysiske TV. Karakterane
er fagleg skjønn etter skalaen 5 = middelmåtig, 7 = fullt brukbart, 9–10 = best i klassen.
Ei implementert funksjon er ikkje automatisk like moden som tilsvarande funksjon hos
ein etablert spelar. Det finst ikkje eit etterprøvbart tidlegare poengsett her som kan
brukast til å rekne ut ei talfesta forbetring.

## 1. Android TV UX og navigasjon — 8/10

**Samanlikning:** Spole har no ei roleg OSD-flate med tidslinje og éin inngang til
avspelingsvala. Back går tilbake gjennom menyhierarkiet, og D-pad, Play/Pause og
fokusretur har automatiske prøver. Dette nærmar seg den enkle TV-flyten brukaren
etterspør frå [Wholphin](https://github.com/damontecres/Wholphin). Wholphin har òg
tilpassbar framside, kapittel, trickplay og fleire spelarval; enkel utsjånad åleine er
ikkje eit konkurransefortrinn.

**Svakheiter:** Emulatorprøver dekkjer ikkje alle produsentfjernkontrollar, lange
medielister eller fokushistorikk etter prosessdød. Ein samla meny gir få knappar, men
ofte brukte avanserte val krev eit ekstra steg. Bibliotek, detaljark og spelar har
framleis fleire lokale fokuskontraktar som må haldast samkøyrde.

**For 9–10:** Automatiser komplette reiser frå hylle til detaljar, episode, spelar og
tilbake med gjenoppretta fokus/scroll. Mål respons på svak fysisk TV med store bibliotek.
Prøv produsentspesifikke fjernkontrollar og tastatur. Bevar snarvegar og ei enkel OSD;
legg sjeldne val i menyen framfor å gjere hovudflata tettare.

## 2. Mobil og nettbrett — 8/10

**Samanlikning:** Spole har dobbelttrykk for spoling, vertikal volum-/lysstyrkejustering,
pinch for tilpassing/fylling, rotasjon og PiP. Den same motoren tek med Emby og
familieopplevinga. [Findroid](https://github.com/jarnedemeulemeester/findroid) er ein
tydeleg mobilreferanse med nedlasting, mpv/ExoPlayer og navigasjonsrørsler; Spole har
breiare tenesteflyt, men dette beviser ikkje betre detaljpolering.

**Svakheiter:** Gesture kan vere vanskelege å oppdage. Det manglar fysisk prøving av
tommelrekkevidde, systemrørsler langs kantane, nettbrett og ulike skjermforhold. Den
fulle nedlastings-til-aktivitet-prøva på testtelefonen er hindra av for lite diskplass;
den felles fråkopla motoren er prøvd på TV.

**For 9–10:** Test reelle éinhandsgrep, TalkBack og store nettbrett med skrift 2,0.
Gi diskret introduksjon til rørsler og gode tilgjengelege alternativ. Kvalifiser
telefonens komplette fråkopla flyt på ei eining med nok plass og kontrollert nettavbrot.

## 3. Mediemotor, kodekar og undertekstar — 7,5/10

**Samanlikning:** Spole kan no teikne ASS/SSA lokalt med libass, bruke fontvedlegg,
dekode innebygd PGS/DVD og opne annonserte SUP/MKS-sidefiler. Det reduserer behovet for
brenning på tenaren. Store bitmap-filer går til avgrensa diskcache. Dette er eit klart
løft, men [Dune si dokumenterte lydstøtte](https://dune-hd.com/support/app/index.html)
er knytt til konkrete boksar, firmware og lydoppsett. Spole har ikkje tilsvarande
fysisk dokumentasjon for TrueHD/Atmos/DTS-HD eller langvarig 4K.

**Svakheiter:** Ein programvaredekodar beviser ikkje HDMI-bitstream. Innebygd PGS kan
vente til neste display-sett etter spoling. Separate IDX/SUB-par blir ikkje lesne
direkte; ein annonsert MKS frå tenaren kan dekkje dei. ASS har medvitne grenser på
720p/30 Hz, 2 MB dokument og 8 MB fontar. Media3 sin vanlege bufferstrategi er ikkje
ein dokumentert Dune-liknande bufferingsmotor. Store eksterne bitmap-filer må framleis
lastast ferdig før teksten blir slått på; videoen held fram, men tekststart kan kome seint.

**For 9–10:** Kvalifiser dekodar, HDR, lydsignal og søk på ei fysisk maskinvarematrise
med receiver. Prøv bitrate-toppar, nettavbrot og timelange økter. Utvid formatkorpuset
for ASS, bitmap og MKV-søking, og mål bufferpolitikken før han blir endra. Vis korrekt
Direct Play/Stream/Transcode-status og bevar eksplisitt reserve ved reelle formatfeil.
Vurder inkrementell bitmap-lasting med ein separat lastar slik at tidleg tekst ikkje
krev full nedlasting eller held igjen videolastaren.

Den offisielle Jellyfin-TV-klienten er ikkje ein stilleståande «legacy»-referanse:
[0.19.0](https://jellyfin.org/posts/androidtv-v0.19.0/) fekk mellom anna lokal VobSub og
betre HDR-/Dolby Vision-deteksjon. Det ville vere urimeleg å gi Spole ekstra poeng ved
å samanlikne med gamle svakheiter som allereie er retta hos konkurrenten.

## 4. Jellyfin- og Emby-integrasjon — 7,5/10

**Samanlikning:** Spole sitt praktiske fortrinn mot dei Jellyfin-orienterte Wholphin og
Findroid er éi felles brukaroppleving for begge backendane, i tillegg til Seerr og
familieprofilar. Varig framdriftskø overlever lukka spelar/prosess og skil kontoar,
tenarar og profilar. Nyare tenarframdrift får vinne over eldre lokal kø.

**Svakheiter:** Lagringsmodellen er framleis éi tilkopling per tenestetype per profil:
éin Jellyfin og éin Emby, ikkje vilkårleg mange Jellyfin-tenarar. Det er ein konkret
avgrensing, ikkje full multi-server-støtte. Kontrakttestane bruker kontrollerte
tenarsvar; ekte serverversjonar, bibliotek og samtidige klientar gir fleire kombinasjonar.
Biletecachens storleiksgrense er heller ikkje bevis for at alle livssyklusar er utan lekkasje.

**For 9–10:** Innfør stabil server-ID som nøkkel gjennom tilkoplingar, innhald, cache,
nedlasting og framdriftskø, med migrering av eksisterande kontoar. Utvid reelle
Jellyfin-/Emby-integrasjonsprøver for tokenbytte, offline, samtidige klientar og
serverversjonar. Mål heap etter gjentekne konto- og skjermbyte.

## 5. Yting på svak TV-maskinvare — 6,5/10, førebels

**Samanlikning:** Fokuszoom og parallax les no animasjons-/scrollverdiar i rett fase.
Tidlegare lokale prøver målte fokuskomposisjonar frå 10 til 1 og hero-komposisjonar frå
12 til 0. Det fjernar konkret unødvendig arbeid. Det gir ikkje grunnlag for å hevde at
Spole er raskare enn Wholphin på Chromecast eller meir stabil enn Dune under 4K-avspeling.

**Svakheiter:** Det manglar fysisk p95/p99-frame-time, kaldstart, langsiktig PSS og
dekodarbelastning. libass legg til native kode og arbeidsminne. Bitmap-sidefiler bruker
no små kopieringsbufferar, men ferdige bileteruter og Media3-køar bruker framleis RAM.
Ytingskarakteren har derfor lågare tryggleik enn funksjonskarakterane.

**Målt før ViewModel-oppdelinga:** Signert R8-bygg på den eksisterande API 36-TV-emulatoren, med
ekte bibliotek og utan bygg/testemulatorar i arbeid, gav **703 ms median til første
viste ramme** over fem kalde prosessoppstartar (680–863 ms). PSS etter 8 sekund var
**134,7 MiB**. 28 D-pad-tastar gav 278 UI-rammer, der **79 (28,4 %) vart merkte janky**;
p50/p95/p99 var 25/53/77 ms. Dette viser at jamn 60 Hz-flyt ikkje er dokumentert i dette
emulatormiljøet. Det er ikkje eit mål på dekoda videorammer, ferdig lasta bibliotek eller
ei fysisk Chromecast. Utan førmåling på same oppsett kan talet heller ikkje brukast til
å hevde ei talfesta forbetring. Rådata: `app/build/upgrade-review/release-tv-metrics.json`.

**For 9–10:** Køyr repeterbare produksjonsmålingar på faktisk lågprisutstyr, med same
bibliotek og cachetilstand. Bruk Perfetto til å finne lange rammer og test minnestabilitet
over minst 30 minutt. Deretter kan baseline-profilar, oppstart, bildearbeid og spelerbuffer
optimaliserast mot målte flaskehalsar. Måleskript og ei
[fysisk prøvematrise](PLAYER_HARDWARE_QUALIFICATION.md) følgjer endringa.

## 6. Kodekvalitet og arkitektur — 8/10

**Samanlikning:** StateFlow, livssyklusstyrt UI, IO-arbeid og eit omfattande testgrunnlag
er gode fundament. Motoren for fråkopla avspeling, rapportering, justeringar, tekst,
preview og rørsler har fått tydelegare ansvar. Findroid si
[moduldeling](https://github.com/jarnedemeulemeester/findroid/blob/main/settings.gradle.kts)
skil mellom data, spelarkjerne, lokal spelar og appflater. Spole har framleis berre
`:app` og `:playback-ffmpeg`; fleire filer er ikkje det same som uavhengige modular.

**Forbetringa:** Hovud-ViewModel er redusert frå 4150 til 457 linjer. Fjorten koordinatorar
eig funksjonane, med konkrete avhengigheiter, innsprøytbar IO-dispatcher og underordna
jobbar. Komponentsettet blir erstatta ved kontobyte; skrivetilgangen til den gamle økta
blir tilbakekalla før kansellering. Dei tre delvis tilkopla under-ViewModel-ane er fjerna.
Éi atomisk tilstandskjelde og synkrone, lesbare projeksjonar fjernar konkurrerande
tilstandskopiar. Testane prøver komponentane appen faktisk brukar, også seine svar og
byte bort frå og tilbake til same konto.

**Svakheiter:** Skjermsnapshotet er framleis breitt, og funksjonsgrenser er ikkje
handheva av Gradle-modular. Nettspelarmodellen er rundt 920 linjer og spelarskjermen
rundt 1080. Native renderarar og den pakkeinterne Media3-adapteren krev framleis
oppgraderingskontroll. Eksisterande lintgjeld er ikkje fjerna av oppdelinga.

**For 9–10:** Flytt rein avspelingsdomene, backend-kontraktar og UI-flater til avgrensa
modular som handhevar dei nye ansvarsgrensene. Test prosessgjenoppretting og
kryssmodulkontraktar. Lås og kvalifiser Media3/
native oppgraderingar, og reduser relevant lintgjeld gradvis utan å undertrykkje feil.

## 7. Nisjefunksjonar — 8/10

**Samanlikning:** Trickplay med kapittelbilete som reserve, kapittelval, intro-/kreditt-
segment, fart og lyd-/tekstforsinking finst no i den samla menyen. Det er god dekning
utan fleire hovudknappar. Wholphin og Findroid dokumenterer òg trickplay, segment og
kapittel; Spole kan ikkje rekne dette eller Seerr åleine som unikt.

**Svakheiter:** Preview er avhengig av tenarmetadata; kapittelbilete er ikkje det same
som ei full Emby-trickplay-tidslinje. Forsinkingane er øktval, ikkje dokumenterte
kalibreringsprofilar per receiver. Lydsynk med fart og ulike HDMI-modusar er ikkje fysisk
målt. Spole har heller ikkje Wholphin si dokumenterte breidde innan musikk og Live TV/DVR.

**For 9–10:** Kvalifiser trickplay/segment på fleire serverversjonar, lange tidslinjer og
nettavbrot. Prøv forsinkingar mot ei målesekvens og vurder eigne lagra lydprofilar dersom
brukarprøver viser behov. Hald menysnarvegar konsistente og la utilgjengelege funksjonar
falle roleg bort.

## Samla vurdering

**Aritmetisk snitt: 7,6/10** (53,5 / 7, avrunda). Appen har ein konkurransedyktig grunnmur og ein vesentleg
betre spelar. Han er enno ikkje dokumentert best i klassen på maskinvare, mediedekning
eller arkitektonisk oppdeling.

**Verdiproposisjon:** Ei samanhengande og roleg TV-/mobil-/nettbrettoppleving som samlar
Jellyfin, Emby, Seerr og familiebruk. Vel Spole for denne kombinasjonen. Vel ikkje Spole
på eit udokumentert løfte om betre heimkino-lyd enn Dune eller betre TV-yting enn Wholphin.

**Tre viktigaste tiltak før toppsjiktet:**

1. Dokumenter fysisk avspeling og yting: HDMI-lyd, HDR, bitrate-toppar, søk, RAM og
   lange økter på svak TV-maskinvare.
2. Fullfør multi-server-modellen med stabile identitetar og prøvd migrering, slik at
   fleire tenarar av same type fungerer utan kryssing av cache, konto eller framdrift.
3. Handhev funksjons- og spelargrensene med eigne modular og kvalifiser oppgradering
   av Media3/native kode. Oppdelinga av hovud-ViewModel er gjennomført; appmodulen og
   nettspelaren har framleis større vedlikehaldsflater.

Detaljar om implementasjon, lokale bygg og testresultat ligg i
[oppgraderingsloggen](PLAYER_UPGRADE_2026-10-17.md).
