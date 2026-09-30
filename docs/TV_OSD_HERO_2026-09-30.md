# TV-spelar og utval · 30. september 2026

- OSD viser systemklokka øvst til høgre når kontrollane er synlege. Ho følgjer
  språket og 12-/24-timarsvalet på eininga og oppdaterer ved kvart minutt.
- Til høgre under tidslinja står tid att, til dømes «1:08 att». Verdien følgjer
  spoleførehandsvisinga og stoppar på null. Ukjend lengd gir ingen falsk resttid.
- Avspelingsmåte («Direkte avspeling frå …» og transkodingsgrunn) er av som
  standard. Første start etter denne oppdateringa slår òg av det gamle valet éin
  gong. Brukaren kan slå det på att under Avspeling → Avanserte val; seinare
  startar og oppdateringar bevarer det valet. Kontoar og andre appval blir bevarte.
- TV-utvalet bruker 52 % av vindaugshøgda, mot 64 % før. Ved stor skrift får
  teksten framleis auke høgda, slik at handlingane kan lesast og nåast.
- Jellyfin-/Emby-logoen er fjerna frå metadata i utvalet. Kjelde står framleis
  i radoverskriftene og detaljvisinga. Dette gir tittelkunst og handlingar meir ro.
- Radoverskriftene viser kjelda éin gong, til høgre. «Sjå vidare» får dermed
  ikkje «· Jellyfin/Emby» i sjølve tittelen. Skjermlesaren får framleis begge delar.
- TV-utvalet har 8 dp mellom tekstgruppene og handlingane, mot 4 dp før.
  «Sjå vidare»-korta bruker éi tittellinje på TV, slik at episodeteksten kjem rett
  under tittelen. Lange titlar blir avkorta; full tittel finst i detaljvisinga
  og i kortet si skjermlesarskildring. Mobil held fram med opptil to tittellinjer.
- Avspelingsinfo-brytaren varslar no spelaren og innstillingane straks: nøkkelen
  mangla i lista over observerte preferansar.
- TV-utvalet byter tittel kvart åttande sekund sjølv om ein knapp har fokus.
  Fjernkontrolltrykk startar ventetida på nytt. Lett TV-modus stoppar framleis
  rotasjonen. «Rolege overgangar» og avslåtte systemanimasjonar gir direkte
  tittelbyte på TV i staden for å stoppe utvalet heilt.

Andre moglege uttrykk for kjelda er eit lite grått kjeldenamn etter metadata,
eller kjelde berre i detaljvisinga. Den valde løysinga bruker dei eksisterande
radoverskriftene og slepp å gjenta kjelda i utvalet.

## Verifisering

- 25/25 målretta einingstestar: `JellyfinPlaybackTest` og
  `PersonalizationObserverTest`.
- 11/11 målretta Android-testar i `TvRefinementUiTest` på den eksisterande
  instrumenteringsprofilen `Spole_TV_Instrumentation` (5566). Dette dekkjer
  klokke/resttid, vis/skjul av kontrollane, neste-episode-kort, faktisk
  innstillingsbrytar, direkte preferanseendring, rotasjon med fokus og rolege
  overgangar, lett TV-modus, første rad, kortbaseliner og skriftstorleik 2.0.
- `assembleDebug`, `assembleDebugAndroidTest`, målretta `testDebugUnitTest`
  og `lintDebug`: `BUILD SUCCESSFUL`. Lint: 0 feil, 161 åtvaringar og 1 hint.
- Den innlogga TV-profilen (5564) vart oppdatert med ein signert produksjons-APK
  av den første gjennomføringa, med `install -r`. OSD med ekte episodeopplysningar,
  mindre hero, heile første rad og kjelde berre til høgre vart kontrollerte visuelt.
  Kontoar og andre lagra val vart bevarte. Ingen instrumentering køyrde på denne
  profilen.
- Siste finpuss av mellomrom og rotasjon med «Rolege overgangar» er òg installert
  og kontrollert på den innlogga TV-profilen i beta11. Sjå
  `VERIFICATION_v0.18.0-beta11.md` for endelege release- og oppdateringsresultat.
