# Mobilavspeling og offline

## Lokal video

`JellyfinPlayerActivity` held skjermen vaken berre medan den lokale spelaren faktisk spelar eller
bufrar. Ved lås, Heim eller annan bakgrunnsbruk pausar han og rapporterer framdrift; han startar
ikkje att automatisk. PiP er framleis ei uttrykkeleg handling frå spelaren, ikkje noko Heim gjer
automatisk. Spole køyrer ikkje ein `MediaSessionService` for lokal video, så det finst ikkje
skjult bakgrunnsavspeling.

## Offline-reglar

`OfflineMediaCandidate` er portvakta for nedlastingsjobbar. Berre komplette, direkte
kompatible Jellyfin-/Emby-filer som tenaren uttrykkeleg leverer som direkte fil kan få ei lokal
fil. Live-TV, ISO/platemenyar, DRM, ufullstendige filer og alt som krev tenartranskoding er nekta.

Nedlasting startar berre frå detaljsida på mobil/nettbrett — aldri inne i spelaren, på TV eller i
barnemodus. Film og enkel episode brukar dei valde lyd-, tekstings- og filvala på detaljsida. Ein
serie er ikkje éi fil: handlinga opnar ein episodeveljar og køyrer berre episoden brukaren vel.
Media3 køyrer som ein vedvarande app-privat jobb med lågprioritets varsel, prosentsframdrift og
pause/hald fram. Trykk på varselet opnar **Nedlastingar**, den faste vaksen-destinasjonen i
mobil- og nettbrettnavigasjonen. Der kan brukaren sjå framdrift og plassbruk, velje berre Wi-Fi,
pause eller halde fram enkeltjobbar eller alt, prøve på nytt, og fjerne lokale filer.

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
