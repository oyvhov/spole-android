# Kalender, språk og stor skrift

9. september 2026 · kjeldearbeid etter 0.16.0-alpha04. Ingen ny release eller ny signert APK i denne bolken.

## Levert

- Kalenderens overskrift, filter, lukkeknapp, tomtilstandar og dagetikettar brukar nynorsk/engelsk-ressursar.
- Utgjevingstal og skjermlesaromtale har korrekt eintal/fleirtal. 17 tekstnøklar og to fleirtalsressursar per språk.
- Datoformat følgjer UI-lokalet. Episodetidspunkt brukar Android sitt valde tidsformat i staden for fast `HH:mm`.
- Datofelt har fleksibel breidd og sentrert tekst med nok minsteplass. Ein ny test avdekte klipping ved skriftstorleik 2.0; dette er retta og testen bevart.
- Stabil enum-identitet for filter; språkbyte bevarer vald dag og typefilter.
- UX-tekstgjennomgangen held tomtilstanden kort, med eit konkret forslag om å byte dag eller filter.

## Kontroll

- [x] 279/279 einingstestar, inkludert språkressursdekning og parameter/fleirtalsparitet.
- [x] Debug- og test-APK bygde; lint utan nye feil (28 eksisterande åtvaringar).
- [x] 12/12 Android-testar på isolert mobilprofil: kalender/detaljpanel og språkressursar.
- [x] 17/17 Android-testar på brei emulatorprofil (1920 × 1200, 240 dpi): kalender, språk og minimerbar sidemeny. Dette er 12 gjentekne testar frå mobilrunda pluss fem sidemenytestar, ikkje 29 ulike testar.

Testdata er isolerte fixtures, ikkje ekte kontodata. Ingen førespurnad, tenarinnstilling eller bibliotek vart endra. Endeleg fysisk kontroll av TalkBack, 12-/24-timarsbyte og Galaxy Tab S7+ står att. Eksisterande testar kontrollerer datovindauget (28 dagar), type-/dagfilter, nøyaktig episodeopning, retur til kalenderen og stabilt detaljpanel med sein omtale.

Ingen endring av kjelde eller tilgangsreglar for Radarr/Sonarr. Tekst i innkomande metadata og eldre app-genererte modellfelt er ikkje omsett av denne bolken; engelsk er framleis førehandsvising. Delt kalender og full Android TV-støtte er framleis eigne opne roadmap-punkt.
