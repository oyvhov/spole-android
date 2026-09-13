# Filmvurderingar og tenestestatus

## Bibliotek

Filmar med vurdering frå serveren viser eit lite mørkt merke med stjerne og tal nedst på omslaget.
I listevising står vurderinga saman med informasjonen om filmen, slik at små omslag ikkje blir dekte.
Vurderinga følgjer **Vis vurderingar**. Manglande eller ugyldig poengsum gir ikkje noko merke.
Vurderinga kjem frå eksisterande `CommunityRating`-data; dette legg ikkje til nettverkskall eller bilde.

## Avspeling

«Opne i Jellyfin» og «Opne i Emby» er fjerna frå detaljsidene. Jellyfin-avspeling i Spole er framleis tilgjengeleg.
Kontrollen avdekte at `IntegratedPlaybackButton`, spelarmodellen og identitetskontrollen framleis berre
støttar Jellyfin. Denne UI-endringa legg ikkje til Emby-avspeling; ho krev eiga implementering og verifisering.

## Innstillingar → Tenester

Tenestesymbola er fjerna frå sidemenyen. TV, mobil og nettbrett brukar same tenesterad i innstillingane:
tenesteikon, namn, eventuell innlogga brukar og status med både symbol og tekst.

- Tilkopla: kontrollmerke.
- Tilkopla med avvik: varselmerke og forklaring på kva som ikkje kunne hentast.
- Feil: varselmerke og feilmelding.
- Under kontroll: klokke og ventetekst.
- Lagra opplysningar utan stadfesta status: «Ikkje kontrollert enno».
- Ikkje sett opp: handling for tilkopling.

Eit lagra innloggingsteikn åleine betyr ikkje at tenesta blir merkt OK. Trykk på rada opnar den
eksisterande tilkoplingsflyten. Kontoar og tenesteadresser blir ikkje endra av denne oppdateringa.

## Verifisering 13. september 2026

- `testDebugUnitTest`: 473/473 bestått. Nye testar dekkjer poengsum frå serverparseren,
  manglande/ugyldige vurderingar, og skiljet mellom lagra innlogging, OK, kontroll, avvik og feil.
- `assembleDebug`, `assembleDebugAndroidTest` og `lintDebug` bestod. Lint: 0 feil, 31 eksisterande åtvaringar.
- TV (isolert 5566): 9/9 Android-testar, 14,409 sekund.
- Mobil (isolert 5562): 9/9 Android-testar, 29,590 sekund.
- Visuell kontroll av vurderingsmerket og tenesteradene. Listevising og tenesteinnstillingar er
  kontrollerte med skriftstorleik 2.0; trykkmarkeringa blir klipt til dei runde hjørna.
- Testane dekkjer synleg/skjult vurdering, manglande vurdering, filmtrykk, status for tre tenester,
  fjerna eksterne knappar, eksisterande Jellyfin-spelknapp, bibliotekfeil/paginering og sidemenynavigering.
- Testane brukar syntetiske data. Ingen instrumentering eller sletting av data på dei innlogga profilane.

Ingen ny produksjons-APK er publisert som del av denne kodeendringa.
