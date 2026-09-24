# Tilpass framsida

Frå 0.18.0-beta8 har framsida éin samla meny: **Innstillingar → Heim → Innhaldsrader → Tilpass
framsida**. Han erstattar fire eldre innstillingar: «Neste episode», «Slå saman hald fram»,
rad-dialogen på TV og dialogen for rekkjefølgje, og han tek over rad-formata («Omslag»).

## Modell

`data/model/HomeLayout.kt` eig reglane:

- **`HomeRowKind`**: kva ei rad viser. Hald fram, neste episode, favorittar, nye filmar og nye
  episodar finst **éin gong per mediatenar** (`perSource`). Spelar no, tilrådingar, nyleg ute og
  kjem snart er felles.
- **`HomeRowKey`**: radtype pluss teneste, lagra som `KIND` eller `KIND:SOURCE`, til dømes
  `NEW_SERIES:EMBY`. Same id er nøkkelen for kortformatet i `Personalization.homeRowFormats`.
- **`HomeLayout`**: rekkjefølgja på alle kjende rader, og kva som er skjult. Rader for ein tenar
  som ikkje er kopla til, held plassen sin, slik at han kjem tilbake der han var.
- **`HomeLibraryChoice`**: kva bibliotek som fyller kvar rad, **per konto**, sidan
  bibliotek-id-ane høyrer til éin tenar. Ein radtype utan val tek alle bibliotek, også dei som
  kjem til seinare.
- **`HomeFetchPlan`**: kva ei oppfrisking skal hente. Skjulte favorittar, nye filmar, nye episodar
  og nyleg ute blir ikkje spurde etter i det heile. Hald fram og neste episode blir alltid henta,
  fordi Watch Next på Google TV og nettbrett-helten brukar dei.

Framsida brukar ikkje lenger biblioteksvalet frå Bibliotek-fana. Det valet gjeld no Bibliotek og
søk. Mediecachen har eit fingeravtrykk etter biblioteksvala for framsida
(`homeLibrariesFingerprint`).

## Overgang

- Første lesing av `homeLayout` byggjer oppsettet frå `home_row_order`, `home_sections` og
  `show_next_up` og lagrar det under `home_layout`. Éin gammal brytar blir til ei rad per tenar.
- Første lesing av `homeLibraries(konto)` kopierer biblioteksvalet frå Bibliotek-fana til alle
  radene for den kontoen. Det var det som styrte framsida før.
- Å skrive dei gamle nøklane (`homeRowOrder`, `visibleHomeSections`) fjernar `home_layout`, slik at
  eldre testar og flytar som set dei, framleis får effekt.
- Eit kortformat valt før delinga gjeld til rada får sitt eige (`homeRowFormat` med
  `legacyFormatKey`).

## Menyen

`ui/screens/HomeLayoutEditor.kt`:

- Filterbrikker: **Alle**, éi per tilkopla mediatenar, og **Anna** for felles rader.
- Med ein tenar vald: **Vis alt frå …** og **Skjul alt frå …**.
- Kvar rad: tenestelogo, namn, teneste, «Skjult», brytar, pilar opp/ned og ein knapp for
  **bibliotek og format**. Kontrollane ligg på ei eiga linje, så tittelen aldri blir klemt ved
  skriftstorleik 2.0.
- Rad-innstillingane listar berre bibliotek som kan fylle rada: filmbibliotek for nye filmar,
  seriebibliotek for nye episodar og neste episode, og alle for hald fram og favorittar. Kryssar
  du av alle, betyr det «alle bibliotek».
- Når «Slå saman hald fram og neste episode» er på, er neste episode-rada merkt «Visast inni
  …», og brytaren hennar avgjer om episodane blir slått inn.
- Endringar blir lagra med ein gong. Ei endring som krev data den siste oppfriskinga ikkje henta,
  startar ei ny oppfrisking.

## Testar

- `HomeLayoutTest`: overgang, flytting, synlegheit per teneste, lagring, biblioteksval og henteplan.
- `HomeLayoutEditorTest` (Robolectric, mobil): Emby-seriar utan Jellyfin, tenarar utan adresse,
  flytting og tilbakestilling, bibliotek per rad og skriftstorleik 2.0.
- `ParallelFeedTest` og `AddressRouteTest` dekkjer henting; `HomeLayoutUiTest` på emulator.
