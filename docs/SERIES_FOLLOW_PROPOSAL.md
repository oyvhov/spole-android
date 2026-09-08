# Forslag: følgje ein serie utan ein overflødig førespurnad

Status: den enkle Seerr-baserte varianten er implementert etter 0.14.1, enno ikkje publisert.
Ingen serverdata er endra under testing. Resten av dokumentet tek vare på bakgrunnen for valet.

## Valt løysing

Brukaren ønskjer det minst forvirrande alternativet, utan eiga Sonarr-innlogging. Vi brukar derfor
berre vanleg Seerr-metadata og personlege, lokale varsel — ikkje eit ekstra Sonarr-oppslag.

- «Sjå sesongar» viser kva som er i biblioteket, delvis tilgjengeleg eller alt førespurt.
- Ingen sesongar er førehandsvalde. Daterte framtidige sesongar viser «Kjem …», udaterte viser
  «Premiere ikkje avklart». Berre eksplisitt val og «Send førespurnad» kan opprette noko i Seerr.
- Når kjend, står neste episode og utgjevingsdato over sesongane. Dato er ikkje bibliotektilgjenge.
- Bjølla ved delvise/alt førespurde sesongar følgjer bibliotekstatus og sender ikkje ein ny
  førespurnad. Lokale varsel er merkte «Berre varsel» og kan fjernast utan å endre Seerr.
- Ingen påstand om «blir følgd automatisk»: produksjonsstatus er ikkje overvaking. Ny sesong blir
  heller ikkje automatisk følgd av det lokale varselet for den førre sesongen.
- Ein vanleg Seerr-brukar treng verken Sonarr-adresse, administratornøkkel eller ny backend.

Dette dekkjer ikkje episodevise varsel eller innsending av problemrapportar. Manglande airedato
eller Seerr sin delvis-status seier ikkje nøyaktig kva episodar som manglar. Vi viser ikkje
«manglar allereie sende episodar» utan eit datagrunnlag som faktisk stadfestar det.

Sjå REQUEST_FLOW.md for den implementerte flyten og VERIFICATION_SERIES_FLOW.md for kontrollane.

## Funnet i Spole

`RequestSeason.canRequest` tillèt berre Seerr-status 1 og 7. Delvis tilgjengelege sesongar
(status 4) kan ikkje sendast på nytt. Sesongstatus og opne førespurnader blir henta før vising
og kontrollert igjen før POST. Dette er riktig vern og skal bevarast.

Holet: alle ordinære sesongar som kan førespørjast, blir førehandsvalde. Appen les ikkje
overvaking i Sonarr eller premieredato ved dette valet. Ein framtidig sesong kan difor bli
behandla som ein mangel sjølv om Sonarr allereie er sett opp til å følgje han.

«Returning Series» er produksjonsmetadata, ikkje bevis på automatisk henting. Tilgjenge og
overvaking må vere to separate eigenskapar; serien kan vere delvis tilgjengeleg OG overvaka.

## Tidlegare vurdert brukaroppleving (ikkje alt implementert)

| Situasjon | Vis til brukaren | Handling |
| --- | --- | --- |
| Episodar ligg i biblioteket, resten er ikkje sende enno | Tilgjengeleg no, neste episode/dato når kjend | Vis episodar |
| Sesongen er stadfesta overvaka | Blir følgd automatisk | Følg med / varsle, ikkje ny førespurnad |
| Sesongen manglar og Seerr tillèt førespurnad | Manglar i biblioteket | Vel sesong og send |
| Delvis sesong med manglande allereie sende episodar | Manglar episodar | Meld frå via Seerr der rettane tillèt det, ikkje duplisert sesongførespurnad |
| Status kan ikkje stadfestast | Sjekk sesongar | Hent på nytt; ikkje lov automatisk henting |

«Følg med» må vere eit eige, personleg varslingsabonnement, ikkje eit nytt Seerr-request-ID.
Varsel om nye episodar og varsel når ein heil vald sesong er klar må vere separate val.
Framtidige eller udaterte sesongar skal ikkje førehandsveljast som manglar. Brukaren kan
framleis aktivt velje dei når Seerr tillèt det.

## Vurdert ekstra datagrunnlag (ikkje brukt)

Seerr sin noverande kjeldekode har `GET /api/v1/service/sonarr/lookup/:tmdbId` for innlogga
brukarar. Han spør Sonarr gjennom Seerr, så klienten treng ikkje administratornøkkelen.
Resultata kan innehalde serien sin ID, TVDB-ID, `monitored`, `monitorNewItems` og sesongovervaking.
Dette er ei mogleg kjelde, ikkje verifisert mot den installerte Seerr-versjonen enno.

Viktige avgrensingar før implementering:

- Endepunktet brukar den første konfigurerte Sonarr-tenaren og søkjer på tittel. Berre ein
  eksakt ekstern ID-match er gyldig; aldri bruk første eller nærmaste namnetreff.
- Fleire Sonarr-tenarar og standard/4K må matchast mot rett tenar og ekstern serie-ID frå
  Seerr. Uklar matching skal gi ukjent status, ikkje eit automatisk-løfte.
- Overvaka serie/ sesong beviser ikkje at kvar eksisterande episode er overvaka. Seriemonitor,
  sesongmonitor, episodeunntak og innstillinga for nye sesongar er ulike felt.
- Overvaking er ikkje bevis på nedlasting eller garanti for at ei utgåve blir funnen.
- Fersk Seerr-status skal framleis avgjere om ein førespurnad er lov. Ekstra lesing skal aldri
  omgå rettar eller opne ein delvis sesong som Seerr sjølv sperrar.
- Sonarr-stiar, nøklar og andre brukarars førespurnader skal ikkje visast eller lagrast i
  følgjeinformasjonen. Ingen konto- eller tenarinnstillingar skal endrast automatisk.

## Avgrensingar som står att

Brukaren har ikkje namngjeve serien/sesongen bak den opphavlege førespurnaden. Vi kan derfor
ikkje fastslå om akkurat den ville ha blitt henta automatisk. Sonarr-overvaking blir ikkje lesen
eller endra i denne løysinga. Reell nedlasting/import og løpande episodevarsel er ikkje testa.

## Primærkjelder

- [Seerr: førespurnadsvern](https://github.com/seerr-team/seerr/blob/develop/server/entity/MediaRequest.ts)
- [Seerr: Sonarr-oppslag](https://github.com/seerr-team/seerr/blob/develop/server/routes/service.ts)
- [Seerr: tilgang til service-rutene](https://github.com/seerr-team/seerr/blob/develop/server/routes/index.ts)
- [Sonarr: serie- og overvåkingsfelt](https://github.com/Sonarr/Sonarr/blob/develop/src/Sonarr.Api.V3/Series/SeriesResource.cs)
- [Sonarr: overvaking av nye sesongar](https://github.com/Sonarr/Sonarr/blob/develop/src/NzbDrone.Core/Tv/RefreshSeriesService.cs)
