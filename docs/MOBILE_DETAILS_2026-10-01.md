# Detaljvising på mobil · 1. oktober 2026

Brukaren rapporterte at episodeomtalen og det store biletet skyv «Spel av» heilt ned
mot systemfeltet. Den generiske overskrifta «Detaljar» tok dessutan plass før sjølve
tittelen. Referansebiletet har iPhone-systemfelt; endringane her gjeld Android-appen.
Brukaren presiserte deretter Infuse som designreferanse og at tilpassinga skal
gjelde mobil. Den nye telefonvisinga bruker kunst og identitet meir samla.

## Oppsett

- Titteldetaljar har ei kompakt verktøylinje med fast lukkeknapp. Kalender-retur er
  bevart. Avspelingsdetaljar og dei andre popuparkene held på eigne overskrifter.
- Telefonarket får 96 % av den trygge vindaugshøgda. Den faste høgda er framleis
  uavhengig av bilete og metadata. TV og nettbrett held på høgderegelen sin.
- Bibliotekstitlar har bakgrunnskunst heilt ut til kantane og ein mørk overgang
  ned mot leseflata. Tittel eller ekte serielogo ligg på kunsten, med episodetittel
  og sentrerte fakta, sjangrar og vurderingar under. Kjeldeikon blir ikkje viste
  som del av tittelen.
- Lukkeknappen ligg fast over kunsten. Han flyttar seg ikkje når innhaldet blir
  rulla. Kalender-retur er bevart på same plass.
- Avslått detaljbakgrunn og høg kontrast bevarer den enkle kunst-/tekstvisinga.
  Klassisk episodekunst er framleis høgst 180 dp; filmplakatar får sitt eige format.
- «Spel av» / «Hald fram» ligg i eit eige fast felt under lesekolonnen. Kolonnen får
  mindre tilgjengeleg høgd, slik at omtale, sporval og episodar kan rullast heilt
  fram utan å hamne bak knappen.
- `StableSheetDialog` eig framleis navigasjonsinnfellinga, den faste popuphøgda og
  opnings-/lukkeanimasjonen. Avspelingsfeltet legg til 12 dp luft over og under.
- Mobilknappen kan vekse og bryte tekst ved stor skrift. Han er deaktivert til
  opninga og detaljinnlastinga er ferdige.
- Kjelde, innlogging, konkret medie-ID og neste episode avgjer om feltet blir vist.
  Ein serie utan ein konkret episode og titlar utan medieteneste får ikkje eit
  tomt avspelingsfelt.
- Favoritt, sett, nedlasting, trailer og serielenkje ligg i innhaldet. Lydspor,
  tekstspor og versjon blir framleis sende frå dei same vala til spelaren.

TV brukar framleis den eksisterande filmatiske detaljsida med fjernkontrollfokus.
Nettbrett får ikkje den nye sentrerte telefonhelten.

## Verifisering

- `testDebugUnitTest`: **810/810 bestått**, inkludert sju nye mobil-UI-testar.
- `assembleDebug` og `assembleDebugAndroidTest`: bestått.
- `lintDebug`: bestått, ingen lint-feil.
- Dei sju nye Robolectric-testane brukar Android 34, innfødd grafikk og eit
  telefonvindauge på 360 × 780 dp. Dei dekker rulling/utviding, skriftstorleik 2.0,
  innlasting, lang filmtittel, manglande innlogging og serie utan avspelbar episode.
- Tre etterfølgjande opningar er kontrollerte i 65 teikna rammer kvar. Metadata
  kjem i ramme 0, 8 og 36. Flata glir opp utan oversving eller retur, og den målte
  høgda er fast. Kvar dialog må vere avslutta før neste blir opna.
  Robolectric sine semantikkmål tek ikkje med render-laget si flytting; testen
  måler derfor den ugjennomsiktige flata i dei faktisk teikna bileta.
- Skriftstorleik 2.0 blir sett i Android-ressursane og stadfesta frå den faktiske
  tekstlayouten i dialogen. Eit `LocalDensity`-overstyrt foreldreledd åleine nådde
  ikkje dialogvindauget og gav same skrift i begge bileta.
- Ei syntetisk navigasjonsinnfelling på 48 px stadfestar at heile knappen og
  lesekolonnen ligg over gestfeltet. Utviding og rulling flyttar ikkje knappen.
- TMDB-merket hadde ei fast tekstboks som klipte merkenamnet ved skriftstorleik
  2.0. Boksen veks no med teksten. Begge skriftstorleikane kontrollerer at heile
  tekstlinja får plass i breidd og høgd.
- Visuell kontroll av begge skriftstorleikane er gjennomført med lokale
  testbilete i `app/build/mobile-details-review/`. Bileta brukar reservekunst og
  syntetisk innhald; dei er ikkje skjermbilete frå den innlogga installasjonen.

Den første instrumenteringsstarten på den delte 5562-profilen vart avbroten då
anna arbeid var aktivt. Skjermstorleik og tettleik vart sette tilbake. Ingen
instrumenteringssuite vart fullført i den første mobilgjennomgangen, og dei lagra
produksjonskontoane vart ikkje brukte eller endra. Fysisk iPhone er ikkje testa.
Endringane er publiserte i
[Spole 0.18.0-beta12](https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta12),
kode 132. Oppdateringa frå beta11 gjennom appen og den nye detaljvisinga med
normal lukking er kontrollerte på mobilprofilen. Denne profilen har lagra demo,
så den faste avspelingsknappen er verifisert gjennom dei syntetiske UI-testane.
Release-gjennomgangen stadfesta 15/16 utvalde Android-testar på den isolerte
TV-profilen; ein tastaturtest har den same tidsfristfeilen som i beta11.
Sjå [full release-verifisering](VERIFICATION_v0.18.0-beta12.md).

Designreferansen er Infuse-biletet som brukaren la ved 1. oktober: kunst bak
identiteten, kompakte metadata og samla handlingar. Spole bevarer sin eigen aksent,
funksjonane sine og den faste avspelingsknappen.
