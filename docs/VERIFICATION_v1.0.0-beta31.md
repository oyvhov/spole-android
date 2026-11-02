# Verifisering av Spole 1.0.0-beta31

2. november 2026. Produksjonspakke `app.reelstack`, versjonskode 168. Bygd oppå beta30 (`37a695f`).

## Omfang

Runden i `RORSLE_OG_TOPPMENY_2026-11-01.md` og punkt 1–7 med kosmetikk frå `SWIFTLY_LAN_2026-11-01.md`.
Arbeidet vart gjort av fleire: rørslespråket og profilflukta i hovudøkta, hopp-knappen, radinngangen,
toppmenyen og det rolegare søket av Codex (`HANDOFF_CODEX_2026-11-02.md`), og søk på TV, spelaren,
seriesida og kosmetikken av fire parallelle agentar i eigne arbeidstre, fletta inn utan konfliktar.

- `ui/theme/SpoleMotion.kt`: fokus 140, endring 280, ark 320/200, landing 450 (fjør, demping 0,86,
  stivleik 195), redusert 100 ms lineært. 17 filer migrerte; spelaren sin OSD og dei langsame
  effektane er urørte.
- `ui/ProfileSwitchMotion.kt` og `ui/ProfileFlightLayer.kt`: flukt 450 ms, bytet ved 150 ms, radene
  30 ms etter landing, aldri meir enn 1 500 ms halding. Utan kjent sirkel, med redusert rørsle, via
  PIN eller frå Stova: inga flukt.
- `ui/components/RowEntrance.kt`, `ui/TvTopBar.kt`, `TvMenuStyle` med migrering frå `hide_tv_sidebar`.
- `player/SegmentPolicy.kt`, `player/TrackMatching.kt`, `data/repository/TrackChoiceStore.kt`.
- `ui/DetailSeries.kt` (episoderada), `ui/components/WatchProgressBar.kt`, `ui/theme/Oklch.kt`,
  `ui/components/ProfileHue.kt`.

## Bygg og testar

- `testDebugUnitTest`: 1 559 testar, 1 558 bestått, 0 feil, 1 hoppa over (den valfrie nettverkstesten). `lintDebug`: 0 feil, 218 åtvaringar. Omsetjingskontroll: 1 694 nøklar, berre den eine tillatne standardnøkkelen utan omsetjing.
- Isolert TV 5566: første runde på den samla greina før søk-fletta, 17 klassar, 136 testar, alle bestått (TvTopBarUiTest 4, TvNavigationIntegrationTest 3, RowEntranceTest 5, StartupRevealTest 6, HomeMediaRowsTest 6, HomeHeaderTest 5, AccountPanelsTest 11, ProfileSessionBoundaryTest 1, AccountOptionsTest 3, JellyfinPlayerUiTest 15, TvPlaybackControlsTest 15, PlaybackOptionsUiTest 3, EpisodeSeriesNavigationTest 1, HomeVisualPolishTest 3, MediaArtworkTest 1, TvRefinementUiTest 38, DesignRefreshUiTest 22); andre runde etter søk-fletta og versjonsbyte: TvNavigationIntegrationTest 3/3 og TvTopBarUiTest 4/4 bestått; HomeSearchNavigationTest 0/4, som er den kjende grunnlinja for denne telefontesten på TV-profilen (same fire namn som før runden) Emulatoren segfaulta éin gong (qemu RenderThread) og KVM-rettane i WSL måtte setjast att; sjå minnenotatet.

Ingen førespurnad vart send, og ingen avspeling starta på den ekte kontoen.
