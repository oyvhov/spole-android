# APK · 0.16.0-alpha22

Bygd 13. september 2026 etter TV-gjennomgang 3. Publisering av det same
verifiserte artefaktet er autorisert av brukaren. Offentleg kontroll blir ført inn etter publisering.

- Pakke: `app.reelstack`, versjonskode 65, versjon `0.16.0-alpha22`.
- Universal release-APK: 4 001 354 byte, ikkje debuggable.
- SHA-256: `d9d6c0f439c7c0ec03a53cdc577ed3d46dd9c93fed56056d71d3ee87dc69b4bc`.
- Signeringssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- `testDebugUnitTest lintDebug assembleRelease`: BUILD SUCCESSFUL, 4 minutt 59 sekund.
- 451/451 einingstestar består. Lint: 0 feil, 31 åtvaringar.
- UI-koden er uendra sidan 41 TV-kontrollar og 11 mobil-/nettbrettkontrollar
  bestod før versjonsbytet; sjå `TV_FEEDBACK_PASS3.md` for detaljar og avgrensingar.
- Signert APK installert over versjonskode 64 på ekte TV-profil 5564 med `-r`.
  Appen opnar med lagra profil, tema og menyval. Ingen appdata nullstilte.
- APK, kontrollsum og R8-mapping er arkiverte under
  `app/build/release-v0.16.0-alpha22/` utanfor Git.

Bygglogg: `app/build/alpha22-apk-build.log`.

## Publisert og kontrollert

- Publisert som prerelease `v0.16.0-alpha22`, ikkje draft eller stabil latest.
- Kjeldecommit: `32a0ba1` (release-taggen er uendra).
- Offentleg release-liste utan autentisering viser utgåva og nøyaktig éin APK.
- GitHub-digest, byte-storleik og SHA-256 av separat offentleg nedlasting samsvarer.
- Mobilprofil 5560 med alpha21 finn alpha22 gjennom «Sjekk no», lastar ned og
  kontrollerer APK-en, og opnar Android sin oppdateringsdialog etter kjeldeløyve.
- Fullført installasjon gjennom Android-dialogen er ikkje stadfesta: etter trykk
  på Update rapporterte emulatoren framleis kode 64. System UI hadde også vist
  eit heng før testen. Nedlasting og oppdagingsflyt er verifiserte; ferdig
  installasjon er berre stadfesta gjennom `install -r` på TV-profilen.
- Det mellombelse kjeldeløyvet på mobilprofilen er sett tilbake til default.

Release: https://github.com/oyvhov/spole-android/releases/tag/v0.16.0-alpha22
