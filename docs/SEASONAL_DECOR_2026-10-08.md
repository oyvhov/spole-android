# Sesongpynt i to nivå, Halloween i biblioteket og påskeegg · 8. oktober 2026

Sesongtemaa var pynt som måtte veljast manuelt. Dei visste ikkje kva dag det var eller kva som låg i
biblioteket, og banneret «Finn julefilmen din» kunne ikkje trykkjast på. No kan sesongen følgje
kalenderen, han opnar ei eiga side i biblioteket, og pynten har to nivå.

## Endra

- **«Etter kalenderen»** er eit nytt val under Utsjånad → Sesongtema. Halloween gjeld frå
  20. oktober til 1. november, og jul frå 1. desember til 13. januar (tjuandedag jul).
  - Kalenderen legg sesongen over det lagra temaet berre i det som blir vist (`Personalization.seasonal`).
    Lagra bakgrunn og aksent blir aldri skrivne om, så appen går tilbake til dei av seg sjølv.
  - Mange skjermar lagrar ved å kopiere det dei viste. Lagringa tek difor kalendersesongen av først
    (`withoutCalendarSeason`). Ein bakgrunn eller aksent som er endra oppå sesongen, blir lagra.
  - Under valet står det kva sesong som er på og til kva dato.
- **To nivå med pynt** («Pynt»), synleg når ein sesong er valt:
  - **Av:** berre fargane.
  - **Litt:** lue eller spøkelse på logoen, helsinga på Heim, gresskar eller snøfnugg på tidslinja i
    spelaren, og sesongfigur i større lastehjul. Ingenting rører seg.
  - **Mykje:** alt frå «Litt», og i tillegg snø eller glør over bileta, scener i menyen og ark, og ein
    edderkopp som firer seg ned i menyen når fjernkontrollen har lege i ro i 45 sekund. Han klatrar
    opp att ved første tastetrykk. Halloween berre.
  - Den gamle brytaren «Sesongpynt» blir overført: på blir «Mykje» og av blir «Av». Nøkkelen blir
    fjerna ved neste lagring.
- **Halloween i biblioteket.** Ei sesongside viser titlar som tenaren har merkt `halloween`, og
  deretter sjangeren Horror/Skrekk. Jul brukar `christmas`. Sida går på tvers av alle bibliotek,
  og filter, sortering, søk og detaljar fungerer som i biblioteket elles. Ein barneprofil får berre
  titlane med stikkordet.
  - På TV er inngangen ei rad med gresskar (eller snøfnugg) i sidemenyen, først blant
    biblioteksnarvegane. Framsida på TV opnar på kunst og viser ikkje helsinga.
  - På mobil og nettbrett er helsinga på Heim ei dør («Halloween i biblioteket ›»). Ho viser
    nedteljing den siste månaden: «23 dagar til Halloween.»
- **Spelaren:** knotten på tidslinja er eit gresskar eller eit snøfnugg med same storleik og plass
  som den kvite prikken, på TV og mobil. Lastehjulet i spelaren har sesongfiguren i midten.
- **Påskeegg:** sju raske trykk på Spole-logoen, eller ↑ ↑ ↓ ↓ ← → ← → på fjernkontrollen, spolar
  tilbake. Då viser ein filmleader 3-2-1, Spole-merket snurrar baklengs og «Spolar tilbake …» står
  under. Ingen tast eller trykk blir konsumert, så fjernkontrollen verkar som før. Med redusert
  rørsle står merket stille ein augneblink.
- Pyntescena nedst i menyen blir no teikna under den nedste kontrollen, og fell bort utan plass.
  Før kunne spindelvevet legge seg over Innstillingar når menyen var lang.

## Verifisering

- 923 einingstestar: 922 bestått, 1 hoppa over (valfri prøve mot ekte nett), 0 feil. Nye:
  `SeasonCalendarTest` (9), `SeasonalPreferencesTest` (3), `KonamiCodeTest` (4) og
  `SeasonShelfQueryTest` (4).
- Lint: 0 feil, 185 åtvaringar og 1 hint. Første køyring fann `RestrictedApi` på
  `dispatchKeyEvent` i aktiviteten. Tastane blir no lytta på med `onPreviewKeyEvent` i Compose.
- Isolert TV 5566: `SeasonalTouchesTest` 8/8, `MobileSettingsTest` 6/6 (oppdatert til nivåa og med
  ein ny kalendertest), `WideNavigationSettingsTest` 6/6, `TvSetupTest` 4/4 og
  `TvRefinementUiTest` 37/38. Den siste feilen er den kjende `libraryEditorGivesARemoteItsFirstFocus`.
- Ekte data på lagra TV-profil 5564, med signert bygg av greina installert med `install -r`:
  - Halloween-rada i menyen opna ei side med Jellyfin-titlar merkte `halloween` (mellom anna
    Beetlejuice, Casper, Coraline og Practical Magic), og deretter skrekkfilmar.
  - Gresskaret låg på tidslinja medan Coraline spelte. Avspelinga varte i om lag 12 sekund, så
    ingen hald-fram-posisjon vart lagra.
  - Konami-koden spolar tilbake på framsida.
  - Profilen vart deretter sett tilbake til «Heile året», «Midnatt» og «Lime», og den offisielle
    beta5-APK-en vart installert att (same SHA-256 som arkivet). Kontoar og første installasjonstid
    er uendra.
- Jellyfin-sjangrar og -stikkord er sjekka i ein lesekopi av databasen: 38 filmar med Horror, 11 titlar
  med `halloween`, 92 med `christmas` og 17 med `christmas calendar`.

## Avgrensingar

- Med to festa bibliotek blir TV-menyen éi rad for lang for 1080p, og Innstillingar ligg under
  kanten til fokus rullar dit. Det var trongt frå før.
- Biletet av mobilbanneret er ikkje teke. Den isolerte mobil-AVD-en hadde 95 % full lagring, og
  installasjonen feila. Banneret er dekt av `SeasonalTouchesTest`. Skjermstorleiken
  1920×1200/240 på AVD-en er sett tilbake.
- Jule-sida er ikkje sjekka med ekte data. Edderkoppen er sjekka i UI-test. TV-profilen har redusert
  rørsle, og då kjem han ikkje fram.
- «Practical Magic» feila med HTTP 500. FFmpeg på tenaren melde «Invalid data» for fila i
  `W:\Filmar 10`, som truleg er ei av dei skadde filene der.
