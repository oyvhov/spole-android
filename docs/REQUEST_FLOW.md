# Personlege førespurnader

## Flyt

1. Opne ein tittel i Oppdag. Filmar har «Legg til»; seriar har «Sjå sesongar».
2. Arket viser plakat, tittel og den verifiserte Seerr-kontoen som sender.
3. Bibliotekstatus blir henta på nytt. Ingen sesongar er valde på førehand: vel berre dei du vil leggje til.
   Komande sesongar får premieredato; udaterte sesongar blir ikkje framstilte som allereie utgjevne.
   Tilgjengelege, delvis tilgjengelege, blokkerte og alt førespurde sesongar kan ikkje sendast på nytt.
4. «Varsle når det er klart» er på som standard og kan slåast av før sending. Android kan be om varslingsløyve; avslag stoppar ikkje sjølve førespurnaden.
5. Berre «Send førespurnad» sender til Seerr. Tomt sesongval, lasting, feil og pågåande sending sperrar knappen. Konto og sesongtilgjenge blir kontrollerte igjen rett før sending.
6. Etter vellukka sending opnar Aktivitet. «Mine» viser eigne førespurnader og varslingsval.

## Følgje utan å førespørje

Sesongar med Seerr-status ventar på godkjenning, førespurd eller delvis i biblioteket får ei bjølle.
Ho lagrar eit personleg varsel på denne eininga. Ho sender aldri POST/DELETE til Seerr og endrar
ikkje overvaking, søk eller automatisk henting. Ein vanleg Seerr-brukar kan følgje tilgjenge sjølv
utan løyve til å sende nye førespurnader; serveren avgjer framleis kva metadata kontoen får lese.

- Eit lokalt varsel er merkt «Berre varsel» i Aktivitet og har inga førespurnadsframdriftslinje.
- «Slutt å følgje» fjernar berre det lokale varselet. Dette er ikkje «Trekk tilbake».
- Ein eigen førespurnad for nøyaktig same sesong blir brukt om han alt finst, slik at bjølla ikkje
  opprettar ei ekstra rad. Varsel for éin sesong og ein samla fleirsesongsførespurnad har ulike
  vilkår; appen endrar ikkje varslingsvalet for heile gruppa automatisk.
- Sesongen får ikkje tilgjengevarsel før Seerr melder AVAILABLE. Dette er ikkje eit abonnement
  på kvar ny episode, og Seerr sin status er ikkje eit løfte om at framtidige episodar er utgjevne.
- «Returning Series», premieredato eller delvis tilgjenge er aldri brukt som bevis på overvaking.
  Appen lovar derfor ikkje «kjem automatisk». Ingen direkte Sonarr-tilkopling eller ekstra backend.
- Alle varsel kan slåast av lokalt. Lagring og bakgrunnssjekkar brukar same kontoavgrensing som før.

## Kva statusane tyder

| Status | Datagrunnlag |
| --- | --- |
| Førespurd | Registrert førespurnad; godkjenning åleine er ikkje ei nedlasting. |
| Lastar ned | Seerr returnerer ein faktisk nedlastingspost for filmen eller ein vald sesong. |
| Blir lagt i biblioteket | Nedlastinga er fullført, men Seerr har ikkje stadfesta bibliotektilgjenge. |
| I biblioteket | Seerr melder AVAILABLE for filmen eller alle dei valde sesongane. |
| Avvist / Treng tilsyn | Avvist førespurnad, blokkering eller rapportert nedlastingsfeil. |
| Status ukjend | Detaljstatus kunne ikkje hentast; ingen påstått framdrift. |
| Følgjer med | Eit lokalt varsel ventar på endra bibliotekstatus; ingen ny førespurnad er sendt. |

Standard- og 4K-status blir haldne frå kvarandre. Ei nedlasting for ein annan sesong påverkar ikkje førespurnaden. Ukjent sesongnummer blir ikkje gjetta. Prosent gjeld dei kjende, matchande nedlastingane, ikkje nødvendigvis alle episodane i ein sesong.

## Varsel og personvern

- Varsel blir sende lokalt på denne Android-eininga etter stadfesta bibliotektilgjenge, ikkje straks Seerr godkjenner førespurnaden.
- Nye førespurnader har varsel på som standard. Historiske førespurnader som blir importerte, har varsel av for å unngå gamle massevarsel.
- Aktiv app sjekkar om lag kvart 30. sekund. Bakgrunnsarbeid er planlagt kvart 30. minutt; Android, nettverk og innstillinga for berre Wi-Fi kan forseinke det. Dette er ikkje server-push.
- Opptil 100 lokale førespurnader blir lagra; opptil 20 får detaljoppdatering per runde. Eldre postar blir kontrollerte etter tur. Dette er ikkje eit komplett arkiv eller full Seerr-administrasjon.
- Fylging og varslingsval er skilde etter normalisert tenaradresse og verifisert Seerr-brukar-ID. Ein ny innloggingscookie endrar ikkje denne identiteten.
- Tilgjengelege TMDB-plakatar kan følgje varselet. Bilethenting har storleiks-/tidsgrenser, sender ingen tenarpassord og følgjer ikkje omdirigeringar. Manglande bilete gir tekstvarsel.
- Varsel skjuler detaljane på låst skjerm etter Android sine private varslingsreglar. Trykk opnar Aktivitet. Appen endrar ikkje globale Seerr-varslingsinnstillingar.

## Kontraktar

Personlege førespurnader krev Seerr-økt, ikkje administrator-API-nøkkel. Appen les `/auth/me` og samanliknar ID-en med den viste kontoen. POST inneheld eksplisitte sesongnummer og ikkje eit felt for å opptre som ein annan brukar. HTTP 202 utan nye sesongar blir ikkje meldt som oppretta førespurnad.

Primærkjelder: [Seerr sine statusar](https://github.com/seerr-team/seerr/blob/develop/server/constants/media.ts), [request-ruter](https://github.com/seerr-team/seerr/blob/develop/server/routes/request.ts), [nedlastingsovervaking](https://github.com/seerr-team/seerr/blob/develop/server/lib/downloadtracker.ts).

## Verifikasjon

Ein separat Android-testemulator blir brukt til automatiske testar og syntetiske varsel. Den innlogga emulatoren blir berre brukt til lesing, navigering og lokale val. Ingen ekte førespurnad blir sendt til brukarens Seerr under desse testane. Server-POST blir verifisert med kontrollert testtransport; ende-til-ende nedlasting/import på ekte tenarar er ikkje gjennomført.

Verifisert for 0.9.0, 5. september 2026: 126 JVM-testar, 44 Android-testar på API 36 og 10 ekstra UI-testar med 150 % tekststorleik passerte. Lint: 0 feil, 16 åtvaringar. Manuell kontroll stadfesta personleg profilbilete, låste bibliotekssesongar, standardvald manglande sesong, deaktivert sending ved tomt val, bibliotekikon og lesbar aktivitetsstatus. Oppgradering med bevart innlogging vart kontrollert på den innlogga emulatoren.
