# Verifisering av Spole 0.16.0-alpha23

Versjon 0.16.0-alpha23, versjonskode 66, pakke `app.reelstack`.
## Testar

- Einingstestar: 453/453 bestått, ingen feil eller hoppa-over testar.
- Isolert TV 5566: 27/27 bestått. Navigasjon (2), TV-detaljar/bibliotek (14), spelar (11).
- Lint: 0 feil, 31 åtvaringar.
- TV-testane dekkjer normal/skjult sidebar, høgre/venstre mellom plakatar,
  aktivt menyval, første film frå verktøy, detaljretur, dobbel tekststorleik,
  tilbake frå OSD og automatisk/bevart fokus på neste episode.
- Neste episode har deterministisk framdriftslinje og tekst. Portrett og
  episodebilet-URL-ar er testa med syntetiske serverdata og utan tilgangsteikn i URL.
- Manuell TV-kontroll med ekte kontoar: kompakte episodar, retur til heil hero,
  sidemenyfokus og venstre mellom favorittplakatar. Skriftstorleik 1.0 og 2.0.
- Full Android-testpakke og fysisk TV er ikkje køyrde i denne runden.

## Artefakt og oppdatering

- Signert universal release-APK, ikkje debuggable, minimum Android 8/API 26.
- Storleik: 4 017 738 byte.
- SHA-256: `8341b029e576bd8c0cfea5cf12bece14279162e9d135dfe83d92070b67c75b69`.
- Signeringssertifikat SHA-256: `36fa94f03494f326053bdcc3d7253994950652d282e6db681cc33270b5f51a10`.
- Bygg: `BUILD SUCCESSFUL`, endeleg logg `app/build/alpha23-verified-build.log`.
- Produksjons-APK installert med `-r` på TV 5564. Kontoar og tema bevarte.
- APK, tilhøyrande R8-mapping, SHA256SUMS og SOURCE_COMMIT er arkiverte i release-assets.
- Publisert prerelease: https://github.com/oyvhov/spole-android/releases/tag/v0.16.0-alpha23
- Kjeldecommit: `38fcd7a`. Releasen er ikkje kladd og er synleg i den offentlege release-lista.
- APK lasta ned utan innlogging; SHA-256 samsvarer med bygget og GitHub-digest.
- Mobil 5560: fullførte den tidlegare nedlasta alpha22-oppdateringa frå alpha21.
  Deretter fann «Sjekk no» alpha23, lasta ned APK-en og opna Android-installasjonen.
  Play Protect-skanninga vart fullført, og installert pakke er stadfesta som alpha23/kode 66.
- Mellombels løyve til installasjon frå Spole er sett tilbake til førre standardverdi.

Sjå `TV_FEEDBACK_PASS4.md` for endringar og avgrensingar i den manuelle gjennomgangen.
