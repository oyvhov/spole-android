# Direkteavspeling: gjennomgang 18. september 2026

## Utgangspunkt

Brukarbiletet viser Emby, AVC-video og DTS 5.1, med `AudioCodecNotSupported`.
DTS manglar i lista over lydformat appen melder til serveren. Det forklarer kvifor
serveren konverterer lyd, men er ikkje i seg sjølv ein grunn til å kode om AVC-biletet.
Eit skjermbilete åleine stadfestar ikkje kva kodarar serveren faktisk køyrde.

## Funn og endringar

| Funn | Endring |
|---|---|
| Berre plattformdekodarar og passthrough; ingen lokal DTS-/TrueHD-reserve | Offisiell Media3 FFmpeg-lyddekodar, bygd frå låste kjelder, for alle fire Android-ABI-ar |
| Lydformat manglar i serverprofilen sjølv om lokal programvare kan dekode dei | Profilen spør det lasta JNI-biblioteket om faktisk kodekstøtte |
| Støtte for stereoinndata vart brukt som prov på støtte for 5.1-inndata | Fjernar denne slutninga; krev korrekt plattformstøtte, passthrough eller den lokale dekodaren |
| Buffering over 18 sekund kunne tvinge full videoomkoding | Beheld ventemelding og endeleg tidsavbrot, men endrar ikkje kodekar berre fordi nettet er tregt |
| Vilkårlege feil på originalfila kunne utløyse lydomkoding | Berre dekodar-, lydutgangs- og ikkje-støtta-containerfeil kan utløyse konvertering |
| Endå ein lydfeil kunne tvinge full videoomkoding | Stopp med feil dersom lydreservevegen feilar; berre stadfesta videofeil utløyser full fallback |
| Emby sin godkjende, statiske `DirectStreamUrl` vart oversett | Brukar original HTTP-fil når serveren tillèt DirectStream og URL-en uttrykkeleg har `Static=true` |
| Intern ny forhandling kunne endre kompatibilitetsnivå utan at spelarmodellen visste det | `PlaybackPlan` tek med det faktiske kompatibilitetsnivået |

Maskinvaredekoding og støtta passthrough er framleis førsteval. Lokal lyddekoding
gir PCM til Android-lydutgangen. Det inneber ikkje at TV-høgtalarar eller ARC kan
levere alle dei opphavlege kanalane, eller at Atmos/DTS:X-objekt blir bevarte ved PCM-dekoding.
Ingen programvaredekodar for video er lagd til.

Alle server-URL-ar går framleis gjennom same-origin-kontroll og fjerning av tilgangsteikn.
Ein vilkårleg HLS-/remux-URL blir ikkje merka som originalfil, og serverrettar blir ikkje overstyrte.
Nettverksfeil får avgrensa retry utan å nedgradere videoen.

## Samanlikningsgrunnlag

- [Jellyfin Android TV](https://github.com/jellyfin/jellyfin-androidtv) inkluderer ein
  FFmpeg-lydmodul saman med Media3. Det viser kvifor berre ein ExoPlayer-avhengnad
  ikkje gir same kodekdekning som den komplette Jellyfin-klienten.
- [Wholphin](https://github.com/damontecres/Wholphin) dokumenterer ekstra lyddekodarar
  for ExoPlayer og ein alternativ MPV-spelar. Vi tek ikkje inn ein ekstra videospelar
  eller kopierer appkode; denne endringa utvidar den eksisterande Media3-vegen.
- [Media3 sine støtta format](https://developer.android.com/media/media3/exoplayer/supported-formats)
  skil mellom containerstøtte, Android-dekodarar og ekstra programvaredekodarar.
- [Den offisielle FFmpeg-modulen](https://github.com/androidx/media/tree/1.11.1/libraries/decoder_ffmpeg)
  er grunnlaget for implementasjonen. Sjå `playback-ffmpeg/README.md` for kjeldeversjonar,
  bygging og distribusjonsmateriale.
- [Emby: Direct Play / Direct Stream / Transcoding](https://support.emby.media/support/articles/DirectPlay-Stream-Transcoding.html)
  og [Emby Playback Guidelines](https://betadev.emby.media/doc/restapi/Playback-Guidelines.html)
  er grunnlaget for skiljet mellom originalfil, kopiert video og full omkoding.

## Testopplegg

- Einingstestar for original DirectStream-URL, servergodkjenning, URL-sanitering,
  bevart videokopiering ved lydfallback og skiljet mellom transport- og kodekfeil.
- Fire 20-sekunds MKV-fixtures: AVC-video og syntetisk DTS, AC3, EAC3 eller TrueHD 5.1.
  `scripts/build-audio-test-fixtures.sh` lagar dei frå eksisterande testvideo og ein sinustone.
- Faktisk avspeling gjennom appaktiviteten, lydutdata og spoling til åtte sekund,
  mot syntetiske Emby- og Jellyfin-endepunkt. Kontrollerer at videoen går direkte,
  utan fallback/HLS og utan ny avspelingsforhandling ved spoling.
- Berre `emulator-5562` blir brukt til instrumentering. Kontoane og appdataa på
  review-einingane 5560 og 5564 blir ikkje endra.
- Kontroll av JNI på fire ABI-ar: berre Android-systembibliotek som dynamiske
  avhengnader og 16 KiB ELF-segmentjustering. Faktisk emulatoravspeling testar x86_64.

## Verifisert bygg og målretta testar

- `testDebugUnitTest`: **622 testar, 0 feil**.
- `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease`: fullførte.
- `lintDebug`: **0 feil, 93 åtvaringar**. Ingen åtvaringar vart undertrykte for å få grønt bygg.
- Målretta Android-runde: **8/8 bestått**. Fire testar spelar kvar av DTS/AC3/EAC3/TrueHD
  gjennom både Emby- og Jellyfin-fixture (åtte avspelingsløp); tre testar sjekkar
  kapabilitetar/JNI, og éin sjekkar undertekstoppvarming og byte.
- Endeleg samla Android-runde: **31 valde testar, 1 feil** (PiP-retur, sjå under).
  TV-fjernkontrolltesten vart hoppa over fordi 5562 er ein mobilemulator.
  Rå resultat ligg lokalt i `app/build/playback-review/final-suite.txt`;
  den målretta grøne runden ligg i `audio-final.txt` i same mappe.
- Signert lokal release-APK: `app.reelstack`, versjonskode **96**, `0.17.0-beta17`,
  **11 062 102 byte**. Versjonsnummeret er ikkje auka: dette er ikkje ein ny publisert oppdatering.
- APK SHA-256: `b3722fadcaa00baa26fc17f96888f1497fc9b34f790dbd60d653e4431eb88a04`.
- Signeringssertifikat SHA-256:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- `zipalign -c -P 16 4` godkjende APK-en. Alle fire JNI-bibliotek og begge lisensar
  finst i APK-en. R8-mappinga bevarer JNI-klassenamn, callback og den reflekterte renderaren.
- Fullt FFmpeg-kjelde-/relenkingsarkiv er laga lokalt i
  `playback-ffmpeg/build/distribution/spole-ffmpeg-source-and-relink.tar.gz`.
  SHA-256: `f316f4128885b473bcf97d693ae5342ca495bab1e7e7c1904fd54f733dd28a22`.

Testoppsett: Android sin fullskjermintroduksjon blokkerte først vindaugsfokus og
kontrolltestar. På berre den isolerte 5562 vart `immersive_mode_confirmations=confirmed`
sett. Test-fixturen vel no serverstyrte undertekstar eksplisitt og gjenopprettar
dei opphavlege testinnstillingane etterpå, slik at lagra språkval ikkje endrar testgrunnlaget.

## Avgrensingar og attståande kontroll

Den eksisterande testen `miniPlayerKeepsVideoRunningAndReturnsWithoutRestarting`
har framleis tidsavbrot når han prøver å flytte PiP-aktiviteten fram att med eit
Intent. Avspelinga kjem inn i PiP, men testen stadfestar ikkje retur til spelande
fullskjerm. Dette er ikkje retta eller ignorert her, og den samla spelartestsuiten
skal difor ikkje rapporterast som heilt grøn. Dei målretta direkteavspelingstestane
er bestått uavhengig av denne testen.

Dette fjernar ikkje all omkoding. Ikkje-støtta video/HDR-profil, låg vald bitrate,
serverrestriksjonar og biletundertekstar som krev innbrenning kan framleis krevje det.
Lokal PGS-/DVD-undertekstvising er ikkje implementert i denne endringa.

Syntetiske servergrensesnitt er ikkje ein ende-til-ende-test av ein ekte Emby- eller
Jellyfin-installasjon. Brukaren sin konkrete film, TV/avspelingsboks og HDMI-/ARC-lydveg
må framleis verifiserast. Eit grønt emulatortestresultat er ikkje dokumentasjon på
fysisk surround, bitperfekt passthrough eller HDR på TV-en.

Ingen ny GitHub-release vart publisert som del av sjølve gjennomgangen.
Den etterfølgjande release-oppgåva brukar beta18 / kode 97; sjå
[verifikasjonsrapporten](archive/releases/VERIFICATION_v0.17.0-beta18.md) for den endelege APK-en og testresultata.
