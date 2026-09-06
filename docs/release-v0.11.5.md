## Spole 0.11.5 · Offentleg anbefalingsfeed

- “Anbefalingar” blir henta frå det offentlege [spole-recommendations-repoet](https://github.com/oyvhov/spole-recommendations),
  ikkje frå Seerr-trending og ikkje frå det private app-repoet.
- Lista kan redigerast på GitHub utan ny APK.
- Seerr blir framleis brukt for live detaljar, status og førespurnader når ein opnar ei anbefaling.

### Verifisering

- 151 einingstestar bestod før endpoint-justeringa; den siste endringa er berre URL-en til den
  offentlege, verifiserte feeden og blir bygd og lint-kontrollert på nytt.
- Den offentlege JSON-fila svarar anonymt med HTTP 200.
- Emulatoren har demodata og ikkje ekte tenestekontoar i denne køyringa.
