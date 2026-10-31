# Verifisering av Spole 1.0.0-beta28

1. november 2026. Produksjonspakke `app.reelstack`, versjonskode 165. Bygd oppå beta27 (`1cade67`) flett
med greinene `launcher-bridge` og `launcher-profiles`.

## Omfang

- Eigne profilar: Tenestene dine → Profilar → «Eigen profil for …» lagar ein vaksen profil med ein kopi av
  ein konto i hovudprofilen (`kid.<id>.adult`). Barnemodus er «ein barneprofil er aktiv»
  (`ConnectionRepository.isKidProfile`, `SessionScope.kids`). Frå barnemodus til ein eigen profil krev PIN.
- Hovudprofilen blir ikkje kalla opp etter ein konto som har eigen profil.
- Stova: godkjenn Quick Connect-koden (`LauncherApproveActivity`), framdrift ved stopp (`LauncherProgress`),
  `spole://profile`, profillista og `select_profile` (`LauncherProfilesProvider`, berre for Stova), og siste
  Tilbake frå eit bibliotek eller ein tittel Stova opna går til Stova.

## Bygg og testar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease`: BUILD SUCCESSFUL.
  1354 einingstestar, 0 feil, éin valfri nettverkstest hoppa over. Lint: 0 feil. Omsetjingar og
  release-data: OK.
- Isolert TV 5566: `LauncherLibraryIntegrationTest`, `AccountPanelsTest`, `LanguageResourcesTest` og
  `LinkedLoginTest`, 25/25.
- Native bibliotek: dei 20 `.so`-filene er byte for byte like beta27. Kjeldearkiva for FFmpeg og dei native
  undertekstbiblioteka er difor dei same som ved beta27 (same digest). `check-native-subtitles.py`: OK.
- APK: 28082972 byte, SHA-256 `380993628cd1b717d7880d9563bd997953ce0a5c0af4d71d56e6951a389a3789`, sertifikat `36fa94f0…1a10`, pakke `app.reelstack`, versjon
  `1.0.0-beta28`/165, ikkje feilsøkbar.

## På einingar (med greinbygga før flettinga)

- Shield: eigen profil for Eilev (Emby) laga i Spole; Stova-flisa opna `spole://profile` → Spole som Eilev med
  berre Emby-innhaldet hans i vanlege Spole; profilmenyen viste Øyvind (hovudkonto) og Eilev (eigen profil);
  tilbake til hovudprofilen utan PIN. Fokushoppet og tilbodet om «Eigen profil for Øyvind» vart funne her og
  retta etterpå (UI-test), men den rettinga er ikkje prøvd på Shield.
- Loftet (release, `install -r`): Stova-flisa → `LauncherOpenActivity` → Heim som Øyvind; lenkjefilteret for
  `profile` vart funne mangla her og retta. Profilbyte i Stova utan krasj.
- Emulator-5580 (debug): profillista svarar ikkje `adb shell`; Tilbake frå Spole går til Stova.

## Publisering og oppdatering

- Release `v1.0.0-beta28` publisert 1. november 2026 som prerelease med éin APK (28 082 972 byte, SHA-256
  `380993628cd1b717d7880d9563bd997953ce0a5c0af4d71d56e6951a389a3789`), mapping, `SOURCE_COMMIT.txt`
  (`680d440`), `SHA256SUMS.txt`, FFmpeg-kjelda og kjeldearkivet for dei native undertekstbiblioteka. Alle
  digestane på GitHub var like dei lokale filene. Den offentlege lista (utan token) viste utgåva som ikkje
  kladd, og den offentlege nedlastinga gav same SHA-256 som det bygde APK-et.
- Oppdatering i appen på isolert TV 5566 frå den offisielle beta27: Appoppdateringar → «Sjekk no» viste
  beta28 med release-notata → «Last ned oppdatering» → «Installer oppdatering» → Android «Update» → «App
  installed.». Installert versjonskode 165, og appen opna att med same innhald som før.
- CI «Bygg og test» for `680d440`: grøn.

