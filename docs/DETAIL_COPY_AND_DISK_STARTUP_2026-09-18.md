# Bibliotekdetaljar og diskoppstart

## Detaljrettingar (tekne med i beta21)

- Tilgjenge-merket er overflødig inne i biblioteket. `DetailAside` skjuler det for
  Jellyfin og Emby, men bevarer det når ein oppdagar tilgjengeleg innhald via Seerr.
- Emby blir no behandla likt med Jellyfin ved skjuling av den ekstra bibliotekstatusen
  nedst i detaljarket. Feil og avspelingshandlingar er ikkje fjerna.
- Eit reint numerisk `OfficialRating` i aldersområdet 0–21 får lokaliserbar eining:
  «15 år» på nynorsk/bokmål og «Age 15» på engelsk. Kodar som PG-13/TV-MA/NO-15
  blir bevarte, ikkje omsette til ei norsk aldersgrense. Manglande grense blir ikkje funnen på.
- UX-tekstprinsippa er brukte til å fjerne overflødig status og leggje meining til eit
  einsleg tal, utan nytt ikon eller farge som brukaren må lære.

## Diskoppstart: kodefunn i beta20, ikkje ei ny avspelingsretting

- `MediaPlaybackClient` bruker tilkoplingsgrense 5 sekund og lesegrense 8 sekund for
  API-kall, også POST til `Items/{id}/PlaybackInfo`. POST blir ikkje automatisk repetert
  i `HttpTransport`. GET kan bli prøvd éin gong til ved IO-feil.
- Videoklienten har tilkoplingsgrense 8 sekund og lesegrense 20 sekund på Jellyfin,
  45 sekund på Emby. Ei lesegrense gjeld venting på data, ikkje total fil-/filmlengd.
- Vakthaldet stoppar etter meir enn 45 sekund samanhengande `STATE_BUFFERING` og sender
  Stopped. Dette er ikkje det same som ei garantert oppstartstid på 45 sekund: eit
  tidlegare API-kall kan allereie ha feila.
- Undertekstuthenting har eiga 8-sekunds kallgrense; beta20 lèt videoen halde fram med
  varsel dersom teksten feilar.
- Konklusjon: tidsgrensene gir ingen garanti for ein sovande disk. Særleg API-lesegrensa
  kan vere for kort dersom serveren ventar på disken før PlaybackInfo blir svart på.
  At eit nytt forsøk fungerer kan passe med dette, men er ikkje bevis utan tenarlogg/timing.
- Ingen ventetider, omkodingsreglar, diskinnstillingar eller serverar er endra i denne
  oppgåva. Eventuell oppstartsretting må skilje første byte frå stopp under avspeling,
  bevare brukarens Play/Pause-val og ha kansellering som ikkje gjer Tilbake treg.

## Verifisering

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: BUILD SUCCESSFUL,
  exit 0, 5 minutt 49 sekund. Lint: 0 feil, 93 åtvaringar. `git diff --check` er rein.
- JVM: 630/630, null failures/errors.
- Android, isolert 5562: 4/4 (7,41 sekund). Fullt detaljark for begge medietenestene
  skjuler overflødig status; aldersmerkinga er synleg ved skriftstorleik 2.0; Seerr
  bevarer tilgjenge-merket og delvis/ventande/blokkert status er framleis skilde.
- Ingen fysisk diskoppstart eller brukarens filmfiler er testa. Dette var den lokale
  føretesten; sjå `VERIFICATION_v0.17.0-beta21.md` for release-kontrollen.
