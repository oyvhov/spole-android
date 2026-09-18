# Spole 0.17.0-beta26

To rettingar på beta25.

## Retta

- **Barnemodus viste titlar kontoen ikkje har bibliotek for.** Barneframsida browsa kvar einaste
  biblioteksvising kontoen har, også samlingsvisinga («boxsets»). Ei flat listing av den visinga
  returnerer medlemmane i samlingane frå heile tenaren, og slik hamna vaksne titlar i barneprofilen.
  Det var ein feil i Spole, ikkje ei rettigheit på tenaren.

  Framsida hentar no berre film- og seriebiblioteka, og spør rekursivt etter filmar og seriar i
  kvart av dei. På denne tenaren gir det 209 barnefilmar og 63 barneseriar, og ingenting anna.

- **Oppdateringsdialogen var trong på telefon.** To knappar med tekst på same linje, ein
  lukkeknapp med tekst på tittellinja og 28 dp marg fekk ikkje plass på ein telefonskjerm. På smal
  skjerm står knappane no under kvarandre i full breidd, lukkeknappen er eit ikon, og margane er
  smalare. Fjernsynet er uendra.

## Merk

Kva eit barn ser, blir framleis avgjort av biblioteka du gir kontoen i Emby eller Jellyfin. Spole
filtrerer ingen titlar lokalt — det er kva for **bibliotek** som blir spurde som er retta her.
