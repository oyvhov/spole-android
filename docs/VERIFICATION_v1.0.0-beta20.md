# Verifisering av Spole 1.0.0-beta20

25. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 157.

## Omfang

Tilbakemelding frå eigaren: ordet «TBA» skal ikkje stå på episodar; skriv heller episodenummeret
med tekst.

«TBA» er plasshaldaren tenaren eller TMDB brukar for ein episode utan namn. Den felles reinsinga
(`episodeNameOf`) fjerna han alt i dei kompakte linene («S1 - E5»), men tre stader kom råteksten
likevel fram:

- Episodekorta på seriesida fall tilbake til råteksten når det reinsa namnet var tomt, og skreiv
  «5 · TBA». Kortet heiter no `episodeCardName` og skriv «Episode 5» (same tekst som før for
  episodar som heiter «Episode 5»); ekte namn står som «5 · Pilot».
- Kommande episodar frå Seerr/TMDB fekk heile namnet som tittel, ofte «TBA». Namnet blir no reinsa
  før det blir lagra.
- Fleire visingar skreiv undertekstlina urørt (smarthylle-kort, barnemodus, nedlastingslista,
  korttekst i radene, valet av episodar å laste ned). `withoutPlaceholders` tek bort berre
  plasshaldardelar av lina.

`isEpisodePlaceholder` kjenner att «TBA», «TBD», «N/A», «-» og tom tekst uavhengig av store og små
bokstavar, også i kalenderklientane og i parseren sin reserve for episodar utan serienamn.

## Bygg og testar

- 1227 einingstestar: 1226 bestått, éin valfri nettverkstest hoppa over. Lint: 0 feil.
- Nye testar i `EpisodeLabelTest` (kortnamn og reinsing av lina) og `DesignRefreshTest` (Seerr og
  kalender). Alle feila med beta19-koden; den viste «5 · TBA».
- Isolert TV 5566: EpisodeSeriesNavigationTest 1/1, SmartShelvesTest 30/30, DetailArtworkUiTest 3/3, SessionDetailsTest 4/4, CalendarAndSheetTest 7/7, HomeMediaRowsTest 6/6, LibraryBrowserUiTest 3/3, OfflineDownloadsUiTest 1/1, TvLibraryRegressionTest 1/1; TabletFeatureTest 5/9 og ReleaseRowsTest 1/2 med dei same kjende feila som beta17–19.

Ingen førespurnad vart send, og ingen avspeling starta på den ekte kontoen.
