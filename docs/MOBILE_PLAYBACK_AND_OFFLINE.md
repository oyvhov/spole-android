# Mobilavspeling og offline

## Lokal video

Mobil og nettbrett støttar dobbeltrykk på venstre/høgre side av videoen for å hoppe 10 sekund
bakover/framover. Kvar side dekkjer 45 % av breidda; midten er reservert for avspelingskontrollar.
Ein kort, nøytral puls viser kva side som vart brukt. Nye dobbeltrykk bruker siste søkjemål medan
videoen bufrar. Knappetrykk, tidslinjedrag, menyar, episodeval og feilvising utløyser ikkje gesten.
Spolingsknappane bruker 32 dp bogepiler med «10» inni, ute på kvar si side av videoen.
Ved dobbeltrykk får den aktuelle sida ein svak, mjuk puls. Når kontrollane er opne, reagerer
det eksisterande ikonet; når dei er skjulte, blir same ikon kort vist på sida. Gestvisinga har
inga forklaringstekst, sekundtekst eller bakgrunnsboksar. Knappane held plassen under spoling
og bufring, og alle hopp blir framleis avgrensa til faktisk videolengd.

Tidslinja fyller tilgjengeleg breidd. Nettbrett i begge retningar har 64 dp dragområde,
8 dp strek, 24 dp handtak og større tidsmerke. Telefon har minst 48 dp dragområde.

Framdriftsjournalen vernar posisjonar som enno ikkje er lagra på tenaren. Etter stadfesta
lagring og neste vellukka tenarlesing blir journalposten fjerna, slik at ein annan eining sitt
søk, «sett»-val eller nye episode får styre. Cache/feila nettverkslesing fjernar ikkje ventande
posisjonar. Tilbakespoling til dei første sekunda erstattar eldre lokal framdrift utan eit nytt
hald-fram-kort. Pause blir rapportert også under bufring; ein pausa spelar eller bakgrunnsspelar sender
ikkje same posisjon kvart tiande sekund. Heim oppdaterer personlege rader når ei synleg økt
startar, pausar, stoppar eller byter episode, via tenesta sin eksisterande polling-/push-flyt.

`JellyfinPlayerActivity` held skjermen vaken berre medan den lokale spelaren faktisk spelar eller
bufrar. Ved lås, Heim eller annan bakgrunnsbruk pausar han og rapporterer framdrift; han startar
ikkje att automatisk. PiP er framleis ei uttrykkeleg handling frå spelaren, ikkje noko Heim gjer
automatisk. Spole køyrer ikkje ein `MediaSessionService` for lokal video, så det finst ikkje
skjult bakgrunnsavspeling.

## Kontroll 13.–14. oktober 2026

Den første JVM-køyringa omfatta 980 testar med 0 feil; 52 av 52 relevante Android-testar bestod. Debug-appen og testpakken
bygde; lint hadde 0 feil, 194 åtvaringar og 1 hint. `git diff --check` var rein.
Android-testane køyrde på den eksisterande `Spole_Instrumentation` (5562, API 36), med syntetiske
kontoar og loopback-video frå Jellyfin-/Emby-fixturar. Dei innlogga review-profilane vart ikkje endra.

Kontrollen dekkjer dobbeltrykk på begge sider, fleire hopp før posisjonstilstanden kjem etter,
skjulte kontrollar og bufring, knapp-/menyavgrensing, tidslinjedrag, pause, bakgrunn, rotasjon,
kontogrenser og feila framdriftsrapportar. Nettbrett i begge retningar vart kontrollerte ved
skriftstorleik 2.0, med trykk nær øvre og nedre kant av den 64 dp store tidslinjetreffflata.
Behaldaren rundt handtaket eig treffhøgda; ei ytre minstehøgd åleine forstørra ikkje Material-spelaren si
indre berøringsflate. Ferske tenarsvar etter stadfesta lagring overstyrer gamle lokale posisjonar,
også ved bakoversøk, «sett»-status og eldre tenartidsstempel. Ventande lokale søk blir bevarte.

Dei faktisk lagra og visuelt kontrollerte skjermbileta ligg lokalt i
`app/build/player-review/tablet-landscape-font2.png` og
`app/build/player-review/tablet-portrait-font2.png`. Endeleg Android-logg er
`app/build/player-improvements-complete-android.log` (`OK (52 tests)`), og bygg-/lintlogg er
`app/build/player-improvements-touch-area-build.log`. Mellombels justering av lagringsgrensa
på testeininga vart sett tilbake; ingen AVD vart nullstilt eller appdata sletta.
Synk mellom to ekte einingar/kontoar og fysisk nettbrett er ikkje prøvd i denne kontrollen.
Ingen versjonsauke eller publisert APK-release.

Etter ikonbytet vart debug-app og testpakke bygde på nytt, og 25 av 25 testar i
`PlayerSeekLayoutTest` og `TvPlaybackControlsTest` bestod på same isolerte testeining.
Ikona vart visuelt kontrollerte på telefon og nettbrett i begge retningar, med skriftstorleik 2.0
på nettbrett, og ved dobbeltrykk i begge retningar. Skjermbileta ligg i
`app/build/player-review/icons-phone-landscape.png`,
`app/build/player-review/icons-tablet-landscape-font2.png` og
`app/build/player-review/icons-tablet-portrait-font2.png`; testloggen er
`app/build/player-icons-android.log` (`OK (25 tests)`).

Den endelege gestvisinga utan tekst vart bygd og kontrollert med 25 av 25 testar i
`PlayerSeekLayoutTest` og `JellyfinPlayerUiTest` på same isolerte testeining. Kontrollen dekkjer
ikon på kvar videoside, dobbeltrykk utanfor knappane, skjult OSD, gjentekne hopp, bufring og
stabil plassering, også med skriftstorleik 2.0. Pulsen følgjer appen sitt val for reduserte rørsler.
Visuelt kontrollerte skjermbilete ligg i `app/build/player-review/side-controls-phone-landscape.png`,
`app/build/player-review/side-controls-phone-portrait-font2.png`,
`app/build/player-review/side-controls-tablet-landscape-font2.png` og
`app/build/player-review/side-controls-tablet-portrait-font2.png`. Endelege loggar er
`app/build/player-touch-visual-build.log` og `app/build/player-touch-visual-android.log`
(`OK (25 tests)`).

## Offline-reglar

`OfflineMediaCandidate` er portvakta for nedlastingsjobbar. Berre komplette, direkte
kompatible Jellyfin-/Emby-filer som tenaren uttrykkeleg leverer som direkte fil kan få ei lokal
fil. Live-TV, ISO/platemenyar, DRM, ufullstendige filer og alt som krev tenartranskoding er nekta.

Nedlasting startar berre frå detaljsida på mobil/nettbrett — aldri inne i spelaren, på TV eller i
barnemodus. Film og enkel episode brukar dei valde lyd-, tekstings- og filvala på detaljsida. Ein
serie er ikkje éi fil: handlinga opnar ein episodeveljar og køyrer berre episoden brukaren vel.
Media3 køyrer som ein vedvarande app-privat jobb med lågprioritets varsel, prosentsframdrift og
pause/hald fram. Trykk på varselet opnar **Nedlastingar**. Der kan brukaren sjå framdrift og
plassbruk, velje berre Wi-Fi, pause eller halde fram enkeltjobbar eller alt, prøve på nytt, og
fjerne lokale filer.

Frå 26. september 2026 ligg **Nedlastingar** alltid som eiga rad i Innstillingar (mobil: under
Avspeling; nettbrett: i kategorilista). I menyen er dei **av som standard**; brytaren
«Nedlastingar i menyen» under menyvala set dei inn rett før Innstillingar. Opna frå Innstillingar
får sida ei tilbakepil, menyen markerer Innstillingar, og Tilbake fører dit brukaren kom frå
(`downloadsReturnTab`). Varsel og ei nyleg lagd nedlasting opnar sida på same måte. TV og
barnemodus har framleis verken rad, menypunkt eller rute.

«Sjå fråkopla» opnar spelaren frå aktiviteten. Før 26. september fekk han app-konteksten utan
`FLAG_ACTIVITY_NEW_TASK`, og Android krasja appen (`OfflinePlayerLaunchTest`). Framdrift og
statuslinje følgjer `ActionRow`: knappen står ved sida av teksten berre når teksten framleis får
minst 45 % av breidda, elles under han. Sporet i framdriftslinja har sidefargen, fordi kortet og
Material sitt standardspor er same farge.

Trykk på nedlastingssymbolet gir alltid respons: detaljsida viser medan den trygge
serverforhandlinga går, gjer symbolet utilgjengeleg for dobbelt-trykk, og opnar **Nedlastingar**
berre etter at jobben er lagd i kø. Tenaravslag, utgått tilgang eller utrygg/ikkje-direkte fil
vert ståande som ei konkret melding på den same detaljsida. Ei serie som framleis hentar episodar
forklarer det i staden for å ha ein stille, deaktivert knapp.

Forhandlinga før ein nedlastingsjobb (`offline/OfflineNegotiation.kt`) brukar same
kapabilitetar som avspelinga: `AndroidPlaybackCapabilities`, med dekodarkontroll og FFmpeg-lyd.
Ho spør utan bilettekst, fordi fila blir lasta ned med alle innebygde spor, og utan
straumingsgrense (`OFFLINE_NEGOTIATION_BITRATE`). Offline-spelaren brukar `PlaybackRenderersFactory`
og same lokale lydfallback som den vanlege spelaren. Når kontoen har bytt adresse sidan jobben
vart lagd, blir URL-en flytt over på den gjeldande adressa (`offlineRebasedUrl`) før autentisering.

Berre-Wi-Fi-innstillinga gjeld òg denne jobben, også etter at prosessen har vore stengd.
`DownloadManager` les innstillinga når han blir bygd. Tenesta blir ikkje starta frå
`Application.onCreate`, sidan Android nektar det når prosessen er starta i bakgrunnen.
`MainActivity.onStart` startar ho berre når det finst uferdige jobbar.
Førespurnaden i Media3-databasen har berre hasha profil-, teneste- og medienøklar;
tilgangsteiknet vert henta frå aktiv konto akkurat når HTTP-førespurnaden skal sendast. Ein jobb
frå ein annan profil vert stansa ved profilbyte og får aldri låne tokenet til aktiv profil.

`OfflineStorage` nyttar berre `filesDir/offline/<profil-hash>/<teneste-hash>/`, og Media3-cachen
ligg under appen si private `filesDir/offline-media3/`. Ingen av delane er delbare eller eksterne.
Titlar og episodetekst i nedlastingsbiblioteket er krypterte med Android Keystore; Media3-databasen
inneheld berre hashar. Ei ferdig fil opnar i ein eigen cache-only-spelar med dummy-upstream, slik
at han ikkje kan falle tilbake til nettverket eller ei anna konto. Han krev at aktiv profil og
presis server-/kontoidentitet framleis stemmer. Fjerning av profil, utlogging eller teneste fjernar
både jobbane og relevante private filer før konto-teikn vert sletta.

Spole gjer ikkje automatisk sletting av ferdige filer. Brukaren ser privat plassbruk og fjernar
filer sjølv, slik at eit ferdig val aldri forsvinn utan varsel. Android kan framleis avvise ei
nedlasting når eininga manglar ledig lagring; den feilen vert synleg i biblioteket og kan prøvast
på nytt etter opprydding.

## Langtest-matrise

Køyr minst 60 minutt direkteavspeling og transkoding kvar, med skjerm på, skjermlås, manuell PiP,
rotasjon, Wi-Fi til mobilnett og tilbake, kort nettbrot, app i bakgrunnen og profilbyte. Test
offline både med avbroten nedlasting og etter utlogging/tenestefjerning, og kontroller at inga fil
er att i den private offline-mappa.
