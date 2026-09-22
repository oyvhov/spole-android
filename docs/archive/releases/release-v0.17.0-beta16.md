# Spole 0.17.0-beta16

Dette er ei testutgåve. Ho rettar den eine feilen som gjorde mest skade: at ein avspelingsfeil kosta både biletet og surroundlyden.

## Viktige endringar

- Fell eitt steg om gongen i staden for rett til botnen. Ein avspelingsfeil sette før heile økta over på den forsiktige profilen — H.264, høgst 1080p, AAC i stereo, straumkopiering av — og der vart ho verande ut filma. No blir berre lyden omkoda først, med biletet urørt; eit bilete som faktisk feila i dekodinga, eller ein feil nummer to, er det einaste som kostar videoen.
- Målt side om side på ein Pixel 9 Pro XL, same fil og same tenar: Emby sin eigen app speler EAC3 5.1 direkte, medan Spole omkoda 1080p H264 til H264 og 5.1-lyden til AAC 192 kbit/s i stereo. Det var denne sperra som gjorde det, ikkje ei manglande kodekstøtte.
- Stigen har botn: to steg, så stopp. Inga evig omstartsløyfe.
- Ventetida ved ei hengande avspeling er uendra. Ein stopp i bufferen er bandbreidd, ikkje ein kodek eininga ikkje taklar, så den vegen går framleis rett til den forsiktige profilen.

## Distribusjon

- Pakke: `app.reelstack`
- Testutgåve: `0.17.0-beta16`
- APK-en er signert med den eksisterande Spole-produksjonsnøkkelen.
- Releasen er ei prerelease og kan installerast over tidlegare Spole-betaer.

Sjå verifikasjonsrapporten for bygg, testar, signatur og SHA-256.
