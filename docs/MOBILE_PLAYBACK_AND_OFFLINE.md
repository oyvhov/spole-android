# Mobilavspeling og offline

## Lokal video

`JellyfinPlayerActivity` held skjermen vaken berre medan den lokale spelaren faktisk spelar eller
bufrar. Ved lås, Heim eller annan bakgrunnsbruk pausar han og rapporterer framdrift; han startar
ikkje att automatisk. PiP er framleis ei uttrykkeleg handling frå spelaren, ikkje noko Heim gjer
automatisk. Spole køyrer ikkje ein `MediaSessionService` for lokal video, så det finst ikkje
skjult bakgrunnsavspeling.

Cast er annleis: når mottakaren har godteke LOAD, kan telefonen låsast eller appen avsluttast utan
at mottakaren stansar. Ved profilbyte, utlogging eller fjerning av ei teneste stoppar Spole først
Cast og ryddar relevante private offline-data.

## Offline-reglar

`OfflineMediaCandidate` er portvakta for nedlastingsjobbar. Berre komplette, direkte
kompatible Jellyfin-/Emby-filer som tenaren uttrykkeleg leverer som direkte fil kan få ei lokal
fil. Live-TV, ISO/platemenyar, DRM, ufullstendige filer og alt som krev tenartranskoding er nekta.

Nedlasting startar berre frå spelaren på mobil/nettbrett — aldri frå TV. Media3 køyrer som ein
vedvarande app-privat jobb med lågprioritets varsel, prosentsframdrift og pause/hald fram. Berre-
Wi-Fi-innstillinga gjeld òg denne jobben. Førespurnaden i Media3-databasen har berre hasha profil-,
teneste- og medienøklar; tilgangsteiknet vert henta frå aktiv konto akkurat når HTTP-førespurnaden
skal sendast. Ein jobb frå ein annan profil vert stansa ved profilbyte og får aldri låne tokenet til
aktiv profil.

`OfflineStorage` nyttar berre `filesDir/offline/<profil-hash>/<teneste-hash>/`, og Media3-cachen
ligg under appen si private `filesDir/offline-media3/`. Ingen av delane er delbare eller eksterne.
Fjerning av profil, utlogging eller teneste fjernar både jobbane og relevante private filer før
konto-teikn vert sletta.

Ei lagringsgrense og eit eige nedlastingsbibliotek med sletting kjem før brei utrulling. Dei er
ikkje skjulte som ferdige funksjonar: denne versjonen tilbyr berre trygg nedlasting frå den lokale
spelaren, med pause/hald fram i systemvarslet.

## Langtest-matrise

Køyr minst 60 minutt direkteavspeling og transkoding kvar, med skjerm på, skjermlås, manuell PiP,
rotasjon, Wi-Fi til mobilnett og tilbake, kort nettbrot, app i bakgrunnen og profilbyte. Test
offline både med avbroten nedlasting og etter utlogging/tenestefjerning, og kontroller at inga fil
er att i den private offline-mappa.
