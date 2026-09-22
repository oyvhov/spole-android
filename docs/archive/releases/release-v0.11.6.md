## Spole 0.11.6 · Stabil anbefalingsstatus og detaljpopup

### Endringar

- Anbefalingar frå den offentlege katalogen blir no slått opp mot Seerr under refresh. Statusen på
  Home-kortet er dermed basert på live bibliotek-/requestdata, ikkje berre på katalogfila.
- Detaljarket har fast standardstorleik. Omtale, sjangrar, fakta og eventuell tagline får reserverte
  plassar, medan eit roleg skeleton viser kva som blir henta.
- Omtale er avgrensa til fire linjer i arket; resten kan lesast ved å rulle i arket utan at sjølve
  popupen skiftar storleik.

### Avgrensing

Statusoppslaget krev ei aktiv Seerr-tilkopling for brukaren. Utan Seerr kan appen vise anbefalinga,
men kan ikkje vite om ho ligg i eit privat Jellyfin-/Emby-bibliotek før ein faktisk har ein teneste
som kan svare på det.
