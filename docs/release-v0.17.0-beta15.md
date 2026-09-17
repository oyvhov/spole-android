# Spole 0.17.0-beta15

Dette er ei testutgåve. Ho rettar avspelingsmodusen på Emby og held surroundlyden i live når tenaren må omkode han.

## Viktige endringar

- Spør økta på tenaren om kva som faktisk skjer med straumane, i staden for å lese det ut av omkodings-URL-en. Jellyfin skriv `VideoCodec=copy` når biletet berre blir kopiert; Emby skriv den ekte kodeken same kva han gjer, så kvar Emby-økt vart vist som «Full omkoding» også når biletet gjekk urørt gjennom.
- Rettar same misforståinga i det Spole rapporterer tilbake: `PlayMethod` blir «DirectStream» når biletet blir kopiert, så Emby sitt eige dashbord sluttar å telje Spole som ein full omkodar.
- Ber om Dolby Digital i staden for AAC når lyden må omkodast på ei eining som melder passthrough. AAC 5.1 blir dekoda av TV-en og sendt vidare som det utgangen hans ber, altså to kanalar; AC3 går urørt gjennom settet og kjem fram til mottakaren som 5.1.
- Lét ein AC3- eller EAC3-straum bli kopiert når det er biletet som må omkodast.
- Endrar ikkje kva som kan spelast direkte. Ei DTS-fil blir framleis omkoda på ei eining utan DTS-dekodar eller DTS-passthrough.

## Distribusjon

- Pakke: `app.reelstack`
- Testutgåve: `0.17.0-beta15`
- APK-en er signert med den eksisterande Spole-produksjonsnøkkelen.
- Releasen er ei prerelease og kan installerast over tidlegare Spole-betaer.

Sjå verifikasjonsrapporten for bygg, testar, signatur og SHA-256.
