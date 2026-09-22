# Pixel-lyd og TV-biletfrekvens etter beta18

Brukaren melder at Emby fungerer godt på Shield TV med Sonos Playbar via optisk,
at Jellyfin går direkte men hakkar på TV, og at fleire titlar ikkje startar på Pixel 9 Pro.
Pixel-biletet viser `ERROR_CODE_DECODING_FAILED · audio/eac3 → AUDIO_ONLY`.
Det identifiserer lydformatet, men ikkje dekodaren eller den underliggjande feilen.

## Retta lokal lydreserve

Beta18 la til FFmpeg, men valde framleis plattformdekodaren først. Feil under sjølve
dekodinga gjekk rett til serveromkoding. `setEnableDecoderFallback` dekkjer feil ved
initialisering av MediaCodec, ikkje eit generelt byte til ein annan renderer under avspeling.

- Plattform og fungerande passthrough er framleis førsteval, for begge tenestene.
- Ved ei stadfesta MediaCodec-lyddekodingsfeil får formatet eitt forsøk med lokal FFmpeg,
  dersom biblioteket faktisk støttar formatet og sporet ikkje er kryptert.
- Nye medieperiodar blir laga frå same mediekjelde. Ingen ny `PlaybackInfo`, serverøkt,
  bitrate, container eller videokodek blir kravd for dette forsøket.
- Posisjon, pause, lydval og undertekstval blir bevarte. Tilstanden blir nullstilt ved ny tittel.
- Feil frå FFmpeg, video, transport og lydutgang blir ikkje forveksla med ein plattform-
  lyddekodingsfeil. Reserveforsøket kan ikkje gå i ring. Eksisterande serverreserve står att.
- Stats viser namnet på faktisk dekodar, tal på lydavbrot og tapte biletrammer også når talet er null.
  Feilteksten tek no med renderer-namn. Lange diagnosar kan rullast ved stor skrift.

## Retta TV-tilpassing

To kodefeil kunne gi ujamn biletkadens sjølv med full buffer:

1. Ukjend `Format.frameRate` oversåg biletfrekvensen som Emby/Jellyfin allereie hadde
   oppgitt for originalfila. Direkteavspeling tek no vare på gyldig kjeldemetadata;
   frekvens frå faktisk videodekodar har førsteprioritet. Omkoda video arvar ikkje kjeldefrekvens.
2. TV-valet aksepterte berre nær 1:1-frekvens og kunne endre oppløysing. Det godtek no
   heiltalsmultiplar (t.d. 25 fps på 50 Hz), held på ein kompatibel gjeldande modus,
   bevarer oppløysing og skil mellom 23,976 og 24. Opphavleg skjermpreferanse blir òg
   gjenoppretta når han var Android-standarden 0.

Dette er **ikkje stadfesta årsak til brukarens Jellyfin-hakking**. Bufferstopp, tapte
biletrammer, lydavbrot og skjermkadens er ulike problem. Ingen vilkårleg bufferauke,
tvungen videoomkoding eller generell deaktivering av TV-passthrough er lagd til.

## Kontroll

- 631 einingstestar: 0 feil.
- Endeleg Gradle-køyring fullførte `testDebugUnitTest`, `assembleDebug`,
  `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease`: BUILD SUCCESSFUL.
  Lint rapporterer 0 feil og 93 åtvaringar. Release inkluderer R8-optimalisering.
- Lokal signert release-APK: `app/build/outputs/apk/release/app-release.apk`,
  11 063 174 byte, SHA-256
  `45a92d59b86fddda270cbab82381aea42bce76286a2c041cdff500e9d4c5db47`.
  `apksigner verify --print-certs` stadfestar eksisterande produksjonssertifikat
  (`36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`);
  `zipalign -c -P 16 4` bestod. Dette er eit lokalt kontrollbygg med uendra
  beta18-versjon/kode 97, ikkje ei publisert eller versjonert appoppdatering.
- 12 målretta Android-testar på isolert 5562: 12 bestått, 0 feil/hoppa over.
  Tre nye recovery-testar køyrer kvar mot begge server-fixturane: injisert AAC-/EAC3-
  rendererfeil, reell FFmpeg-dekoding etterpå, spoling, pause og undertekstbevaring.
  Dei stadfestar ingen ny PlaybackInfo, ingen server-Stopped og ingen HLS-nedgradering.
- DTS, AC3, EAC3 og TrueHD går framleis direkte i fire testar, kvar mot begge tenestene.
- JNI/kapabilitetar og avgrensa/nullstillbar reserve er testa; diagnosar er testa med 2× skrift.
- Endeleg samla målretta runde på siste debug-bygg: **29/29 Android-testar bestått**,
  ingen feil eller hoppa over. Inkluderer dei nye testane, åtte ekstra regresjonstestar
  for HLS, spoling/rotasjon, lyd/kvalitet, kontobyte, pause, neste episode, undertekstar
  og nettverksretry, samt heile spelaren sin UI-testklasse.
- Ei tidlegare ekstrarunde vart avbroten av ein emulator-omstart. Android rapporterte
  `kernel_panic,ext4-fs_(device_dm-55):_panic_forced_abugr_error`; ADB bad om godkjenning
  på nytt. Ingen userdata vart reparert, sletta eller nullstilt. Etter gjenoppretta
  tilgang vart heile den målretta runden køyrd på nytt og fullført som 29/29.
- Rå testresultat ligg berre lokalt under `app/build/playback-review/pixel-recovery-*.txt`.
- Ingen instrumentering på review-profilane 5560/5564, ingen sletta kontoar eller appdata.
- Fysisk Pixel, Shield/optisk/Sonos og faktisk Jellyfin-fil er ikkje reproduserte lokalt.
  Dei nye testane simulerer rendererfeilen; dei beviser ikkje kva som feila på Pixel.
- Den første rettingsrunden hadde ingen release eller versjonsauke. Etter brukarens
  publiseringsbeskjed blir rettingane versjonerte som beta19 (98); sjå
  [release-verifikasjon](archive/releases/VERIFICATION_v0.17.0-beta19.md). Beta18-asseten er ikkje bytt ut.
  Den tidlegare kjende PiP-returfeilen er ikkje del av denne rettinga.

## Primærkjelder

- [Media3 1.11.1 DefaultRenderersFactory](https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultRenderersFactory.java)
- [MediaCodecAudioRenderer](https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/MediaCodecAudioRenderer.java)
- [Android: frame rate](https://developer.android.com/media/optimize/performance/frame-rate)
