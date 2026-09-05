## Spole 0.11.2 · Stabil detalj-popup

- Gjer detalj-popupen stabil frå første frame. Popupen byter ikkje mellom ståande og liggjande ramme
  etter at biletet er dekoda; medietypen avgjer ramma før visinga startar.
- Slår av ekstra biletkryssfading i film-, serie- og episode-popupen. Botnark-animasjonen får dermed
  ikkje konkurranse frå ein ny bileteffekt når første film blir opna.
- Bevarar plakatformatet for film og thumbnail-formatet for episodar, utan at ytre popup-høgd blir
  målt på nytt under opninga.

Dette er ein debug-signert APK for direkte installasjon over Spole 0.11.1. Same app-ID og signering er
bevarte, slik at eksisterande tenesteinnloggingar og lokal data kan liggje att.

### Verifisering

- 149 einingstestar bestod.
- 4 målretta Android-testar i `CalendarAndSheetTest` bestod, inkludert stabil popup ved asynkron omtale.
- Android lint fullførte med 0 feil.
- APK-storleik: `28 457 001` byte.
- SHA-256: `2fe67d50715ac439249c8d0bae0aa1f8aae5a7e5c6c50dabf6b85c314cdc4a6b`.
- Førstepopupen vart opna fleire gonger på den vanlege `emulator-5560`-review-emulatoren med installasjon
  over eksisterande data. Denne emulatoren hadde demodata i denne kontrollen; ingen ekte tenestekonto blir
  påstått verifisert her.
