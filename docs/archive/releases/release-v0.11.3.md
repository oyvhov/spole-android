## Spole 0.11.3 · Release-baserte Home-rader

- “Anbefalingar” er ei eiga, valfri Home-rad frå Seerr.
- “Nyleg tilgjengeleg” brukar faktiske release-datoar frå Radarr og Sonarr, ikkje `latest added` frå
  Jellyfin eller Emby.
- Release-rada viser berre dei siste 28 dagane, sorterer nyaste først og tek berre med Radarr sine
  digitale/fysiske heimeutgivingar. Filmar med berre kinodato blir ikkje viste.
- Jellyfin og Emby held fram med eigne, uavhengige rader for sist lagde filmar og episodar.
- Nye Home-val blir migrerte inn i Innstillingar på eksisterande installasjonar, men kan slåast av
  kvar for seg.

### Verifisering

- JVM-testar og Android lint bestod.
- Manuell kontroll på `emulator-5560` viste Anbefalingar, release-dato, overlay-kort og detaljpopup
  utan krasj. Emulatoren hadde demodata i denne kontrollen; ingen ekte konto blir påstått verifisert.
