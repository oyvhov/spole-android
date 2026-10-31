# Verifisering av Spole 1.0.0-beta27

31. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 164. Bygd oppå beta26 (`5d5d1ff`).

## Omfang

Tilbakemelding frå eigaren: biletet som viser kvar ein spolar, låg nesten midt på skjermen;
designet skulle bli minimalistisk og fint.

Før: biletet var alltid sentrert vassrett på skjermen og stod 150 dp over tidslina, i eit svart
kort med tida under, og «+10 s»-merket låg midt på skjermen i tillegg.

No:

- `seekPreviewOffset`/`SeekPreviewPlacement` legg biletet 8 dp over tidslina, sentrert på
  spolepunktet og halde innanfor endane av sporet. Begge tidslinene rapporterer sporet sitt i
  rot-koordinatar (TV: utan dei 12 dp med indre marg; telefon: mellom midtpunkta til handtaket).
- `PlaybackSeekPreview`: berre biletet (200 dp på TV, 168 dp på telefon, sideforhold frå
  trickplay), 10 dp hjørne, 1 dp kvit kant med 22 % dekning, skugge, og tida som ein liten
  etikett nedst inne i biletet.
- «+10 s»-merket blir ikkje vist medan eit førehandsbilete finst.
- Med skjulte kontrollar på TV ligg biletet over tidsfeltet nedst.

## Bygg og testar

- 1325 einingstestar: 1324 bestått, éin valfri nettverkstest hoppa over. Lint: 0 feil.
- Nye testar: `SeekPreviewPlacementTest` (plassering og endar). `PlaybackPreviewUiTest` krev no at
  biletet ligg rett over tidslina, til venstre for midten ved 10 av 60 s og til høgre ved 40 av 60 s,
  og at «+10 s»-merket ikkje er synleg. Med beta26-koden låg biletet midt på (x = 960 av 1824).
- Isolert TV 5566: PlaybackPreviewUiTest 2/2, PlayerSeekLayoutTest 14/14, TvPlaybackControlsTest 15/15, JellyfinPlayerUiTest 14/14, JellyfinPlayerTest 44/44.

Ingen førespurnad vart send, og ingen avspeling starta på den ekte kontoen.
