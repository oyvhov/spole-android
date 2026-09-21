# Spole 0.17.0-beta40 – verifikasjon

Dato: 2026-09-21

## Resultat

- Versjon: `0.17.0-beta40`, versionCode `119`
- APK, signatur, GitHub-URL og commit blir fylte inn etter det endelege release-bygget.
- APK-signatur skal vere den eksisterande Spole-signaturen:
  `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`

## Endringar

- Responsiv og ryddigare barnemodus for mobil og nettbrett.
- Eigne bibliotekssider med filter og korte siste-rader på framsida.
- Valfri PIN for retur til vaksenmodus og val for biblioteksnamn under bilete.
- Lokal avspelingsframdrift ventar til reell avspeling, medan Jellyfin/Emby framleis eig synk-status.

## Test og bygg

- `git diff --check`: bestått.
- `testDebugUnitTest`: bestått etter endringane.
- Relevante profil-, preferanse- og barneskjermtestar: bestått.
- Endeleg releasekommando, lint, Android-testar, signatur, APK-hash og GitHub-digest blir
  dokumenterte her før publisering.
