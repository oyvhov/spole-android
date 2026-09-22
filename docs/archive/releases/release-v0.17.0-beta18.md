# Spole 0.17.0-beta18

Denne testutgåva prioriterer direkteavspeling frå Emby og Jellyfin.

- Lokal lyddekoding for DTS, AC3, EAC3 og TrueHD når Android ikkje har ein eigna dekodar eller passthrough.
- Bevarer maskinvaredekoding og støtta passthrough som førsteval.
- Buffering og nettverksfeil tvingar ikkje lenger fram full videoomkoding.
- Ei gjenteken lydfeil utløyser ikkje videoomkoding utan at videodekodaren faktisk har feila.
- Retta bruk av originale HTTP-straumar og vidareføring av kompatibilitetsnivå.

Same pakkenamn og signeringsnøkkel; versjonskode 97. Éin universal-APK for telefon og TV.
Opne **Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no**.
Testutgåver må vere slått på.

## Viktig om lyd

Direkteavspeling betyr at serveren slepp omkoding, ikkje nødvendigvis at lydutgangen
leverer surround. Shield TV via optisk til Sonos Playbar kan få stereo ved PCM-dekoding.
Den konkrete HDMI-/TV-/optisk-kjeda må testast fysisk. Pixel 9 Pro og dette TV-oppsettet
er ikkje fysisk verifiserte i denne releasen. Biletundertekstar og ikkje-støtta video/HDR
kan framleis krevje omkoding.

FFmpeg-lisensane ligg i APK-en. Full native-kjelde og relenkingsmateriale ligg i
`spole-ffmpeg-source-and-relink.tar.gz` ved sida av APK-en.

## Kjent avgrensing

Testen for retur frå minispelaren (PiP) til fullskjerm har framleis tidsavbrot på
Android 16-emulatoren. Denne testutgåva rettar direkteavspeling, ikkje denne PiP-flyten.
Dei målretta lyd-/direkteavspelingstestane er bestått; heile spelartestsuiten er ikkje heilt grøn.
