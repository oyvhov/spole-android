# Spole 0.17.0-beta24

Barnemodus får sitt eige skal. Der dei førre utgåvene gav barnet ein eigen konto inne i den vaksne
appen, er dette ei eiga flate: ingen siderad, ingen faner, ingen detaljpanel.

## Nytt

- **Barneskalet.** Eit eige skal ved sida av vaksenappen. Maks tre nivå: framside, episodar, spelar.
- **Framsida for barn.** To rader, ikkje fleire. «Hald fram» med brei kunst og framdriftslinje på
  kunstflata si eiga botnkant, og «Seriane dine» som rutenett. Ingen tilrådingar og ingen kalender.
- **Episoderutenett.** Store nummer og stillbilete. Sette episodar er dempa, og sesongveljaren viser
  seg berre når serien faktisk har fleire sesongar.
- **Barnespelaren.** Tre kontrollar: spol attende, spel/pause, spol fram. Ingen meny for lydspor,
  undertekst eller kvalitet — dei er sette av forelderen. Opning i andre appar er av i barnemodus.
- **Autospel med grense.** Neste episode startar av seg sjølv maksimalt tre gonger på rad, så ventar
  tilbodet på eit trykk.
- **Verdsromlaget.** Ein roleg stjernehimmel bak barneframsida. Han står stille når systemanimasjon
  er slått av, og har inga skjermlesarbeskriving.
- **Nytt profilbyte.** Profilane fell ned frå profilbiletet i hjørnet i staden for å kome som eit
  popupark nedanfrå, og rada viser namnet på personen med rolla under.

## Retta

- **Emby-barneprofil gjekk til feil tenar.** Med både Jellyfin og Emby kopla til valde appen den
  første medietenaren med teikn, så eit Emby-brukarnamn blei autentisert mot Jellyfin og feila som
  feil passord. Skjemaet spør no kva for ein tenar kontoen høyrer til.
- Tenarar som ikkje viser offentlege kontoar utanfrå — som Emby over ei ekstern adresse — får no ei
  forklaring i staden for ei tom liste.
- «Barneprofilar» var ei seksjonsoverskrift brukt som radetikett, og hovudprofilen viste namnet på
  tenartilkoplinga i staden for på personen.

## Verifisert

Køyrt på Google TV med ekte Emby-konto: profilbyte, PIN-utgang, barneskalet, episoderutenettet og
avspeling frå både framsida og episodelista.
