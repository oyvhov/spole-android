# Spole 0.18.0-beta6

Denne testutgåva rettar den stille nedlastingshandlinga på detaljsider.

## Rettar

- Nedlastingssymbolet viser straks at Spole klargjer den trygge serverforhandlinga, og kan ikkje
  trykkjast fleire gonger medan ho går.
- Når ei fil ikkje kan lastast ned, står den konkrete årsaka att på same detaljside i staden for at
  handlinga ser ut til å ikkje gjere noko.
- Etter at ei direkte-kompatibel fil er lagd i kø, opnar Spole **Nedlastingar** automatisk slik at
  framdrifta er synleg med ein gong.
- Seriesider forklarer når episodane framleis lastar, i staden for å ha eit stille deaktivert
  nedlastingssymbol.

## Avgrensingar

- Berre komplette Jellyfin-/Emby-filer som kan spelast direkte lokalt kan lastast ned. Livestream,
  DRM, plateavtrykk og media som krev omkoding blir ikkje lagde i kø.
- Nedlasting er framleis avgrensa til vaksne profilar på telefon og nettbrett. TV og barnemodus
  viser ikkje handlinga.
