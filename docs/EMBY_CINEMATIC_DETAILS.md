# TV-detaljar og Emby-avspeling

Implementert og kontrollert 15. september 2026. Samla i release 0.17.0-beta04 (83).
Sjå `VERIFICATION_v0.17.0-beta04.md` for endeleg release-verifikasjon. Testane under
er historikk frå dei lokale beta03-bygga før publisering.

Siste justering: høgare TV-hero i full breidd, eige bakgrunnslag på detaljsida,
S2 - E2 i episodeetikettar og venstrejustert OSD-logo. Dei store bibliotekfanene er
erstatta av ein diskret meny som hugsar siste tenar, med automatisk reserveval når
berre éin tenar er tilkopla.

## Endringar

- TV har ei felles film-/serieside med stor bakgrunnskunst, logo med tekstreserve,
  metadata, utvidbart samandrag, avspeling, trailer og personlege handlingar.
- Sesongar, episodar og medverkande ligg under hovudinnhaldet. Lyd, undertekst og
  filversjon kan veljast før start. Mobilen bevarer detaljarket sitt.
- Sterk kontrast bevarer kunsten til høgre og legg tettare bakgrunn bak teksten.
  Valet Bakgrunnskunst kan framleis slå kunsten av.
- `MediaPlaybackClient` deler avspelingsforhandling, sporval og rapportering mellom
  Jellyfin og Emby. Eksisterande Jellyfin-kall og aktivitetsnamn er kompatible.
- Emby stadfestar den lagra brukar-ID-en via `Users/{Id}`, aldri `Users/Me`.
  Tenarkjelde følgjer detaljknapp, episodekort, spelar, bilete og lokal framdrift.
- Neste episode, heimrad og valfri Android TV Watch Next støttar begge tenarane.
  Lokal framdrift og launcher-lenkjer blir skilde etter konto og tenar.
- Emby-intro og rulletekst bruker kapittelmarkørar. Jellyfin bevarer sine eksisterande
  segment-/plugin-kall. Fråvær av markørar gir ingen hopp-knapp.

## Vidare integrasjon: bibliotek og vurderingar

- Studio, produksjonsselskap og rå seriestatus er fjerna frå faktafeltet. Detaljsvaret
  erstattar gamle fakta, slik at utdaterte studiofelt ikkje blir fletta tilbake.
- `CriticRating` frå Jellyfin/Emby blir vist som Rotten Tomatoes-prosent på TV og mobil.
  Null, manglande og ugyldige verdiar blir skilde: 0 % er gyldig, fråvær gir inga linje.
  Det eksisterande valet «Vis vurderingar» styrer både stjerner og kritikarvurdering.
- Ingen ekstern nøkkel eller ny tredjepart får data frå Spole. Tenaren må sjølv ha
  kritikarvurderinga i metadata, til dømes gjennom OMDb. Den generelle stjernescoren
  blir ikkje feilmerkt som IMDb eller TMDB. Direkte oppslag mot IMDb/Metacritic eller
  Rotten Tomatoes og publikumsscore er ikkje implementert.
- Bibliotek har tenestefaner når begge tenarar er tilkopla; med berre Emby blir Emby
  vald automatisk. Mapper, filtrering, sidevis lasting, kunst og detaljopning bruker
  den valde tenaren. Pågåande bibliotekjobbar blir avbrotne ved tenestebyte.
- Bibliotekval og snarvegar blir lesne og lagra for den aktuelle kontoen. Heimradene
  for Jellyfin og Emby er framleis separate. Bibliotekhuben viser berre titlar frå
  den valde tenaren, også når begge har identiske mappe-ID-ar.
- Emby utan lagra profil-ID blir avvist i biblioteklesaren; ingen `Users/Me`-førespurnad
  eller tilfeldig profil blir brukt. Eksisterande eksplisitte administratorkall er bevarte.

Emby-teamet omtalar kritikarvurderinga som Rotten Tomatoes i
[denne forklaringa](https://emby.media/community/topic/99293-sort-by-critic-rating-not-working/).
Visinga speglar metadata på tenaren, ikkje eit direkte eller uavhengig stadfesta RT-oppslag.

## Siste verifikasjon: bibliotek og vurderingar

- Fullt bygg med einingstestar, debug, Android-testpakke, lint og signert release bestått.
- **519/519 einingstestar**, inkludert Emby-bibliotek, profilkrav, filter og kritikarverdiar.
- Isolert TV 5566: **19/19** (`TvRefinementUiTest`, `TvLibraryRegressionTest`).
  Dei nye testane kontrollerer tenestefaner, Emby som einaste tenar og vis/skjul av
  Rotten Tomatoes ved skriftstorleik 2.0.
- Isolert mobil 5562: **10/10** (`SheetInteractionTest`).
- Lint: **0 feil**, 66 åtvaringar. Signatur identisk med eksisterande produksjonsapp.
- Signert `install -r` på ekte TV-profil 5564. Kontoar og utsjånadsval bevarte.
- Ekte Emby-Furiosa: studiofelt borte, serverlevert Rotten Tomatoes **90 %** synleg.
  Ekte Emby-bibliotek: filmhub, «Bla i alt», filmrutenett og opning av «12 edsvorne menn»
  med Emby-kjelde og serverlevert **100 %** kontrollert. Byte tilbake til Jellyfin
  opnar bibliotekrota att med Jellyfin-innhald. Ingen nye kontoar eller nullstilling.
- Private skjermbilete: `app/build/spole-ratings.png` og `app/build/spole-library.png`.

Siste APK SHA-256: `c2448cf018d9c7f4be64e38b97a082971189ed24e3f091737a014a3067692e57`.
Ikkje publisert. Verifikasjonen under gjeld førre lokale APK og blir ståande som
historikk for avspelingsarbeidet; han er ikkje ein ny komplett format-/utstyrstest.

## Førre verifikasjon: avspeling og filmatiske detaljar

- Bygg: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug assembleRelease` bestått.
- Einingstestar: **514/514**, ingen feil. Nye Emby-testar dekkjer profilstig,
  feil identitet/rettar, filversjon og spor, omkoding, biletundertekst,
  framdriftsrapportering, sesonggrense og kapittelmarkørar.
- Isolert TV 5566: **22/22**. TV-layout/fokus, skriftstorleik 2.0, Emby-knapp,
  pikselkontroll av bakgrunn ved sterk kontrast, lokal kontoseparasjon,
  Watch Next for begge tenarar og episode-/serienavigasjon.
- Isolert mobil 5562: **19/19**. Spelarmenyar, undertekst av/på,
  stor skrift og regresjonstestar av detaljark og opningsrørsle.
- Den første køyringa av TV-testane på mobilprofilen hadde tre feil knytte til
  TV-dialog/fokus. Dei passerer på TV-profilen. Ein fleirtydig undertekstselektor
  i spelartesten vart presisert; den endelege mobilkøyringa er grøn.
- Lint: **0 feil**, 66 åtvaringar. `git diff --check` er rein.
- Signert APK installert med `install -r` på lagra TV-profil 5564.
  Eksisterande kontoar og utsjånadsval vart bevarte.
- Ekte Emby-film opna frå favorittar: video synleg, tidslinje fram til 0:13,
  pause og undertekstmeny med verkelege SUBRIP-spor. Engelsk spor vart valt.
  Ingen generell påstand om høyrbar lyd eller synleg undertekst på filmens intro.
- Endeleg TV-design kontrollert visuelt på ekte Jellyfin-episode og Emby-film.
  Private review-bilete ligg berre i ignorert `app/build/`.

APK SHA-256: `986f130bb8ec29f33540d2ab1936da8a1ae3932577128bd1334f70499c7b309f`.

## Praktiske grenser

Formatstøtta følgjer Media3, Android-dekodarane og tenaren sine omkodingsløyve.
HDR/Dolby Vision, alle surroundformat, alle undertekstformat og ekte automatisk
episodeskifte mot Emby er ikkje fullstendig verifiserte på fysisk TV-utstyr.
Live-TV, musikk, ISO-/platemenyar og offline-nedlasting inngår ikkje i denne flyten.

API-grunnlag: [Emby-autentisering](https://dev.emby.media/doc/restapi/User-Authentication.html),
[avspelingsinformasjon](https://dev.emby.media/reference/RestAPI/MediaInfoService.html)
og [rapportering av avspeling](https://dev.emby.media/doc/restapi/Playback-Check-ins.html).
