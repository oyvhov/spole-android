# UI- og kontogjennomgang — v0.8.0

Gjennomført 5. september 2026 i den native Android-appen, med reelle innloggingar på alle fem tenestene. Private skjermbilete blir ikkje publiserte.

## Prioriterte funn og løysingar

| Område | Funn | Endring |
|---|---|---|
| Konto | Ei tenestetilkopling fortel ikkje kven som eig førespurnaden | Eigne Jellyfin-/Seerr-profilar med serverstadfesta namn, bilete og synleg Seerr-identitet før handling |
| Førespurnader | Ein administratornøkkel kan representere feil person | Personleg økt er påkravd, og aktuell konto-ID blir kontrollert på nytt før sending |
| Heim | Repeterte kjeldemerke og lange overskrifter | Kortare overskrift, lite kjeldemerke per rad, ingen merke oppå plakatane |
| Oppdag | Status dekte delar av biletet | Status og handling under plakaten, tydelegare hierarki og søk |
| Detaljar | Manglande Seerr-omtale og repetert metadata | Språkreserve, fleire seriefakta, omtale før handling og heil filmplakat |
| Aktivitet | Seinare oppføringar mangla tittel og bilete | Avgrensa mellomlager og fleire metadataoppslag, utan å mellomlagre førespurnadsstatus |
| Kalender | Utydeleg mengd per dag og store kort | Dagtal, ryddigare agenda og tydelegare datoar i komande-korta |
| Rørsle | Oppfrisking kunne endre høgda på tom avspeling | Kompakt oppfrisking og sams dimensjonar mellom skjelett og innhald; fast visningshøgd på detaljarket er bevart |

## Manuell etterkontroll

- Alle fem tilkoplingar viste aktiv status etter oppdatering utan ny innlogging.
- Jellyfin og Emby leverte bilete og separate film-/episoderader.
- Søk gav både film- og serieresultat; detaljar viste omtale og metadata.
- Faktiske Jellyfin- og Seerr-profilbilete vart viste, saman med personleg identitet i Oppdag og ved handlinga i detaljarket.
- Aktivitetsdata og kalender vart kontrollerte mot det synlege innhaldet frå tenestene.

## Avgrensingar

Bygg og 109 JVM-testar bestod. Android-lint rapporterte ingen feil og 15 åtvaringar. Dei 23 utvalde UI-testane med skriftstorleik 1,3 bestod, inkludert konto, Heim, kalender, oppsett og gjennomgåande komponentar. Innstillingar og hovudflyt vart i tillegg kontrollerte manuelt med ekte data etter oppdatering på same signeringsidentitet.

Ingen ekte førespurnader, avspelingskommandoar eller køendringar vart sende. Det var ingen aktive avspelingar under denne gjennomgangen. Innlogging frå blank installasjon og feilsituasjonar blir testa isolert med testdata, ikkje ved å slette brukarøktene. Manglande omsetjing hjå metadata-kjelda kan framleis gje engelsk omtale.

Layoutspesifikasjonen ligg i [LAYOUT.md](LAYOUT.md). Automatiske UI-testar køyrer på ein eigen emulator, aldri på emulatoren med dei ekte brukarøktene.
