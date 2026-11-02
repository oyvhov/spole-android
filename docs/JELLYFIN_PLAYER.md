# Jellyfin-spelaren i Spole

Status: integrert i Spole 0.15.0. Sjå `VERIFICATION_v0.15.0.md` for releasekontrollen.

## Brukarflyt

Opne ein tittel frå Jellyfin-biblioteket → **Spel av i Spole**. For ein serie vel du sesong og
episode først. Berre episodar som finst i biblioteket kan spelast. Har Jellyfin lagra ein
posisjon, vel du mellom å halde fram og å starte frå byrjinga.

Spelaren har pause, ti sekund fram/tilbake, tidslinje, lydspor, undertekstar, kvalitetsval og
skjermrotasjon. Kontrollane tonar ut under avspeling og kjem tilbake ved trykk. Tilbake går til
episodelista eller detaljarket. Når appen går i bakgrunnen, blir videoen sett på pause; han
startar ikkje att av seg sjølv. Feil gir høve til å prøve igjen eller opne Jellyfin.

## Teknisk avgrensing

- Media3/ExoPlayer 1.10.1, ikkje ein innebygd nettlesar. Ingen ny backend.
- Personleg Jellyfin-identitet blir stadfesta før bibliotek og avspeling blir opna. Seerr si
  kontokopling blir respektert; ein delt administratornøkkel skal ikkje bli ein falsk personleg konto.
- `PlaybackInfo` forhandlar format med Jellyfin. Konservativ telefonprofil: H.264, 8-bit,
  opptil 1080p og stereo AAC/MP3 direkte når tenaren godkjenner fila. Andre format treng
  tilpassing på Jellyfin-tenaren. Dette er ikkje eit løfte om direkte HEVC/HDR/4K-avspeling.
- Automatisk grense er 12 Mbit/s på Wi-Fi og 4 Mbit/s elles. 4 og 2 Mbit/s kan veljast manuelt.
- Tekstspor blir henta autentisert som WebVTT. Biletundertekstar krev innbrenning på tenaren.
  Avansert ASS-typografi blir ikkje lova bevart ved konvertering til WebVTT.
- Lyd-, kvalitets- og bilettekstbyte forhandlar straumen på nytt og bevarer posisjonen.
  Vanlege tekstspor blir valde lokalt utan å starte videostraumen på nytt. Berre valde
  tekstspor blir lasta. Ein video som er sett på pause, skal halde seg på pause etter sporbyte.
- **Teksten ventar på biletet, aldri omvendt (26. september 2026).** Eit valt tekstspor blir
  henta ved sida av videoen (`loadChosenSubtitle`) og slått på først når fila ligg i
  `SubtitleMemoryCache`. Før dette var sporet slått på frå start. Medan Media3 lastar ei valt
  sidecar-fil, held `CompositeSequenceableLoader` videolastaren att, så ein episode starta, gjekk
  tom for bilete etter om lag 1 MiB og venta til tenaren hadde henta ut teksten frå MKV-fila.
  Det var mest synleg på TV første gong ein episode vart spelt. Same regel gjeld byte av tekst i
  menyen. Ved direkte avspeling blir det planlagde lydsporet sitt språk sett som ønskt språk før
  `prepare()` når språket er eintydig, slik at Media3 sitt første val ikkje må rettast (og
  bufferen kastast) etterpå. Regresjonstest: `JellyfinPlayerTest.slowSubtitleExtractionNeverStallsThePicture`.
- Start, pause/spoling, framdrift kvart tiande sekund og stopp blir sende til den eigne
  Jellyfin-økta. Dette endrar den innlogga kontoen sin sjåposisjon. Nettverksfeil kan hindre
  lagring; spelaren viser ei åtvaring. Ingen varig fråkopla rapportkø.
- Tilgangsteikn går i autorisasjonshovud, ikkje medie-URL-ar eller aktivitetsparametrar.
  Manifest, segment og undertekstar må halde seg på same tenar og under same grunnstig.
  Omdirigeringar blir avviste. Oppsett med separat CDN for straumar er ikkje støtta no.
- Android MediaSession tek imot kontrollar for denne lokale avspelinga. Ingen andre
  brukarar eller einingar sine økter blir styrte.

Første utgåve spelar berre verifiserte Jellyfin-element. Eit Seerr-treff utan Jellyfin-element-ID
får ikkje ein gjetta avspelingsknapp. Emby-avspeling, casting, PiP, nedlasting, automatisk neste
episode, kapittel og introhopp er ikkje med i denne runden.

## Hopp over (november 2026)

Jellyfin sine merkte strekk blir henta for intro, rulletekst, resymé («Recap») og førehandsvising
(«Preview»); Emby kan berre intro og rulletekst, og reklame blir ikkje teken med. Reglane ligg i
`SegmentPolicy.kt` med éin test per grense:

- Eit strekk under 3 sekund får ingen knapp, og ingenting blir tilbode dei siste 1,5 sekunda.
- Knappen står på biletet i 6 sekund frå byrjinga av strekket; etter det berre medan kontrollane er
  synlege. På TV tek han fokus berre når han kjem fyrste gongen, slik at kontrollane ikkje mistar
  fokuset når han kjem att saman med dei. OK på knappen hoppar.
- Rulletekst som tek slutt innan 2 sekund frå slutten av fila «går ut fila»: då heiter knappen
  «Neste episode» og startar neste episode, og finst det inga neste episode, er det ingen knapp
  (eit hopp til siste bilete er ikkje noko hopp). Ukjend lengd gir aldri «Neste episode».

## Lyd og undertekst per serie

Val i spelaren blir hugsa per profil og serie (ein film er si eiga «serie») i `TrackChoiceStore`
(lokalt, ingen hemmelegheiter): språk, tittel, tvungen, SDH, kodek, kanalar og plass, og eit
eksplisitt «av» for undertekst. Neste episode får det beste treffet (`TrackMatching.kt`): språket
må vere likt (norsk `nor`/`nob`/`nb`/`nn` er eitt språk), deretter tel tittel, tvungen og SDH mest,
så kodek, kanalar og plass; under minstepoeng, eller utan treff i språket, gjeld vanleg standard.
«Av» blir hugsa som av. Valet frå tittelsida gjeld framfor minnet for den tittelen, og blir
sjølv lagra fyrste gongen. Profil-ID er med i nøkkelen, så barneprofilar har sitt eige val.
Avspelaren utan nett (`OfflinePlayerActivity`) er ikkje kopla til minnet.

## Referansar og opphav

[Findroid](https://github.com/jarnedemeulemeester/findroid) vart undersøkt som referanse for
sporval, spelarlevetid og framdriftsrapportering. Prosjektet har
[GPLv3-lisens](https://github.com/jarnedemeulemeester/findroid/blob/main/LICENSE).
Ingen Findroid-kode, ressursar eller bibliotek er kopierte inn i Spole. Implementasjonen er
skriven for Spole sine eigne konto- og nettverkslag og brukar Android Media3.

Protokoll og komponentar er kontrollerte mot Jellyfin sine eigne `MediaInfoController`,
`SubtitleController`, `PlaystateController`, `DeviceProfile` og Android si
[Media3-dokumentasjon](https://developer.android.com/media/media3/exoplayer/hello-world).

## Testmateriale

`app/src/androidTest/assets/player` inneheld berre syntetisk video, to lydtonar og testtekst,
generert med FFmpeg. MP4, HLS-segment og WebVTT blir serverte frå ein lokal testtenar.
Desse filene er berre med i testpakken, ikkje i produksjons-APK-en. Ingen ekte kontoar,
serveradresser eller filmfiler er lagra i testmaterialet.
