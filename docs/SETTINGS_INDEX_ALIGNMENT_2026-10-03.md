# Samordna innstillingsoversikt

Nedlastingsrada brukte `SettingsActionRow`, medan resten av mobiloversikta brukte `SettingsCategoryCard`. Dei hadde ulik ikonflate, avstand til teksten og pilfarge. Oversikta deler no `SettingsOverviewRow`, med 48 dp ikonflate, same innrykk, typografi og nøytral pil. Radene kan vekse med teksten.

Nedlastingar ligg etter varsel og før Om appen, både på telefon og i nettbrettmenyen. TV viser framleis ikkje nedlastingar; barnemodus får heller inga nedlastingsrad.

`MobileSettingsTest` har kontrollar av venstrekanten for ikon, tekst og piler i alle sju radene, rekkjefølgja og opning av nedlastingssida, med normal og stor skrift.

Begge testane bestod på isolert emulator-5562 ved systemskrift 1.0 og 2.0 (fire køyringar). Lokale skjermbilde er gjennomgåtte. Ingen kontroll på ein fysisk telefon eller nettbrett i denne runden.
