# Spole alpha11 — codec, 4K og lyd

Lokal oppdatering etter alpha10. Versjon 0.16.0-alpha11, bygg 54. Ingen GitHub-publisering.

## Kva er endra?

Spole brukte før same H.264/1080p/stereo-profil på alle einingar. No blir profilen laga frå Android sine dekodarar, skjermen sine HDR-eigenskapar og lydutgangen Media3 oppdagar. Eigenskapane blir lesne på nytt ved kvar avspelingsforhandling, slik at ei ny lydplanke eller eit hovudsett ikkje blir verande skjult bak ein gamal profil.

- H.264, HEVC/H.265, AV1 og VP9 blir annonserte når eininga har ein dekodar. Oppløysing, profil, nivå og bitdjupn blir avgrensa etter det dekodaren rapporterer. Reine programvaredekodarar blir ikkje automatisk annonserte som 4K-spelarar; desse blir avgrensa til 1080p.
- Før direkte avspeling blir også den faktiske videostraumen kontrollert, inkludert storleik, biletfrekvens, bitrate og profil. Dette fangar kombinasjonar som ikkje kan avgjerast frå separate breidde-/høgdegrenser.
- HDR10 og HLG krev både ein passande 10-bit-dekodar og ein skjerm som rapporterer støtte. HDR10+ krev i tillegg den relevante dekodarprofilen. Dolby Vision blir førebels sendt til kompatibilitetsvegen; HEVC Main10 blir ikkje brukt som bevis på Dolby Vision-støtte.
- AAC, MP3, AC3, EAC3, DTS/DCA, TrueHD, FLAC, Opus og Vorbis blir vurderte kvar for seg. Antal kanalar blir ikkje lenger låst til to. Komprimert lyd kan gå direkte til lydutgangen når Media3 rapporterer at passthrough er støtta; elles kan ein tilgjengeleg Android-dekodar spele lyden. Telefonhøgtalarar blir sjølvsagt ikkje surroundhøgtalarar av dette.
- DTS-HD blir kontrollert som DTS-HD når kjeldemetadata viser HD. Media3 kan bruke DTS-kjernen dersom lydutgangen berre støttar denne; dette er ikkje ein garanti om tapsfri DTS-HD på alt utstyr. Atmos er avhengig av det faktiske lydformatet og mottakaren, ikkje eit generelt påslått Atmos-flagg.
- Filbehaldarar blir skilde: AV1/VP9 blir ikkje annonserte som støtta video i MPEG-TS. HLS kan kopiere kompatibel H.264/HEVC-video medan Jellyfin tilpassar lyd, når tenaren tillèt det.
- Val av lydspor eller tekstundertekst tvingar ikkje lenger automatisk fram omkoding. Biletundertekstar som PGS brukar framleis innbrenning gjennom Jellyfin.

## Kvalitet og reserveavspeling

Automatisk bitrate var 12 Mbit/s på Wi-Fi og 4 Mbit/s på alle andre nett, inkludert kabla TV. No er grensa 120 Mbit/s på nett Android markerer som ikkje takserte, og 4 Mbit/s på takserte/ukjende nett. Dette er ei grense, ikkje ei måling av kor raskt nettet er. Kvalitetsmenyen har i tillegg manuelle val på 80, 20, 4 og 2 Mbit/s.

Media3 kan prøve ein annan tilgjengeleg dekodar ved initialiseringsfeil. Dersom den konkrete fila ikkje blir godkjend eller avspelinga får ei relevant codec-/lydutgangsfeil, brukar Spole ein avgrensa kompatibilitetsveg: H.264, SDR, maksimalt 1080p og AAC/stereo gjennom Jellyfin, utan kopiering av den avviste straumen. Bitrateval, mediekjelde, valde spor og avspelingsposisjon blir bevarte. Det er ingen uendeleg forsøkssløyfe. Tenaren må framleis tillate og klare omkoding.

Dette legg ikkje til ein FFmpeg-/VLC-pakke i APK-en. Spole brukar Media3 1.10.1 og Android sine dekodarar; omkoding skjer på Jellyfin-tenaren.

## Kontroll

Den signerte APK-en er bygd og kontrollert: 329 JVM-testar, 30 utførte avspelingstestar på telefon og 10 på Google TV bestod. Lint har 0 feil. Den arkiverte APK-en er installert som oppdatering på begge review-emulatorane, med eksisterande kontoar og visingsval bevarte. Ekte Jellyfin-avspeling av Taskmaster (1080p H.264 / AAC 2.0) vart stadfesta på TV med undertekst, gjenopptaking og status «Direct from Jellyfin». Telefonen viste ekte bibliotekdata og oppdatert framdrift.

APK: `app/build/test-alpha11/Spole-0.16.0-alpha11.apk`. [Full verifikasjon, avvik og signatur](VERIFICATION_v0.16.0-alpha11.md). Fysiske HDMI-/eARC-kjeder, Dolby-/DTS-mottakarar og langvarig 4K/HDR-avspeling er ikkje verifiserte av emulatorane.

## Fagleg grunnlag

- [Android Media3: formatstøtte](https://developer.android.com/media/media3/exoplayer/supported-formats).
- [Media3 1.10.1: AudioCapabilities](https://github.com/androidx/media/blob/1.10.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/AudioCapabilities.java).
- [Jellyfin: vilkår i ein avspelingsprofil](https://github.com/jellyfin/jellyfin/blob/master/MediaBrowser.Model/Dlna/ConditionProcessor.cs).
- [Jellyfin Android TV: einingsprofil og HDR-avgrensingar](https://github.com/jellyfin/jellyfin-androidtv/blob/master/app/src/main/java/org/jellyfin/androidtv/util/profile/deviceProfile.kt).
