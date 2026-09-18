# Verifisering: Spole 0.17.0-beta18

Dato: 18. september 2026. Signert universal testutgåve for telefon og TV.

## Bygg og testar

- `testDebugUnitTest`: 622 testar, 0 feil.
- `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease`: fullførte med `BUILD SUCCESSFUL`.
- `lintDebug`: 0 feil, 93 åtvaringar. Ingen nye undertrykkingar.
- Endeleg målretta instrumentering på isolert Android 16-mobil, 5562: **10/10 bestått**.
  Fire lydtestar spelar og spolar syntetisk AVC + DTS/AC3/EAC3/TrueHD 5.1 mot både
  Emby- og Jellyfin-fixture utan serveromkoding. Tre testar kontrollerer kapabilitetar/JNI;
  resten dekkjer undertekstoppvarming, direktevideo med rotasjon og HLS med spoling.
- Rå sluttresultat: lokalt `app/build/release-v0.17.0-beta18/playback-tests.txt`.
- Den breiare spelartestrunden er **ikkje heilt grøn**: 31 valde testar, 1 PiP-returfeil,
  TV-fjernkontrolltesten hoppa over på mobil. PiP-feilen vart òg reprodusert separat.
  Ingen spekulativ PiP-retting eller ignorering av testen er med i releasen.

## APK og native-bibliotek

- Pakke `app.reelstack`, versjonskode **97**, versjonsnamn **0.17.0-beta18**.
- Ikkje debuggable; same produksjonssignatur som tidlegare.
- APK: **11 062 102 byte**.
- APK SHA-256: `920e787c744d3667cc55e2c7bd2600c3e3c8af9202a007db01c6c48762b8bb88`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- `zipalign -c -P 16 4`: godkjent. Fire ABI-ar: arm64-v8a, armeabi-v7a, x86, x86_64.
- FFmpeg 6.1.4 / Media3 1.11.1, LGPL-only audio. Lisensane ligg i APK-en.
  Native ELF-segment har 16 KiB justering; R8 bevarer JNI og den reflekterte renderaren.
- Kjelde-/relenkingsarkiv SHA-256:
  `5378dbc96bfe0efa782e6315ca629da82dc846b2bbf6a0d9ac94a2686d92f132`.
- APK, R8-mapping og native-kjeldearkiv er arkiverte i `app/build/release-v0.17.0-beta18/`.

## Installasjon og avgrensingar

- Signert `adb install -r` frå eksisterande produksjon 0.13.2 (kode 37) til beta18 (97)
  fullførte på isolert 5562. Lagra demomodus og nynorsk var bevarte, og heimesida opna.
  Dette er ikkje ei nedlasting gjennom appen eller ein kontroll av ekte kontoar.
- Review-profilane 5560 og 5564 vart starta utan sletting eller nullstilling, men ADB
  vart ståande som `unauthorized`. 5560 viste i tillegg «System UI keeps stopping».
  Difor er bevaring av ekte kontoar/bibliotek på desse profilane ikkje verifisert her.
- Oppdateringsflyten frå offentleg GitHub blir kontrollert etter publisering; resultat
  blir dokumentert i ein etterfølgjande rapport-commit utan å flytte release-taggen.
- Fysisk Pixel 9 Pro og Shield TV → TV → optisk → Sonos Playbar er ikkje testa.
  Lokal PCM-dekoding kan gi stereo gjennom optisk. Direkteavspeling er ikkje eit løfte
  om surround eller Atmos/DTS:X. Video/HDR og biletundertekstar kan framleis krevje omkoding.
- Dei syntetiske server-fixturane er ikkje ein ende-til-ende-test mot brukarens ekte serverar.
