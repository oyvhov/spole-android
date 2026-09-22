# Verifikasjon — Spole 0.16.0-alpha11

Dato: 10. september 2026. [Endringar og avgrensingar](PLAYBACK_CAPABILITIES_ALPHA11.md). Lokal testversjon; ingen GitHub-publisering.

## Signert produksjonsbygg

- Pakke `app.reelstack`, versjon `0.16.0-alpha11`, bygg 54.
- Minimum API 26, target API 36, ikkje debuggable, R8-minifisert.
- APK: `app/build/test-alpha11/Spole-0.16.0-alpha11.apk`, 3 829 821 byte.
- SHA-256: `b76c11187e1a210fcbd90cd4dde38a7f9fd460ccd0868fbcba6ae6597fd4e4c6`.
- Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`, same nøkkel som tidlegare.
- R8-mapping: `app/build/test-alpha11/mapping-0.16.0-alpha11.txt`.
- Endeleg byggjelogg: `app/build/alpha11-final-build3.log`. `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` og `assembleRelease` bestod.

## Automatiske kontrollar

- **329 JVM-testar, 0 feil.** Tolv nye testar for einingsprofil, 4K/HEVC/AV1, kanalgrenser per codec, HDR-avgrensing, byte av lydutgang, feil ved deteksjon, direkte sporbyte, bitrate og avgrensa reserveforhandling. HLS kan kopiere støtta video ved lydkonvertering, men får ikkje kopiere video som feilar den konkrete dekodarkontrollen.
- Lint: **53 åtvaringar, 0 feil**. Dei 13 nye åtvaringane gjeld innlinerte codec-profilkonstantar frå API 29 i deteksjonen, ikkje ubeskytta kall til nye Android-metodar. Nyare format blir berre annonserte når plattformen faktisk har dekodaren.
- APK-identitet og signatur er kontrollerte med Android-verktøya.
- Telefon: **30 utførte avspelingstestar bestod**, éin TV-spesifikk test vart hoppa over (31 tilfelle totalt). Omfattar faktisk AVC-video, HLS, undertekstar, lydsporbyte, spoling, gjenopptaking, rotasjon, avbrot/retry, OSD og einingsdeteksjon. Logg: `app/build/alpha11-phone-final.txt`.
- Google TV: **10/10 målretta testar bestod**, inkludert faktisk AVC-video med fjernkontroll, HLS/undertekst, lyd- og kvalitetsbyte, spoling, rotasjon/gjenoppretting, avbrot/retry og einingsdeteksjon. Logg: `app/build/alpha11-tv-final.txt`.
- Profilane som vart oppdaga på dei to emulatorane er lagra lokalt som `app/build/alpha11-phone-capabilities-final.json` og `app/build/alpha11-tv-capabilities.json`. Dei inneheld ingen kontoopplysningar. Begge emulatorane har programvaredekodarar og SDR; dei annonserer ikkje Dolby-/DTS-passthrough som ikkje finst på desse lydutgangane.

## Testoppsett og utviklingsavvik

Android-testane køyrer på eigne telefon-/TV-profilar med syntetiske kontoar, lokal HTTP-tenar og faktiske videofiler. Dei ekte kontoprofilane blir ikkje sletta eller instrumenterte.

Første avspelingsrunde nådde tidsgrensa før videoen starta. Testtenaren las HTTP `Content-Length` som tal på teikn, medan feltet er tal på byte. Den nye UTF-8-profilteksten utløyste feilen; testtenaren fekk aldri ferdig lese førespurnaden. Testlesaren er retta til å lese byte før UTF-8-dekoding. Den avbrotne utviklingsrunden er ikkje rekna som bestått. Testdata fekk også faktisk video-/lydmetadata slik at dei går gjennom same kjeldekontroll som ein verkeleg Jellyfin-respons.

## Signert APK med ekte kontoar

Den arkiverte APK-en er installert med `install -r` på både den faste telefonen (5560) og Google TV (5564), utan sletting eller instrumentering av kontodata. Android stadfesta alpha11 / bygg 54 på begge. Språk, temaval og separate/kombinerte heimrader vart bevarte. Begge emulatorane er opne i synlege vindauge.

På TV opna Taskmaster S19 E09 frå den kombinerte rada med fokus på Hald fram. Den verkelege 1080p H.264-/AAC 2.0-kjelda starta frå lagra posisjon og viste video med undertekst; OSD rapporterte «Direct from Jellyfin». Pause, den nye kvalitetsmenyen (Auto / 80 / 20 / 4 / 2 Mbit/s) og fysisk Tilbake til Heim vart kontrollerte. Dette stadfestar ein faktisk direkte avspelingsveg mot den eksisterande Jellyfin-tenaren, ikkje fysisk HDR- eller surroundstøtte. Spegelvindauga har lyd avslått, så dette er ikkje ein lyttetest.

Telefonen viste først tomme rader og feil ved kontostadfesting under kald emulatoroppstart. Etter at nettverket var klart og appen vart opna på nytt, lasta ekte titlar og kontoen utan ny innlogging eller dataendringar. Detaljarket viste Taskmaster og framdrifta oppdatert frå TV-avspelinga til 50 % / 24 minutt att. Årsaka til den første kontostadfestingsfeilen er ikkje endeleg fastslått; ho er ikkje rekna som feilfri kald nettverksoppstart. Private skjermbilete og UI-uttrekk ligg berre under ignorert `app/build/`.

## Avgrensingar

Eigenskapar frå emulatorar er ikkje bevis på støtte i ein fysisk TV, lydplanke eller receiver. HDR-bilete, HDMI/eARC, Atmos, TrueHD og DTS-HD må prøvast på det aktuelle utstyret. Dolby Vision er medvite ikkje annonsert som direkte støtta i denne versjonen. Tenaren må kunne omkode format utan godkjend direkte veg. Ingen ekstern codec-pakke er lagd til.
