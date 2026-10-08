# Verifisering av Spole 1.0.0-beta6

8. oktober 2026. Produksjonspakke `app.reelstack`, versjonskode 143.

## Innhald

Sesongpynt i to nivå, sesong etter kalenderen, Halloween- og juleside i biblioteket, sesongknott på
tidslinja og påskeegg. Detaljar i [SEASONAL_DECOR_2026-10-08.md](SEASONAL_DECOR_2026-10-08.md).
Alt anna er som i beta5.

## Bygg, einingstestar og lint

- Fullt bygg (`testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`)
  avslutta med `BUILD SUCCESSFUL`, kode 0, etter 11 minutt 39 sekund.
- 923 einingstestar: 922 bestått, 1 hoppa over (valfri prøve mot ekte nett), 0 feil.
- Lint: 0 feil, 185 åtvaringar og 1 hint.
- `check-release-consistency.py`: versjon, README, CHANGELOG og release-notat er i takt.

## Android-testar

Køyrde på isolert TV-profil 5566 med greina sitt debug-bygg, før versjonsauken. Etter det er berre
versjon, notat og linjeskift i lisensfiler endra.

- `SeasonalTouchesTest` 8/8, `MobileSettingsTest` 6/6, `WideNavigationSettingsTest` 6/6 og
  `TvSetupTest` 4/4.
- `TvRefinementUiTest` 37/38. Den eine feilen, `libraryEditorGivesARemoteItsFirstFocus`, er kjend frå før.
- Ekte data på lagra TV-profil 5564 med signert bygg av greina, der profilen etterpå vart sett tilbake
  til beta5:
  - Halloween-sida viste Jellyfin-titlar merkte `halloween`.
  - Gresskaret låg på tidslinja under avspeling.
  - Konami-koden spolar tilbake.
- Mobilbiletet manglar fordi den isolerte mobil-AVD-en hadde full lagring.

## Signert produksjonsartefakt

- Universal-APK: 11 589 490 byte; `app.reelstack`, kode 143, `1.0.0-beta6`, minSdk 26 og
  targetSdk 36; ikkje debuggable.
- SHA-256: `2c8a0b432024cc9647519e1b35a3b8a088dbf5220ba08b6a3c663c45d4db0e3e`.
- Sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mapping: 90 361 666 byte. `pg_map_id` i mappinga finst i `classes.dex`.
- Alle åtte native bibliotek og begge FFmpeg-lisensfilene er byteidentiske med beta5. Det første
  bygget frå ei ny Windows-arbeidsmappe hadde lisensfilene med CRLF. Dei er normaliserte, og
  `.gitattributes` held dei no på LF. Kjelde- og relenkingsarkivet er det same som før: 30 790 595
  byte, SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Signert APK er installert med `install -r` over beta5 på lagra mobilprofil 5560. Versjonen vart
  `1.0.0-beta6` / 143, sist oppdatert 8. oktober 2026 kl. 19:52:48. Første installasjonstid er
  framleis 15. september 2026 kl. 10:09:51, og appen opna rett på heimesida utan nytt oppsett.
- TV-profilen 5564 står på beta5 for den ekte oppdateringa gjennom appen.
