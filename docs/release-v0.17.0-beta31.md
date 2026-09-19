# Spole 0.17.0-beta31

Tre små rettingar på aktivitetssida, og ein plan.

## Endra

- **Aktivitet opnar på «På vei».** Det du ventar på er grunnen til at du opnar sida; det som alt har
  kome er ikkje nytt. Har du ingenting på veg, opnar sida på «Alle» i staden — ei tom liste ved
  opning les som ei øydelagd side, ikkje som eit tomt filter. Ditt eige val overstyrer alltid.
- **«Se forespørselshistorikk» heiter no «Historikk».** Knappen står på aktivitetssida og har ein
  chevron, så substantivet er nok. Den lange etiketten var også sjølve breiddeproblemet.

## Retta

- **Historikk-knappen overlappa filterknappane på telefon.** Filterrada og historikkrada låg heilt
  inntil kvarandre, så dei avrunda hjørna rørte ved kvarandre. Det er 8 dp mellom dei no.

## Dokumentasjon

- `docs/GLOBALT_SOK_PLAN.md`: arbeidsplan for globalt søk, med akseptansekrav per funksjon. Han
  peikar mellom anna på at søkeinngangen i dag forsvinn heilt over 640 dp, så nettbrett og TV står
  utan søk med mindre ein opnar Oppdag manuelt.
