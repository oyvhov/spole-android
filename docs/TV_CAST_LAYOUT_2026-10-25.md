# Medverkande på TV · 25.–26. oktober 2026

Filmfeltet brukte minst 76 prosent av skjermhøgda før rada med medverkande.
På ein vanleg TV-flate på 960 × 540 dp vart portretta kutta nedst, og namna
hamna utanfor biletet. Dette vart også observert med ekte innhald på den
lagra TV-profilen `Spole_GoogleTV_Test` / `emulator-5564`.

Filmar med medverkande bruker no den naturlege høgda på tittel, metadata,
omtale og handlingar. Personrada følgjer handlingane med éin samla avstand,
utan ekstra botnmarg i toppfeltet. Ved større skrift veks breidda på TV-personkorta
slik at namn får bryte mellom ord. Portretta beheld storleiken sin.

## Kontroll

`TvDetailsRobolectricTest` teiknar det faktiske detaljvindauget ved 960 × 540 dp
med ein lokal bitmap-logo, vurdering, sjangrar, to linjer omtale og medverkande.
Testane kontrollerer heile personkortet, inkludert namn og rolle, første fokus,
fjernkontroll ned/høgre/opp og faktisk skriftstorleik 2.0 i dialogvindauget.
Namna skal få plass utan ellipse eller linjebrot midt i eit ord.

Sluttkontrollen bestod: 2 av 2 nye TV-testar og 7 av 7 eksisterande
`MobileDetailsRobolectricTest`-testar, `lintDebug` med 0 feil og `assembleDebug`.
`git diff --check` er rein. Begge kontrollbileta er opna og kontrollerte visuelt.

Visuelle kontrollbilete blir skrivne til desse lokale, Git-ignorerte filene:

- `app/build/tv-cast-review/movie-cast.png`
- `app/build/tv-cast-review/movie-cast-font-2.png`

Etterkontrollen av den endra flata er gjort med syntetiske data i Robolectric.
Release- og einingskontroll for beta23 er ført i
[release-verifiseringa](VERIFICATION_v1.0.0-beta23.md).

Etter den offentlege beta23-oppdateringa vart same «One Battle After Another»-detalj
opna frå Jellyfin på den lagra TV-profilen 5564. Ved 1920 × 1080 var portretta,
namna og rollene for korta i visningsfeltet synlege heilt ned. Resten av
rollebesetninga ligg vidare i det vassrette rullefeltet. `DPAD_DOWN` fokuserte
det første personkortet. Skjermbiletet ligg lokalt i
`app/build/emulators/spole-beta23-cast-focus.png`.
