# Verifisering — Spole 0.18.0-beta7

Dato: 23.–24. september 2026

## Bygg og artefakt

- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease`
  fullførte med **BUILD SUCCESSFUL** (5 min 17 s) på Windows.
- Einingstestar: **740 køyrde, 0 feila, 0 feil og 0 hoppa over** (727 i beta6; 13 nye).
- Instrumenteringstestar på den isolerte mobiltestprofilen (5562), heile pakka for første gong sidan
  beta4: **377 køyrde, 362 bestått, 15 feila**. Dei same 15 feilar identisk med beta6-koden
  (`418d872`) på same emulator, køyrde isolert. Dei er altså ikkje regresjonar frå denne utgåva:
  - `HomeSearchNavigationTest` (3): ventar nynorsk oppstartstekst; emulatoren står på `en-US`.
  - `CompactNavigationTest` (2): botnlinja viser ikkje lenger dei forventa korte orda.
  - `JellyfinPlayerTest` (8): tidsvindauge og vindaugsfokus på den hovudlause swiftshader-emulatoren.
    Avspeling startar i alle åtte før feilen.
  - `TvLibraryRegressionTest` (1) og `UiConsistencyTest` (1).
  Dei bør ryddast i ei eiga oppgåve; dei skjuler elles framtidige regresjonar.
- Lint: **0 feil**, 148 åtvaringar og 1 hint.
- Produksjons-APK: `app.reelstack`, `0.18.0-beta7` / versionCode `127`, minSdk `26`, targetSdk `36`,
  universell for `arm64-v8a`, `armeabi-v7a`, `x86` og `x86_64`, ikkje `debuggable`, med
  FFmpeg-lisensane i `assets/licenses/ffmpeg/`.
- APK: `Spole-v0.18.0-beta7.apk`, 11 354 854 byte, SHA-256
  `70283d5068e7801ecf51077b789955c18b86cedfa5648c53fae157fb0e81cccc`. Storleiken er tilfeldig lik
  beta6; hashen er ulik.
- Signatur: sertifikat-SHA-256 `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- FFmpeg-kjelde- og relenkingspakken er uendra sidan 0.17.0-beta18 og har SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

## Nedlasting

Feilen: nedlastinga forhandla med `DevicePlaybackCapabilities.CONSERVATIVE` (H.264 ≤ 1080p,
AAC/MP3 i stereo). Tenaren planla difor omkoding for alt med 5.1-lyd, HEVC eller over 1080p,
og Spole avviste fila som «ikkje direkte». Bildetekstar (PGS) som standardspor utløyste i tillegg
innbrenning, og 80 Mbit/s var ei grense for store remuxar.

Retta og dekt av einingstestar (`OfflineNegotiationTest`): éi forhandling utan innbrenning for ein
fil med PGS som standardspor, ingen straumingsgrense, flytting av URL mellom adresser og Wi-Fi-krav.

Ekte filoverføring er **ikkje** køyrd: review-mobilen (5560) har medvite inga lagra teneste, og
det vart ikkje oppretta konto berre for testen. TV-profilen har kontoar, men TV har ikkje nedlasting.

## Ytelse

Sjå `YTELSESPLAN_2026-09-22.md` for kva som er gjort per punkt. Dekt av einingstestar:
`ParallelFeedTest` (same straum sekvensielt og parallelt, tak på samtidige kall), `AddressRouteTest`
(adressebyte før profil og straum), `ViewerAccessTest` (identitet gjenbrukt til han går ut),
`RailShapeTest` (kortstorleik for breie bilete).

## Installasjon og oppdatering

_ventar_
