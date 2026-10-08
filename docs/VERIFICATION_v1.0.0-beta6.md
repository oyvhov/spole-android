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
- TV-profilen 5564 vart halden på beta5 til den ekte oppdateringa gjennom appen, sjå under.

## Offentleg release

- Publisert som prerelease, ikkje draft, med nøyaktig éin universal-APK og fem assets:
  https://github.com/oyvhov/spole-android/releases/tag/v1.0.0-beta6 . Stabil latest er ikkje endra.
- Kjelde/tag: `759b5191d98773336683adbec19e2a0afd2b687d`, ein fast-forward av `main` frå beta5.
  `SOURCE_COMMIT.txt` peikar på same commit.
- Alle fem asset-digestar og storleikar i kladden samsvarte med arkivfilene.
- Den uautentiserte release-lista viser beta6 med `draft=false` og APK-digest `sha256:2c8a0b43…0e3e`.
  Den offentleg nedlasta APK-en er 11 589 490 byte og har same SHA-256 som det signerte bygget.

## Oppdatering gjennom appen

- På lagra TV-profil 5564 med `1.0.0-beta5` / kode 142: Innstillingar → Varsel og oppdatering →
  Appoppdateringar → Sjekk no fann `v1.0.0-beta6` med dei offentlege release-notata.
- «Last ned oppdatering» lasta ned APK-en gjennom Spole. «Installer oppdatering» kom etter appen sine
  kontrollar. Android sin oppdateringsdialog vart godkjend og melde «App installed.». Play Protect
  spurde ikkje på TV-profilen.
- Installert pakke er `1.0.0-beta6` / kode 143, sist oppdatert 8. oktober 2026 kl. 19:55:59. Første
  installasjonstid er framleis 9. september 2026 kl. 20:50:58.
- Appen opna att på heimesida med den eksisterande Seerr-profilen og «Sjå vidare» frå Jellyfin og Emby,
  utan nytt oppsett. Sesongtemaet stod framleis på «Heile året».
- Dette er ei faktisk offentleg nedlasting og Android-installasjon gjennom appen, i tillegg til den
  separate `install -r`-kontrollen på mobilprofilen. Ingen appdata eller kontoar er nullstilte.
- GitHub-kontrollane på release-commit `759b519` fullførte med `success` både for `main` og for greina
  `sesongpynt` (bygg, lint og einingstestar; instrumenteringsjobben er manuell og vart hoppa over).
