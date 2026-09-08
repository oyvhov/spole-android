# Forslag: følgje ein serie utan ein overflødig førespurnad

Status: undersøkt og foreslått, ikkje implementert i 0.14.1. Ingen serverdata er endra.

## Funnet i Spole

`RequestSeason.canRequest` tillèt berre Seerr-status 1 og 7. Delvis tilgjengelege sesongar
(status 4) kan ikkje sendast på nytt. Sesongstatus og opne førespurnader blir henta før vising
og kontrollert igjen før POST. Dette er riktig vern og skal bevarast.

Holet: alle ordinære sesongar som kan førespørjast, blir førehandsvalde. Appen les ikkje
overvaking i Sonarr eller premieredato ved dette valet. Ein framtidig sesong kan difor bli
behandla som ein mangel sjølv om Sonarr allereie er sett opp til å følgje han.

«Returning Series» er produksjonsmetadata, ikkje bevis på automatisk henting. Tilgjenge og
overvaking må vere to separate eigenskapar; serien kan vere delvis tilgjengeleg OG overvaka.

## Føreslått brukaroppleving

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

## Datagrunnlag utan eigen backend

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

## Verifisering som står att

Brukaren er spurd om konkret serie og sesong. Les akkurat den i emulatoren. Test så:
serie utan bibliotek, delvis pågåande sesong, ny sesong som kjem seinare, overvaking av/på,
fleire Sonarr-tenarar, 4K, utgått status, vanleg brukar og tilbaketrekking/avslag i Seerr.
Ingen ekte førespurnad treng sendast for å kontrollere lesinga.

## Primærkjelder

- [Seerr: førespurnadsvern](https://github.com/seerr-team/seerr/blob/develop/server/entity/MediaRequest.ts)
- [Seerr: Sonarr-oppslag](https://github.com/seerr-team/seerr/blob/develop/server/routes/service.ts)
- [Seerr: tilgang til service-rutene](https://github.com/seerr-team/seerr/blob/develop/server/routes/index.ts)
- [Sonarr: serie- og overvåkingsfelt](https://github.com/Sonarr/Sonarr/blob/develop/src/Sonarr.Api.V3/Series/SeriesResource.cs)
- [Sonarr: overvaking av nye sesongar](https://github.com/Sonarr/Sonarr/blob/develop/src/NzbDrone.Core/Tv/RefreshSeriesService.cs)
