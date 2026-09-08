# Verifisering av Spole 0.14.2

8. september 2026. Sesongflyten frå `c6cf2fd`, med nytt versjonsnummer for publisering.

## Bygg og testar

- Reint bygg: `clean testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease` bestod.
- 238/238 einingstestar, ingen feil eller hoppa over.
- 100/100 Android-testar på isolert emulator-5562, 222,773 sekund. Endeleg debug- og test-APK for 0.14.2 vart installerte før testen.
- Lint: 0 feil, 23 åtvaringar. Release sin obligatoriske lint-kontroll bestod.
- Sesongval, delvis tilgjenge, eigarskap, lokale varsel utan ny førespurnad, kontobytte, popup-rørsle og stor skrift er dekte.

## Oppdatering med ekte data

Den signerte APK-en vart installert med `-r` på den permanente emulator-5560, utan avinstallering eller sletting av appdata. Installering og kald oppstart bestod. Android stadfesta versjon 0.14.2 / 41. Profil og bibliotekinnhald var framleis synlege etter oppdateringa; ingen ny innlogging vart kravd.

Detaljert kontroll av Silo og Lioness og avgrensingane ved varslingsflyten er dokumenterte i [sesongverifiseringa](VERIFICATION_SERIES_FLOW.md). Ingen ekte førespurnader eller tenarendringar vart sende under release-kontrollen. Dette er ikkje ei ende-til-ende verifisering av nedlasting eller bakgrunnsvarsel på fysisk telefon.

## Artefakt

- Fil: `Spole-v0.14.2.apk`, 2 268 108 byte.
- Pakke: `app.reelstack`.
- Versjon: 0.14.2, versionCode 41; Android API 26–36 (min/target).
- APK SHA-256: `efe583564ab99eaed7c1bb0770b28071c43d394cf8cd1be1f6b4616b870edda0`.
- Signatur verifisert med apksigner. Sertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- APK, `SHA256SUMS.txt` og den nøyaktige R8-mappinga `mapping-v0.14.2.txt` er klargjorde som release-vedlegg.
- Skjermbiletet frå ekte konto er berre lagra lokalt i ignorert byggmappe og skal ikkje publiserast.
