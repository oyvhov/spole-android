# Verifisering: 0.16.0-beta06

Versjonskode 73, pakke `app.reelstack`. Same eksisterande signeringssertifikat.

## Release-bygg

- `./gradlew.bat testDebugUnitTest assembleRelease` køyrd.
- `testDebugUnitTest` køyrd og passerte etter oppdatering av testane for hero-utval.
- Release-APK laga: `Spole-v0.16.0-beta06.apk`.
- Release-kataloga vart oppdatert i `app/build/release-v0.16.0-beta06` med APK, `mapping.txt`,
  `SHA256SUMS.txt` og `SOURCE_COMMIT.txt`.

## Appikon-fiks

- `AndroidManifest.xml` er oppdatert til å bruke `@drawable/ic_launcher` som både `android:icon`
  og `android:roundIcon`.
- `aapt2 dump resources` viser framleis korrekt `mipmap/ic_spole`-ressurs i pakken, men manifestet peikar
  no eksplisitt til ikonvarianten med lågare visuelt avvik på TV.
- Oppdatert `versionCode=73` og `versionName=0.16.0-beta06` er synleg i apk-metadata.
