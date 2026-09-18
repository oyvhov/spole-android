# Reserveforsøk ved feil i mediebehaldaren

## Feilen

Stats for Nerds på Pixel viste `ERROR_CODE_PARSING_CONTAINER_MALFORMED → STOPPED`
før videoformatet var lese. Den gamle reservehandteringa fanga ikkje denne koden.
`PARSING_CONTAINER_UNSUPPORTED` gjekk til lydomkoding, sjølv om ein behaldarfeil
ikkje i seg sjølv viser at lyddekodaren har feila.

## Endring

- Begge behaldarfeilane utløyser eitt `REMUX`-forsøk når ein urørt originalstraum
  feilar. Ei allereie ompakka HLS-avspeling får ikkje nye behaldarforsøk.
- Ny PlaybackInfo-førespurnad slår av direkte filavspeling og tillèt kopiering
  av både video og lyd. Appen brukar serveren si HLS-adresse, aldri den same
  direkte fila på nytt dersom serveren manglar eit alternativ.
- Ompakkingsprofilen tillèt AAC, MP3, AC3 og EAC3 når eininga melder støtte,
  utan å endre den vanlege lydomkodingsprofilen. DTS/TrueHD blir ikkje lagt
  til HLS-profilen gjennom denne endringa.
- Kjeldeversjon, lyd-/tekstval, posisjon og spel/pause blir bevarte.
- Ein seinare reell dekodarfeil kan framleis utløyse den eksisterande lokale
  lyddekodaren eller lyd-/videoreserven. Ein ny behaldarfeil stoppar utan løkke.
- Nettverksforsøk, tidsgrenser og vanleg direkte avspeling er uendra.

Serveren avgjer kva som faktisk kan kopierast ut frå kodekar, kvalitet, tekst
og brukarrettar. `AllowVideoStreamCopy=true` er ikkje ein garanti for at alle
format kan ompakkast utan omkoding. Stats viser framleis serveren sin faktiske
avspelingsmåte, ikkje berre det appen bad om.

## Verifisering

Einingstestar dekkjer feilklassifisering, avgrensa reserveforsøk, profil og
PlaybackInfo-førespurnad for både Emby og Jellyfin, og manglande HLS-alternativ.
Android-testar injiserer den konkrete Media3-feilkoden i den verkelege modellen
og kontrollerer overgang til faktisk HLS-dekoding, posisjon, spor, pause og stopp
ved gjenteken feil.

- JVM: 634/634 bestått, ingen feil eller hoppa-over-testar.
- Android: 8/8 bestått på den isolerte `emulator-5562` (77,271 sekund).
  Dei to nye testane køyrer kvar for både Emby og Jellyfin. Dei andre dekkjer
  nettverksreserve, lokal EAC3-reserve, HLS med tekst, direkte video med søking
  og rotasjon, kortvarig fokustap og sporbyte medan avspelinga er pausa.
- Debug-app og test-APK er bygde og installerte på testemulatoren. Ingen ekte
  kontoar eller review-profilar vart endra.
- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`:
  `BUILD SUCCESSFUL` etter 6 minutt og 51 sekund.
- Lint: 0 feil, 93 åtvaringar (same tal som før endringa). `git diff --check`
  er rein.

Originalfila til brukaren er ikkje tilgjengeleg i test-fixturen. Desse testane
stadfestar reserveflyten, ikkje at akkurat den fila kan lesast av serveren.
Ingen ny GitHub-release er del av denne kodeendringa.
