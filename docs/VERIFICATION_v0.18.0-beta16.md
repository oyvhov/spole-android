# Verifisering av Spole 0.18.0-beta16

Dato: 3. oktober 2026. Pakke `app.reelstack`, versjonskode 136.

Releasen omfattar dynamiske systemmargar for mobil-toppmenyen og stabil
bakgrunnskunst i mobil-/TV-detaljar. Tidlegare lokal kontroll er dokumentert i
[arbeidsrapporten](MOBILE_HEADER_SAFE_AREA_2026-10-03.md).

## Endeleg bygg og artefaktar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`:
  **BUILD SUCCESSFUL** på 4 minutt 3 sekund, avslutta med kode 0.
- JVM: **837/837**, ingen feil, errors eller hoppa over. Dette inkluderer ulike
  mobilformat, systemmargar, stabil bakgrunn og innlasting utan poster-fallback.
- Lint: **0 feil**, 168 åtvaringar og eitt hint.
- APK: `Spole-v0.18.0-beta16.apk`, **11 453 710 byte**, kode 136, minSdk 26,
  targetSdk 36, fire ABI-ar og ikkje `debuggable`.
- APK SHA-256: `c9296cfa5ebb7c3184e9e18b6e5475ef45eaf5f9ed7b66e20e66d6835458c090`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK og tilhøyrande R8-mapping er frosne under `app/build/release-v0.18.0-beta16/`.
- Alle fire FFmpeg-JNI-biblioteka og dei to lisensfilene er byteidentiske med
  beta15. Tilsvarande kjelde-/relenkingsarkiv har SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.

## Android-testar

- Endeleg beta16-debug-/test-APK på isolert mobilprofil 5562: **10/10** med normal
  skrift og **2/2** med fysisk systemskrift 2.0. Ingen hoppa over. Dei dekkjer
  faktisk statuslinjemarg, sveiping, sløyfe, handlingar, automatisk pause,
  vertikal rulling og stabil bakgrunn før/etter metadata.
- Bilettestane skil poster, opningsbakgrunn og sein metadata-kunst med ulike
  fargar og kontrollerer sjølve dei teikna bileta og den stabile Hero-ramma.
- Heile Android-suiten er ikkje køyrd. TV-komponenten er testa på isolert mobil,
  ikkje TV-instrumenteringsprofil. Ingen instrumentering på lagra kontoemulatorar.

## Installering før publisering

- TV 5564 er oppdatert til signert beta16/kode 136 med `install -r`.
  Opphavleg installasjonsdato 9. september er uendra. Nynorsk, grøn profil,
  profilbilde og faktisk Jellyfin-/Emby-bibliotek er kontrollerte på framsida.
- Mobil 5560 er tilbake på den publiserte beta15-APK-en/kode 135 som baseline
  for oppdateringsprøven. Opphavleg installasjonsdato 15. september og appdata
  er bevarte. Den lagra mobilprofilen er framleis i eksisterande demo.
- Mobil-emulatoren viste eit mellombels System UI-varsel etter kaldstart,
  også på den gamle beta15-utgåva. «Wait» lukka varselet utan å slette data.

Offentleg publisering og faktisk oppdateringsprøve blir dokumenterte etterpå.
