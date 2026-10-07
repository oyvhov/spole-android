# ADR 0002: Personvern, telemetri og nøkkellagring

## Status
Godkjent / Gjeldande

## Kontekst
Brukaren sine mediedata, personlege sjåarvanar, tenar-adresser og tilgangstoken er private og sensitive opplysningar. Mange medieklientar sender telemetri eller krev skykontoar.

## Avgjerd
1. **Inga telemetri eller sporing:** Spole samlar ikkje inn bruksdata, krasjloggar eller analysedata til nokon sentral tenar. Det finst ingen Spole-skykonto eller sporingstie-in.
2. **Direkte mediesamband:** Jellyfin, Emby og Seerr blir kontakta direkte. Valfri Emby Connect-innlogging, TMDB-kunst og GitHub-tilrådingar/oppdateringar er dokumenterte i `PRIVACY.md`.
3. **Trygg lokal nøkkellagring:** Tilgangstoken, sessionar og offline-katalog blir krypterte med AES-GCM og Android Keystore gjennom `EncryptedTokenStore`. Passord blir ikkje lagra. Metadata og mediefiler ligg i app-private lager; bilettoken blir sende som header og skal ikkje lagrast i metadata-adresser.
4. **Offentlege oppdateringskallar:** Sjekk etter app-oppdateringar skjer direkte mot GitHub si opne API utan token eller personidentifikatorar.

## Konsekvensar
- Tenaren eig tilgangsrettane; Spole avgrensar lokale data og handlingar til aktiv konto og profil.
- Dersom feilsøking krev loggar, må brukaren sjølv hente ut eller inspisere lokale feilmeldingar.
