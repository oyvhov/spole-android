# Spole 0.17.0-beta40

Denne testutgåva fokuserer på barnemodus på mobil og nettbrett, og på meir presis lokal
fortsetjingsstatus under avspeling. Ho kan installerast over tidlegare Spole-betaer med same
signering.

## Nytt og retta

- Barneheaderen viser «Hei, [brukar]» utan ekstra hjelpetekst, med betre toppmarg for profilbilete
  og tekst.
- Filmcover i barnemodus får responsiv kolonnestorleik på mobil og nettbrett.
- Framsida viser korte «Siste i [bibliotek]»-rader. Kvar bibliotek opnar no ei eiga barneside med
  filter for alle, filmar, seriar, ikkje sette og favorittar.
- Biblioteksknappane kan vise namnet under bilete i staden for over biletet.
- Foreldra kan velje om retur frå barnemodus til vaksenmodus skal krevje PIN. Når PIN er av, går
  profilbytet direkte.
- Feed-resultat frå Jellyfin og Emby held på bibliotek-ID-en, slik at siste-innhald blir vist i
  rett bibliotekrad.
- Lokal fortsetjingsstatus blir ikkje lagra ved første avspelingssignal eller posisjon null. Etter
  reell avspeling blir framdrift framleis rapportert til Jellyfin/Emby som før.

Dette er ei prerelease-utgåve. «Testutgåver» må vere på i appoppdateringane for at ho skal bli
funnen automatisk.
