# ADR 0001: Avspelingsarkitektur og transkoding

## Status
Godkjent / Gjeldande

## Kontekst
Spole speler video og lyd frå fleire medietenarar (Jellyfin og Emby) på tvers av Android TV, nettbrett og telefonar. Einingane har svært ulik maskinvarestøtte for lyd- og videokodekar (t.d. AC-3, E-AC-3, DTS, TrueHD, ASS/SSA-undertekstar).

## Avgjerd
1. **Direct Play som hovudstrategi:** Klienten ber alltid om direkte strauming utan transkoding når eininga og nettverket tillet det, for å spare ressursar på tenaren og bevare original kvalitet.
2. **Native FFmpeg-utviding:** Appen nyttar ei eiga native C/C++ FFmpeg-utviding for ExoPlayer/Media3 (`playback-ffmpeg`) som leverer påliteleg software-dekoding av lydspor (t.d. DTS-HD, TrueHD, FLAC) som manglar maskinvarestøtte på Android TV.
3. **Ompakking framfor full transkoding:** Ved ukompatibel mediebehaldar (container) prøver klienten først remuxing/ompakking (kopiere videostraum og transkode berre lyd) før full videotranskoding blir bede om frå tenaren.
4. **Feilhandtering og automatisk gjenoppretting:** Viss direkte avspeling feilar i spelaren, gjer Spole eit kontrollert byte til transkoda straum med bevart avspelingsposisjon og valde undertekstspor.

## Konsekvensar
- Låg latens ved oppstart for dei fleste titlar.
- Høgare stabilitet på eldre Android TV-boksar via FFmpeg fallback.
- Fridom frå eksterne proprietære mediespelarar; all avspeling skjer i appen sitt eige UI med felles D-pad navigering.
