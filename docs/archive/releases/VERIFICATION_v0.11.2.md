# Verifisering av Spole 0.11.2

## Bygg

- Pakke: `app.reelstack.debug`
- Versjonskode: `24`
- Versjonsnamn: `0.11.2-debug`
- APK: `Spole-v0.11.2-debug.apk`
- Storleik: `28 457 001` byte.
- SHA-256: `2fe67d50715ac439249c8d0bae0aa1f8aae5a7e5c6c50dabf6b85c314cdc4a6b`.
- Signeringssertifikat: same debug-sertifikat som v0.11.1.

## Kontrollar

- 149 einingstestar bestod.
- 4 av 4 målretta Android-testar i `CalendarAndSheetTest` bestod.
- Android lint: 0 feil.
- Manuell kontroll på `emulator-5560`: same første film vart opna og kontrollert på fleire tidspunkt i
  innflyginga. Detaljarket held same ytre geometri etter opning; biletet startar ikkje ein ekstra
  kryssfading oppå botnark-animasjonen.

## Avgrensing

Review-emulatoren hadde lagra Spole-demodata, ikkje verifiserte live-innloggingar mot Jellyfin, Emby eller
Seerr. Det er derfor ikkje gjort påstand om ende-til-ende-tenestedata i denne releasekontrollen.
