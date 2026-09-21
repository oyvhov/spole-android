# Emby · avspelingstestmatrise

Denne matrisa er avgrensa til reelle Emby-risikoar. Ho erstattar ikkje Jellyfin-testane, og
ho påstår ikkje at ein emulator kan godkjenne alle TV-dekodarar.

| Case | Føresetnad | Forventa resultat | Automatisk dekning | Fysisk kontroll |
| --- | --- | --- | --- | --- |
| H.264 + AAC | Vanleg direkte fil | Direkteavspeling, ingen omkoding | `JellyfinPlayerTest.exercise` med `EMBY` | Telefon og Google TV |
| DTS 5.1 | Emby si PlaybackInfo svarar DTS | Direkteavspeling via lokal FFmpeg, søk held posisjon | `dtsSurroundPlaysDirectlyFromBothServersAndSurvivesSeeking` | Google TV med receiver |
| E-AC3 5.1 | Emby si PlaybackInfo svarar E-AC3 | Same framdrift og søk utan ny forhandling | `eac3SurroundPlaysDirectlyFromBothServersAndSurvivesSeeking` | Google TV |
| TrueHD | Emby si PlaybackInfo svarar TrueHD | Lokal dekodar eller tydeleg kompatibilitetsfall | `trueHdSurroundPlaysDirectlyFromBothServersAndSurvivesSeeking` | Google TV |
| Ustøtta lyd | Renderer feilar etter start | Lokal FFmpeg før tenaromkoding; framdrift blir bevart | `localRecovery` i `JellyfinPlayerTest` | Telefon og Google TV |
| Tekstundertekst | Emby leverer VTT eller sidevogn sviktar | Tekstspor skiftar utan å starte videoen på nytt; feil undertekst stansar ikkje video | `subtitle`-testane i `JellyfinPlayerTest` | Norsk og engelsk teksting |
| Mellombels nettbrot | Éin til to transportfeil | Avgrensa retry, så konkret feil utan endelaus lasting | nettfeil-testane i `JellyfinPlayerTest` | Wi-Fi til mobilnett og tilbake |
| Avspeling etter konto-/profilbyte | Token eller aktiv profil byter under økt | Stopp utan å rapportere til feil konto | kontoendrings-testane i `JellyfinPlayerTest` | Emby barne- og vaksenprofil |

## DTS-regresjonstest

Det finst allereie ein konkret Android-test, ikkje berre ei liste i eit dokument:
`JellyfinPlayerTest.dtsSurroundPlaysDirectlyFromBothServersAndSurvivesSeeking`. Han køyrer same
syntetiske DTS-strøm mot både `ServiceKind.EMBY` og `ServiceKind.JELLYFIN`, verifiserer direkte
PlaybackInfo, dekodarutdata, søk og at søket ikkje gjer ei ny serverforhandling. Ein rein JVM-test
kan ikkje bevise Media3- og FFmpeg-dekoding, så denne skal stå som instrumentert test på den isolerte
testemulatoren — aldri på emulatoren med ekte kontoar.
