# Spole 0.17.0-beta38

Denne testutgåva rettar ein alvorleg avspelingsfeil der ei manglande eller øydelagd ekstern
undertekst kunne stoppe ein video som allereie hadde starta.

## Retta

- Undertekstfeil blir no behandla som ein valfri tekstsporfeil, ikkje som ein videofeil.
- Spelaren slår av berre det feila undertekstsporet, held same videoposisjon og held fram avspelinga.
- Vanlege nettverks-, lyd- og videofeil brukar framleis den eksisterande feilhandsaminga.
- Appen førehandslastar ikkje lenger eit tilfeldig tekstspor når undertekst er slått av.

Oppdateringa bevarer innloggingar, mediedata og brukarval. Ho er ei testutgåve;
«Testutgåver» må vere på i appoppdateringar for å finne henne.
