# Lokal lyddekoding

Offisiell Media3 FFmpeg-lydmodul frå AndroidX 1.11.1, commit
`8c6678b657ede1e7883fc164ef73ed483c7796c3` (Apache 2.0), med FFmpeg 6.1.4,
commit `34277e12e80031c7f89494ba543684bc1dd0be8f` (LGPL 2.1 eller seinare).
Ingen GPL-/nonfree-komponentar, eksterne kodekbibliotek eller videodekodarar er aktiverte.
Java-kjelda er uendra oppstraumskode. Dei fire JNI-biblioteka er bygde frå desse kjeldene.

Køyr på Linux/WSL med Android NDK r27b:

```sh
ANDROID_NDK_HOME=/path/to/android-ndk-r27b bash scripts/build-playback-ffmpeg.sh
```

Skriptet tek vare på kjelder og objektfiler i arbeidsmappa det skriv ut. `SPOLE_NATIVE_WORK`
kan setjast til ei eiga byggmappe. Ingen systeminnstillingar blir endra. Native bibliotek
støttar 16 KiB-sider og armeabi-v7a, arm64-v8a, x86 og x86_64; minste API er 26.

Ved binærdistribusjon skal lisensane følgje APK-en og full tilsvarande FFmpeg-/JNI-kjelde,
byggskript og relenkingsmateriale følgje releasen slik at LGPL-biblioteket kan byggjast om
og bytast ut. Sjå `licenses/`. Modulen blir distribuert frå Spole 0.17.0-beta18.

Pakk den fullførte native-byggmappa med
`SPOLE_NATIVE_WORK=/path/to/build bash scripts/package-playback-ffmpeg-sources.sh`.
Arkivet i `playback-ffmpeg/build/distribution` inneheld komplett FFmpeg-kjeldearkiv,
JNI-kjelde, skript, lisensar, konfigurasjon og statiske bibliotek for alle ABI-ar.
NDK r27b må installerast separat for å kompilere eller relenkje. Java-kjelda og
Gradle-konfigurasjonen ligg i Git-taggen til appreleasen.
