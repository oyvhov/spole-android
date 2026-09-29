# Søk, filter og avspelingsval – andre visuelle runde

Vidareføring av dei godkjende, varsame detaljendringane. Hovudsidebaren er uendra.

## Utforming

- Globalt søk har eit ope skrivefelt og historikk som lesbare rader med klokkeikon.
- Bibliotektreff i globalt søk blir grupperte etter medietype innanfor bibliotekseksjonen.
  Kvar gruppe har symbol og tal på dei returnerte treffa. Kjelder og rettar er uendra.
- Oppdag bruker tekstfaner for medietype og eit lettare søkefelt.
- Biblioteket viser aktive filter som ei tekstlinje. Sett-status og oppløysing ligg
  under Filter, slik at verktøylina får færre knappar. Alle vala er framleis tilgjengelege.
- Lyd- og tekstval i detaljane er heile, opne rader. Avspelaren og detaljmenyane
  markerer det valde alternativet med ei hake. Kvalitetsvala får korte forklaringar.
- Søk, tomt bibliotek og tom nedlastingsliste deler ei roleg tomvising med eit
  frittståande symbol. Eit filtrert, tomt bibliotek tilbyr nullstilling.
- Lasteplasshaldarane for søket har plakatformat og eigne tekstlinjer, med rolegare
  lysrørsle. Valet for reduserte rørsler er framleis respektert.

Søket får ikkje nye datakjelder i denne runden. Personsøket og den uferdige globale
TV-søkeinngangen i GS-planen er ikkje bygde. Dei eksisterande søkjene og
avspelingsvala blir brukte vidare. Ingen release er publisert.

Skjermbilete blir haldne lokalt under den ignorerte `app/build`-mappa og viste til
brukaren for godkjenning. Ingen nye automatiske testar er lagde til eller køyrde.

## Lokal gjennomgang

- Endeleg `assembleRelease` fullført på 3 minutt og 21 sekund.
- APK SHA-256: `d2e7907c8d4f205f158f7e416d8389df83a624ade95b05eb2bf5b581f14c39fc`.
- Produksjonssignaturen er kontrollert mot den eksisterande Spole-nøkkelen.
- Installert med `-r` på 5564 og 5560, utan å slette kontoar eller appdata.
- TV: filtermenyen med korte etikettar og kvalitetsmenyen er visuelt gjennomgåtte
  med ekte Jellyfin-innhald. Avspelinga vart pausa ved ni sekund for skjermbiletet;
  kvalitetsvalet vart ikkje endra.
- Mobil: søk med treff og utan treff er viste med den eksisterande engelske
  demoprofilen. Dette verifiserer ikkje gruppering av ekte bibliotektreff.
- Android sin Bluetooth-prosess krasja gjentekne gonger. Ein manglande
  `BLUETOOTH_CONNECT`-rett vart gjeven til systempakken, men ein ny kontaktrettsfeil
  stod att. Bluetooth vart difor mellombels slått av i mobilemulatoren.
  Ingen Spole-rettar eller appdata vart endra i denne feilsøkinga.
- Endelege bilete: `approval-filters-tv.png`, `approval-quality-tv.png`,
  `approval-search-phone.png` og `approval-empty-phone.png` under `app/build`.
- `git diff --check` er rein. Dei to hovudnavigasjonsfilene har ingen diff.
- Lasteanimasjon, alle avspelingsval og stor skrift er ikkje gjennomgåtte på nytt
  i denne runden. Ingen automatisk testpakke er køyrd, og ingen release er publisert.
