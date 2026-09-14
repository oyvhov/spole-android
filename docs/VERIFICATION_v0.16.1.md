# Verifikasjon: Spole 0.16.1

Versjonskode 79, produksjonspakke `app.reelstack`. Ordinær utgåve frå `main`.

## Kontrollar

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease` avslutta med `BUILD SUCCESSFUL`.
- Quick Connect-einingstestane kontrollerer siste tillatne polling, treg Seerr-registrering og avbrot.
- Cache-einingstesten kontrollerer at eit gammalt serieposter ikkje blir vist i eit breitt episodekort.
- 491 JVM-einingstestar passerte utan feil. Tre Android-einingstestar for `MediaSnapshotStore` passerte på den isolerte TV-eininga.
- Lint: 0 feil og 32 eksisterande åtvaringar.
- Release-pakken er kontrollert som `app.reelstack`, versjonskode 79, versjon 0.16.1.
- Signaturen er det eksisterande Spole-sertifikatet: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.

APK-storleik, SHA-256, kjeldecommit og kontroll av offentleg GitHub-nedlasting blir lagt inn etter publisering.
