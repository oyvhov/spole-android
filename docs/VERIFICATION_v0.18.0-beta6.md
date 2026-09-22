# Verifisering — Spole 0.18.0-beta6

Dato: 22. september 2026  
Kjelde-commit og publisert tagg: `123899b3512d9cab9341a3fd3ceb218c6d5f286e`

## Bygg og artefakt

- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og
  `assembleRelease` fullførte med **BUILD SUCCESSFUL** (254 oppgåver i det tvinga, reine
  releasbygget).
- Einingstestar: **727 køyrde, 0 feila, 0 feil og 0 hoppa over**.
- Instrumenteringstestar på den isolerte mobiltestprofilen: `OfflineDetailDownloadUiTest` og
  `OfflineDownloadsUiTest`, **4 av 4 bestått**. Ingen instrumenteringstest vart køyrd mot
  kontoane på review-einingane.
- Lint: **0 feil**, 147 eksisterande åtvaringar og 1 hint.
- Produksjons-APK: `app.reelstack`, `0.18.0-beta6` / versionCode `126`, minSdk `26`,
  targetSdk `36`, universell for `arm64-v8a`, `armeabi-v7a`, `x86` og `x86_64`, og ikkje
  `debuggable`.
- APK: `Spole-v0.18.0-beta6.apk`, 11 354 854 byte, SHA-256
  `3b3dff5d922ea0e0871ab8491acdd3d109ddfdf135adc090129cfb0fcf1d03a1`.
- Signatur: sertifikat-SHA-256
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- FFmpeg-kjelde- og relenkingspakken er med og har SHA-256
  `2b7eeba9705de0fcff66d3a04f71a811d39a9299d70e1f65728a8a8809965b21`.
- Releasen har nøyaktig fem assets: éin universell APK, kontrollsummar, R8-mapping,
  kjelde-commit og FFmpeg-kjeldepakken. GitHub-digest og ein offentleg nedlasta APK utan
  autorisasjon samsvarar med den lokale SHA-256-hashen.

## Nedlastingshandling på detaljsida

- Film- og episodenedlasting startar framleis berre frå detaljsida på vaksen mobil/nettbrett;
  TV, barnemodus og spelaren viser ikkje handlinga.
- Når den direkte-kompatible fila blir forhandla, viser knappen straks klargjering og kan ikkje
  trykkjast fleire gonger. Feil frå tenaren blir ståande på den same detaljsida i staden for å
  forsvinne stille.
- Ein vellykka jobb opnar **Nedlastingar** automatisk. På seriesider forklarer handlinga at
  episodar framleis lastar, i staden for å vere eit stilt deaktivert symbol.
- UI-testane dekkjer klargjering, dobbeltrykkblokkering, eksplisitt feilrespons og at serie-
  handlinga forklarer lastinga. Ekte Jellyfin-/Emby-filoverføring er ikkje køyrd på review-
  mobil denne gongen: den har medvite inga lagra teneste, og ingen konto eller brukarprofil vart
  oppretta berre for testen.

## GitHub-oppdatering med ekte TV-profil

1. Den bevarte TV-profilen starta på `0.18.0-beta5` / versionCode `125` med eit eksisterande
   Jellyfin-bibliotek.
2. **Innstillingar → Varsel og oppdatering → Appoppdateringar → Sjekk no** fann
   `v0.18.0-beta6`, korrekt storleik og dei publiserte notata.
3. **Last ned oppdatering** fullførte kontrollen og viste **Installer oppdatering**.
4. Android sin **Update**-dialog installerte pakken. Etter **Open** viste pakkekontrollen
   versionCode `126` og `0.18.0-beta6`; den same lagra Jellyfin-profilen og «Sjå vidare»-radene
   var framleis synlege.

Release: <https://github.com/oyvhov/spole-android/releases/tag/v0.18.0-beta6>
