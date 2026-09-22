# Spole 0.16.0-alpha19

## Bibliotek er ei side om bibliotek

Rota i Bibliotek var fire mappefliser på svart. No får kvart bibliotek ei overskrift du kan trykke
og ei rad under med det det held — det du er midt i først, deretter det nyaste — så du ser kva som
er der inne før du opnar det.

## Meir innhald får plass

- Fyrste rada på Oppdag, Aktivitet og førespurnadshistorikken fekk plass til titlane sine. Fem
  omslag i staden for fire, og toppen av neste rad er synleg.
- Biblioteksida si overskrift og dei tre knappane står på same linje på TV.
- Innstillingar viser verdien til høgre på same linje som valet. Seks tenester får plass der tre
  gjorde før.

## Episodesider

Ei episodeside viser sesongen sin: sesongstripa rullar til rett sesong, rada du er på er merkt, og
omtale og medverkande har flytta under biletet der spalta stø tom.

## Mobil

Fem ting låg feil og er retta: knappar som stod utanfor tekstspalta, to ulike overskriftsstilar på
Innstillingar, «Vising / Mine» som stod på kvar sin kant av skjermen, filterknappar der berre den
fyrste av tre fekk plass, og «Varsel og oppdatering» der tre kort låg jamma hjørne mot hjørne.

## Under panseret

- Appen byggjer mindre opp att seg sjølv. Ei oppdatering som hentar dei same dataa kostar no
  ingenting i staden for å teikne kvart kort om att.
- media3 1.11.1, Room 2.8.5, kotlinx-serialization 1.11.0.
- Ein episode utan namn heiter «Episode 1», ikkje «1 · Episode 1». Vurderingsstjerna peikar på
  vurderinga i staden for på årstalet.

Testutgåve, bygg 62. Installer over den eksisterande Spole-appen for å behalde kontoar og
innstillingar.

## Kva som er kontrollert

445 einingstestar, 0 feil. Lint 0 feil. Alle sidene på TV og på telefon er sette med ekte kontoar
på dei lagra emulatorprofilane, og avspeling er prøvd mot ei ekte fil frå Jellyfin.

**Ikkje kontrollert:** dei 58 instrumenterte testklassene er ikkje køyrde — dei krev dei isolerte
profilane, og verten hadde ikkje minne til fleire emulatorar. Emby er berre prøvd mot ein skripta
transport. Fysisk TV og fysisk telefon står att.
