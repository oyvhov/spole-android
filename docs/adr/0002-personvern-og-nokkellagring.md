# ADR 0002: Personvern, telemetri og nøkkellagring

## Status
Godkjent / Gjeldande

## Kontekst
Brukaren sine mediedata, personlege sjåarvanar, tenar-adresser og tilgangstoken er private og sensitive opplysningar. Mange medieklientar sender telemetri eller krev skykontoar.

## Avgjerd
1. **Inga telemetri eller sporing:** Spole samlar ikkje inn bruksdata, krasjloggar eller analysedata til nokon sentral tenar. Det finst ingen Spole-skykonto eller sporingstie-in.
2. **Direkte samband:** Kommunikasjon skjer utelukkande direkte mellom klienten og dei lokale eller private tenarane brukaren sjølv konfigurerer (Jellyfin, Emby, Seerr, Radarr, Sonarr).
3. **Trygg lokal nøkkellagring:** Tilgangstoken og passord blir lagra kryptert lokalt på eininga via Android Keystore og `EncryptedSharedPreferences` (`EncryptedTokenStore`). Klartekstpassord blir aldri lagra permanent etter fullført innlogging.
4. **Offentlege oppdateringskallar:** Sjekk etter app-oppdateringar skjer direkte mot GitHub si opne API utan token eller personidentifikatorar.

## Konsekvensar
- Fullt personvern for sluttbrukaren i eige heimenettverk.
- Dersom feilsøking krev loggar, må brukaren sjølv hente ut eller inspisere lokale feilmeldingar.
