# Spole 0.17.0-beta19

Rettingar for mobilavspeling og TV etter tilbakemeldingane frå beta18.

- Ved feil i Android sin lyddekodar prøver appen lokal FFmpeg-dekoding før serveromkoding. Same videostraum, posisjon, pause og sporval blir bevarte.
- Fungerande maskinvaredekoding og TV-passthrough er framleis førsteval.
- Betre tilpassing av TV-biletfrekvens: kjeldemetadata ved manglande dekodarfrekvens, heiltalsmultiplar som 25 fps på 50 Hz, og uendra oppløysing.
- Stats for Nerds viser dekodarnamn og lydavbrot. Panelet kan rullast ved stor skrift.

Universal release-APK for mobil og TV, versjonskode **98**, same pakke og signatur.
Oppdater via **Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no**
med testutgåver på, eller installer APK-en nedanfor over eksisterande app.

## Kontroll og avgrensingar

631 einingstestar og 29 målretta Android-testar bestod også etter versjonsauken.
Testane bruker syntetiske Emby-/Jellyfin-kjelder og injisert lyddekodarfeil med reell
FFmpeg-avspeling etterpå. Fysisk Pixel 9 Pro og Shield/optisk/Sonos er ikkje verifiserte.
TV-endringane kan redusere ujamne rørsler, men er ikkje ei stadfesting av årsaka til
den rapporterte Jellyfin-hakkinga. Den kjende PiP-returfeilen er ikkje retta her.

FFmpeg-lisensane følgjer APK-en; uendra native-kjelde og relenkingsmateriale ligg i
`spole-ffmpeg-source-and-relink.tar.gz` saman med APK-en.
