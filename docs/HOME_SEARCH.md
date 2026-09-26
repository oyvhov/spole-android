# Søkeinngang frå Heim

## Utforming (frå 26. september 2026)

- Eit **søkeikon ved profilbiletet** øvst på Heim er den faste inngangen på telefon og nettbrett,
  både i den vanlege toppen og over heltebiletet på nettbrett. Det har same storleik (40 dp i ei
  48 dp trykkflate) og same rolege flate som profilbiletet. TV og barnemodus får det ikkje.
- **Søkelina** under toppen («Søk etter filmar og seriar») er **av som standard**. Brukaren kan slå
  henne på under Innstillingar → Heim → «Søkelina på framsida». Ho gjer det same som ikonet.
- Begge opnar **det globale søket**, ikkje Oppdag. Sjå [GLOBALT_SOK_PLAN.md](GLOBALT_SOK_PLAN.md).

Brukaren bad om dette 26. september: søket skulle vere eit globalt søk og ikkje hoppe til Oppdag,
og søkelina skulle ikkje ta plass på framsida som standard.

## Det globale søket

`GlobalSearchScreen` er ein eigen skjerm over det brukaren held på med, ikkje ei fane:

- Øvst: tilbakepil og søkefelt. Feltet får markør og tastatur med ein gong.
- Tomt felt: tidlegare søk som brikker, og ei linje om at søket dekkjer biblioteka og Seerr.
- Treff: **I biblioteka dine** først (spelbart no), så **Legg til noko nytt** frå Seerr. Ein
  tittel Seerr melder som tilgjengeleg og som biblioteksgruppa alt viser, blir berre vist der.
- Ingen treff: éi linje, utan tilvising til filter som ikkje finst her.
- Ei teneste som feilar, gir ei linje om akkurat den; den andre teneste sine treff står.
- Rulling legg vekk tastaturet. Tilbake eller pila lukkar søket og fører til skjermen under.

Søket har **si eiga spørjing** (`ReelstackUiState.globalSearch`, ein `SearchSlice`). Oppdag sitt
felt, filter og rulleposisjon blir ikkje rørte. Same `DiscoverSearchCoordinator` driv begge, med
kvar sin del av tilstanden. Søket startar tomt kvar gong det blir opna; tidlegare søk ligg som
brikker.

## Testdekning

- `DownloadsAndSearchUiTest` (Robolectric): eigen skjerm utan Oppdag-overskrift eller filter,
  grupper og duplikat, tidlegare søk, tomt resultat, lukking.
- `GlobalSearchUiStateTest`: duplikatregelen og normalisering av titlar.
- `HomeSearchNavigationTest` (instrumentert): ikonet opnar søket med fokus, lukking fører til Heim,
  Oppdag beheld filter og tomt felt, søket opnar tomt, og søkelina kjem att når ho blir vald.
- `HomeSearchEntryTest` dekkjer framleis sjølve søkelina på 300 dp og dobbel skriftstorleik.
