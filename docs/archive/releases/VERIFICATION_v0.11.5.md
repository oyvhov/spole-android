# Verifisering av Spole 0.11.5

- Offentleg feed: `https://raw.githubusercontent.com/oyvhov/spole-recommendations/main/recommendations.json`
- Feed kan hentast utan GitHub-token.
- `testDebugUnitTest`, `assembleDebug` og `lintDebug` skal bestå i sluttbygget.
- Seerr er ikkje kjelde for Home-anbefalingane; han blir brukt ved opning for live status og request.

Live tenestedata er ikkje verifisert på emulatoren i denne køyringa fordi han har demodata utan lagra
Jellyfin-, Emby- eller Seerr-konto.
