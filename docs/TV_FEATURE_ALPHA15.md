# TV-toppfelt — alpha15

Toppfeltet brukar same 32 dp toppmarg som resten av heimesida, mot dei tidlegare 16 dp. Titteltypografien er redusert frå 38/43 sp til 32/36 sp. Den kvite handlingsknappen er erstatta med ei svak, transparent flate og teksten «Sjå meir». Tenestelogo med tenestenamn erstattar biblioteksetninga. Profilringen over kunsten er redusert frå 65 % til 18 % kvit dekning; synleg fjernkontrollfokus er bevart.

Utvalet kjem berre frå dei eksisterande, tillatne serieradene. Serverparseren mappar episodetitlane til serienamnet; normalisert serienamn blir brukt til å velje opptil tre ulike seriar, også på tvers av Jellyfin og Emby. Manglande kunst og skjulte rader blir filtrerte vekk. Dersom biblioteket berre leverer éin eller to ulike seriar, blir dei ikkje dupliserte for å fylle tre plassar.

Dei maksimalt tre bileta blir lasta i same storleik frå starten og haldne klare medan feltet er komponert. Overgangen byter gjennomsiktigheita mellom desse laga; han opprettar ikkje eit nytt bildekall ved kvart skifte.

Kunsten og metadata tonar over i neste tittel over 800 ms, etter åtte sekund. Handlingsknappen og profilen blir verande i same komposisjon. Tekstfelta reserverer same linjetal slik at høgda ikkje endrar seg mellom titlane. Knappen opnar den viste tittelen. Rotasjonen stoppar når feltet har fokus, når detaljar er opne, når feltet ikkje er synleg, når appen ikkje er resumed og når systemanimasjonar er slått av. Det blir ikkje gjort ekstra tenestekall for rotasjonen.
