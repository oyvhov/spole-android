# Fysisk kvalifisering av spelaren

Denne prøva skil lokale formatprøver frå dokumentert yting på faktiske einingar.
Ingen rad under er godkjend berre fordi APK-en byggjer eller emulatoren består.
Bruk eigne, lovleg tilgjengelege mediefiler. Bevar kontoar og appdata.

## Einingar og grunnmåling

Prøv minst éin Chromecast med Google TV eller tilsvarande svak TV, éin TV/boks med
HDMI-mottakar, éin telefon og eitt nettbrett. Noter modell, firmware, Android-versjon,
APK SHA-256, nettverk, skjermmodus og mottakar. Bruk signert produksjonsbygg.

```powershell
python scripts/measure-android-ui.py --adb <adb.exe> --serial <serial> --runs 5 --dpad --output app/build/physical-tv-ui.json
```

Skriptet stoppar og opnar den eksisterande appen utan å slette data. `am start -W`
måler første viste ramme, ikkje ferdig innlasta bibliotek. PSS blir henta etter ein
venteperiode. D-pad-sekvensen flyttar berre fokus. `gfxinfo` måler Android-UI, ikkje
videorammer på SurfaceView. Kontroller skjermen før og etter målinga; ei innloggingsside
og eit innlasta bibliotek kan ikkje samanliknast. Køyr utan bygg eller andre tunge
prosessar. Repeter i same bibliotek og med same biletecache ved samanlikning.

Mål for 9–10-vurdering: dokumenter p50/p95/p99, jank-rate, minne etter 30 minutt,
kaldstart og tid til synleg bibliotek. Ta Perfetto-spor ved lange rammer og dokumenter
hovudtråd, biletedekoding og Compose-arbeid. Vel budsjett for den konkrete målmaskina
før prøva; eit emulatorresultat er ikkje bevis for at eit slikt budsjett er nådd.

## Avspelingsmatrise

| Område | Prøve | Godkjenning |
| --- | --- | --- |
| Direkteavspeling | H.264, HEVC Main10, AV1 på støtta dekodar; Jellyfin og Emby | Tenarloggen viser forventa Direct Play/Direct Stream; ingen uventa videotranskoding |
| Lyd | AC-3, E-AC-3/Atmos, TrueHD/Atmos, DTS-HD MA/DTS:X | Mottakaren viser faktisk innsignal; ingen påstand om bitstream ut frå appens codec-namn |
| HDR | HDR10 og støtta Dolby Vision-profilar | Skjermens signalinformasjon og visuell kontroll stadfestar modus og fargar |
| Tung ASS | Stilar, fontvedlegg, teikningar og samtidige animerte ruter | Lesbar tekst, ingen videohakk; dokumenter eventuelle grenser frå 720p/30 Hz-rendering |
| Bitmap | Innebygd PGS/DVD, SUP, MKS frå dei aktuelle serverversjonane | Pause/spoling inn i aktiv rute, av/på og tre språkbyte; ingen uvald fil lasta |
| Synk | Lyd ±2 s, tekst ±10 s; spoling, pause og fart 0,5/1/2 | Mål bilete/lyd mot kjend testsekvens; verdiane overlever den avtalte øktlivssyklusen |
| Lang økt | 60 minutt 4K med stor bitrate, minst ti søk og nettavbrot | Ingen vedvarande minnevekst, låst fokus eller mist lyd; tidsstempla feillogg |
| Fjernkontroll | D-pad, Play/Pause, Back, kapittel, neste episode og menyer | Fokus går tilbake til rett kontroll; ingen dobbel handling |
| Mobil/nettbrett | Rotasjon, PiP, gesture, font 2,0, breitt vindauge | Berre tilsikta handling, ingen overlappar; vidare avspeling utan omstart |
| Fråkopla | Nedlasting med nok diskplass, flymodus, lukk/opne, seinare synk | Rett eigar og posisjon; nyare tenarframdrift blir ikkje overskriven |

Prøver med ekte kontoar høyrer til dei eksisterande review-profilane. Instrumentering
skal berre gå på dei isolerte testprofilane. Lagre personlege skjermbilete og tenarloggar
utanfor Git, og fjern tilgangsteikn før noko blir delt.
