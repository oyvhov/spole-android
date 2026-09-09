# Lokal test-APK — 0.16.0-alpha05

9. september 2026 · bygg 48 · pakke `app.reelstack`.

Inneheld minimerbar fullhøgd sidemeny frå alpha04 og kalenderens språk-/storskriftrettingar frå `867cbf6`. Signert release-bygg for oppdatering av eksisterande installasjon; ikkje ein separat debug-app. Ingen GitHub-publisering.

## Verifisert

- 279/279 einingstestar på nytt bygg; lint 0 feil / 28 eksisterande åtvaringar.
- Føregåande kjelderunde: 12/12 målretta Android-testar på mobilprofil og 17/17 på brei profil. Ikkje køyrde på nytt etter den reine versjonsendringa 47 → 48.
- Signering verifisert med eksisterande sertifikat: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Installert med oppdatering over alpha04 på review-emulatoren. Pakkeversjonen er stadfesta til 48 / alpha05. Heim viser eksisterande personleg profil, bibliotekbilete og minimert sidemeny.
- Android System UI viste mellombels ein «isn't responding»-dialog under emulatoroppstart/kontroll. Etter «Wait» kom normal heimeside tilbake. Dette er ikkje dokumentert som Spole-krasj og heller ikkje ein full fysisk ytingstest.

## Filer

APK: `app/build/test-alpha05/Spole-0.16.0-alpha05.apk`

Storleik: 3 655 513 byte.

SHA-256: `7C867D11940CE8712257BEC7B54ABA00FE5A2C0AF1A4C3FA7ED8EDC0E3DB5199`.

R8-mapping: `app/build/test-alpha05/mapping-0.16.0-alpha05.txt`. Begge filer ligg lokalt utanfor Git; skjermbiletet med ekte konto er heller ikkje lagt i Git.
