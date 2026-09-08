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
