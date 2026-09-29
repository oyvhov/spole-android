# Verifisering – Spole 0.18.0-beta10

Dato: 29. september 2026. Pakke `app.reelstack`, versionCode `130`.

Utgangspunkt: beta9 med dei godkjende lokale endringane i innstillingar, bibliotekeditor,
menyar, detaljar, vurderingar, søk og avspelingsval. Hovudnavigasjonskomponentane er uendra.

## Visuell gjennomgang før release

Sjå `SETTINGS_PAGES_2026-09-29.md`, `SETTINGS_VISUALS_2026-09-29.md`,
`MENU_SYSTEM_2026-09-29.md`, `DETAILS_VISUAL_REVIEW_2026-09-29.md` og
`SEARCH_MENUS_VISUAL_REVIEW_2026-09-29.md` for faktisk dekning og avgrensingar.
Brukaren godkjende skjermbileta før oppdraget om publisering.

## Automatiske testar

Ingen einings- eller instrumenteringstestar er køyrde på nytt for denne releasen.
Historiske testtal i dei tidlegare rapportane gjeld dei respektive førehandsbygga.
Ingen instrumentering er køyrd på profilane med lagra brukardata.

## Bygg, signatur og oppdatering

- `:app:lintDebug :app:assembleRelease`: **BUILD SUCCESSFUL** på 5 minutt og 35 sekund.
- Lint: **0 feil**, 161 åtvaringar og 1 hint (same tal som beta9).
- Versjonskontroll og omsetjingskontroll fullførte; 1422 standardnøklar og éin tillaten nøkkel berre i standardsettet.
- APK: `Spole-v0.18.0-beta10.apk`, **11 418 834 byte**. Universell for arm64-v8a,
  armeabi-v7a, x86 og x86_64; minSdk 26, targetSdk 36, ikkje `debuggable`.
- SHA-256: `9a5914455d597dbcd5c2636eb6ba2ac86b7499b47a5878eb1bf3c1c23167edab`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- R8-mappinga er arkivert saman med APK-en. Alle fire FFmpeg-JNI-biblioteka er byteidentiske
  med beta9, og APK-en inneheld FFmpeg-lisensane. Den tilsvarande kjelde-/relenkingspakken
  er uendra, SHA-256 `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Kjeldehashane er kontrollerte etter bygget; ingen kjeldeendringar under bygginga.
- Mobil 5560: signert `install -r` frå 129 til 130. `firstInstallTime` er uendra
  (2026-09-15 10:09:51). Appen opnar med same engelske språk, mørke/grøne tema og demoinnhald.
  Mobilprofilen har ikkje innlogga tenester; ekte kontoar blir kontrollerte på TV.
- TV 5564 er halden på 129 til gjennomgangen av den publiserte GitHub-oppdateringa.
- Oppstartsskriptet fekk WSL-feil `0x8007274c`, men begge eksisterande emulatorane svarte
  normalt via den etablerte ADB-tenaren. Ingen emulator vart nullstilt eller erstatta.
- `git diff --check` er rein. Skjermbilete og lokale loggar ligg berre i ignorert byggmappe.

Publiserings- og oppdateringsresultatet blir dokumentert i ein eigen rapport-commit etter publisering.
