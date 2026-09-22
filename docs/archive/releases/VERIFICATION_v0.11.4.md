# Verifisering av Spole 0.11.4

## Kontrollar

- JVM-testar: 151 bestod, 0 feila.
- `assembleDebug`: bestod.
- `lintDebug`: bestod utan lint-feil.
- GitHub-feed: offentleg JSON utan innloggingsdata, parsast til separate anbefalingar.
- Seerr: blir brukt ved opning av anbefaling for live status, detaljar og førespurnad.
- Cache: anbefalingar blir lagra i dashboard-snapshoten saman med anna feed-data.

## Kjelde for lista

`recommendations.json` i det offentlege [spole-recommendations-repoet](https://github.com/oyvhov/spole-recommendations)
er den delte kjelda. Han kan endrast med nye TMDB-ID-ar,
medietype, tittel, postersti, sjanger og kort omtale. Appen byggjer ikkje denne lista frå ein lokal
server eller ein ny backend.

## Avgrensing

`emulator-5560` har `app.reelstack.debug` med demodata og ingen lagra ekte tenestetilkoplingar i
denne køyringa. Live Jellyfin, Emby og Seerr må kontrollerast etter innlogging i same appvariant.
