# Verifisering av Spole 1.0.0-beta35

2. november 2026. Produksjonspakke `app.reelstack`, versjonskode 172. Bygd oppå beta33 (`74212ef`).

## Omfang

`74212ef..e8925b9` på `main`: toppmenyen på TV.

- `c95668f`: beta32-arbeidet frå `C:/JellyBin/spole-sesong`. Menyen følgjer «Sider og meny» (`TopMenuEntries.kt`),
  og biblioteka står i menyen utan festing (`LibraryCoordinator.loadMenuLibraries`). Biletet på Heim og
  biblioteksida går til toppen. Profilmenyen opnar på profilen i bruk, og fokus går til sida når ei flis
  forsvinn. «Neste» har Heim-korta, og episodar i søket er 16:9.
- `325680a`, `980646b`, `668117a`: lina er små ord på biletet utan kapsel, stripe eller merke, og har ei aksentstrek
  under sida du er på. Ho rullar vekk med sida (`TopMenuLift`, `LendTopMenuLift`) på Heim, biblioteksida, i
  rutenettet inne i eit bibliotek og på smarthyllene. Eit bibliotek opnar med lina øvst.
- Står att: Oppdag, Nedlastingar og Innstillingar har lina fast øvst (`BelowMenu`).

Versjonsnamnet beta34 vart brukt til eit lokalt, release-signert testbygg (versjonskode 171) som ligg på
Loftet-TV-en. Denne utgåva heiter difor beta35 med kode 172, så Loftet kan oppdatere seg i appen.

## Bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`: BUILD SUCCESSFUL (11 min 32 s).
- `testDebugUnitTest`: 1 620 testar, 0 feil, 1 hoppa over (den valfrie nettverkstesten).
- `lintDebug`: 0 feil, 218 åtvaringar, 1 hint (same tal åtvaringar som beta33).
- `check-release-consistency.py`: OK for 1.0.0-beta35.
- `TvTopBarScrollTest` (ny): lina står øvst i ro, rir opp med sida og kjem att, og fjernkontrollen hentar ho fram.
  Han feilar når lina ikkje blir flytta.
- Android-testane på dei isolerte emulatorane (5562/5566) er **ikkje** køyrde: eigaren har bede om at det blir
  testa på den ekte TV-en Loftet, ikkje på emulator.
- Loftet med det signerte testbygget beta34 (171, same kjelde som denne utgåva): Heim viser små ord på biletet utan
  stripe, lina er borte etter éin Ned og kjem att med Opp (fokus på «Hjem»). Filmar opnar med lina øvst, ho rullar
  vekk i rutenettet og kjem att på toppen.

## Artefakt

- `Spole-v1.0.0-beta35.apk`: 28 139 820 byte, SHA-256
  `9a1fa2a6f1314f0126b9e07249ddb23dd6db686ae00cd8143c93da3108fc8b11`.
- `app.reelstack`, versjonskode 172, versjonsnamn `1.0.0-beta35`, minSdk 26, targetSdk 36, ikkje debuggable.
- Sertifikat SHA-256 `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10` (det faste).
- `check-native-subtitles.py`: OK (fire ABI-ar, 16 KB-justering, åtte lisensar). Dei 20 native biblioteka har same
  CRC som i beta33, og ingen native kjelde er endra. Kjeldearkiva for FFmpeg og dei native undertekstbiblioteka er
  difor dei same som i beta31 og beta33 (same digest).

## Publisering

- Release-commit `4ad2b04`, tagg `v1.0.0-beta35`, pusha med `git push --atomic origin main v1.0.0-beta35`.
- Seks assets med digest kontrollert mot GitHub: APK, `SHA256SUMS.txt`, mapping, `SOURCE_COMMIT.txt`,
  FFmpeg-kjelda og kjeldearkivet for dei native undertekstbiblioteka. Releasen er publisert som
  prerelease (ikkje `latest`), og den offentlege nedlastinga utan innlogging har same SHA-256 som bygget.
- **Oppdatering i appen på ein ekte TV:** Loftet (Google TV Streamer) med det lokale beta34-bygget (171):
  Innstillinger → Varsler og oppdatering → Appoppdateringer → Sjekk nå fann `v1.0.0-beta35` (26,8 MB) med
  release-notata, Last ned oppdatering → Installer oppdatering → Android sin «Oppdater» → «Appen er installert».
  Etterpå: versjonskode 172, `1.0.0-beta35`, og Spole opnar på Heim med den nye toppmenyen, kontoen til Øyvind
  og «Fortsett å se» med ekte innhald.
- Merk: det første trykket på «Installer oppdatering» opna Android sin pakkeinstallasjon i ei gammal oppgåve
  (task 193), som lukka seg utan dialog. Det andre trykket viste dialogen. Ikkje undersøkt vidare.
