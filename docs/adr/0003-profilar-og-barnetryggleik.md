# ADR 0003: Profilar, isolasjon og barnetryggleik

## Status
Godkjent / Gjeldande (oppgradert i beta33)

## Kontekst
Ein familie deler ofte éin TV i stova. Barn skal ha eit enkelt, trygt og morosamt grensesnitt som berre viser deira eigne bibliotek, medan vakseninnhald og innstillingar må vernast mot utilsikta tilgang eller endring.

## Avgjerd
1. **Full dataisolasjon mellom profilar:**
   - Kvar profil (hovudkonto og barnekontoar) har sine eigne, heilt separate tilgangstoken og innstillingar i `ConnectionRepository`.
   - Ved profilbyte blir alt minne og cache for førre profil nullstilt momentant, slik at vakseninnhald aldri ligg att i minnet.
2. **Kryptografisk herda PIN med PBKDF2-HMAC-SHA256:**
   - Retur frå barnemodus krev ein 4-sifra foreldre-PIN.
   - For å motstå rå makt (brute-force) på dei berre 10 000 moglege kombinasjonane, nyttar Spole `PBKDF2WithHmacSHA256` med 10 000 iterasjonar og unikt 16-byte salt per lagring.
   - Versjonert format gjer at eldre SHA-256-kodar automatisk og transparent blir migrerte ved første godkjende innlogging.
3. **Feil-lukka (fail-closed) tilgangskontroll:**
   - Dersom PIN-data i lokal lagring er korrupte, uleselege eller manipulert, gir systemet ALDRI tilgang. Det vert returnert `PinResult.Corrupted`.
   - Gjenoppretting krev passordet til den primære vaksenkontoen mot medietenaren.
4. **Progressiv utestenging og klokketamper-vern:**
   - 3 feila forsøk gjev 5 sekunders sperre, 4 gjev 15 sekund, og 5 eller fleire gjev 60 sekund.
   - Låsetilstanden overlever omstart av appen og motstår manipulering av systemklokka bakover.
5. **Autonomi for barn:**
   - Tilpassing av fargetema, landskap og stjerner («Mi verd») krev ikkje PIN, slik at barnet fritt kan leike med utsjånaden utan å måtte hente ein vaksen.

## Konsekvensar
- Høg tryggleik for barnefamiliar mot omgåing eller lekasje av vakseninnhald.
- Triveleg og fri brukaroppleving for barnet innanfor dei rammene foreldra har sett.
