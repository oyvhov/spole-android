## Spole 0.11.4 · Felles anbefalingar frå GitHub

- Rettar “Anbefalingar” frå Seerr-trending til ei felles, kuratert liste i
  [`spole-recommendations`](https://github.com/oyvhov/spole-recommendations).
- Lista kan oppdaterast på GitHub utan at appen må byggjast eller publiserast på nytt.
- Appen cache-ar lista lokalt. Seerr blir berre brukt for live status, detaljar og førespurnader når
  brukaren opnar ein tittel.
- Oppdag-søket og den private Seerr-aktiviteten er framleis separate frå den felles lista.

### Verifisering

- 151 einingstestar bestod.
- APK-bygg og Android lint bestod.
- Parseren filtrerer bort ugyldige medietypar frå GitHub-lista.
- Manuell emulatorverifisering held fram på `emulator-5560`. Emulatoren har demodata og ikkje ekte
  tenestekontoar i denne køyringa.
