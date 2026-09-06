# Trygg delt kalender · plan, ikkje implementert

## Avtalt åtferd

- **Nyleg tilgjengeleg:** filmar med digital utgjevingsdato dei siste 28 dagane og stadfesta kopi i den innlogga brukarens Jellyfin-/Emby-bibliotek. Episodar må vere utgjevne i same tidsrom og liggje i biblioteket. Dato lagt til er ikkje utgjevingsdato. Kino- og fysisk dato er ikkje digital dato.
- **Kjem snart:** overvaka digitale/fysiske heimeutgjevingar frå Radarr og episodar frå Sonarr. Ingen generell Seerr-/TMDB-trendliste som erstatning for brukarens kalender.
- Alle innlogga brukarar skal etter kvart kunne sjå den tillatne kalenderen utan eigne administratornøklar. Seerr/Jellyfin/Emby held fram som direkte integrasjonar; ingen obligatorisk ny appkonto.

## Kvifor ei ekstra leseteneste er nødvendig

Seerr sine vanlege innlogga oppdagings- og detaljruter tilbyr metadata, men ikkje ein vidareformidla Radarr-/Sonarr-kalender. `/service`-rutene er ikkje ein generell proxy. Klienten kan difor ikkje trygt hente kalenderen frå berre ein personleg Seerr-cookie i dagens integrasjon.

Dei eksisterande direkte Radarr-/Sonarr-tilkoplingane blir bevarte. Kalender og nedlastingskø er skilde: ein vanleg brukar kan lese kalenderen dersom eininga alt har ei slik tilkopling, men appen hentar ikkje administratorkøen for vedkomande. Dette gjer **ikkje** deling av administratornøklar trygt.

## Tilrådd løysing

Eit lite, valfritt kalenderadapter på tenarsida, bak same HTTPS-opphav som Seerr. Alternativt kan dette bli ei rute i sjølve Seerr dersom prosjektet ønskjer å støtte henne. Dette er eit nytt driftssteg som krev eiga godkjenning før oppsett; ingenting er installert eller publisert av denne planen.

1. Android sender den eksisterande personlege Seerr-økta til den eksplisitt konfigurerte kalenderstien på same opphav.
2. Adapteret validerer økta mot den fast konfigurerte Seerr-tenaren. Utgått økt gir 401; ingen anonym kalender eller tillit til ein klientoppgitt brukar-ID.
3. Radarr-/Sonarr-nøklane ligg berre i tenarmiljøet. Adapteret kan berre kalle dei faste kalenderendepunkta; aldri brukargjevne URL-ar, vilkårlege stiar eller POST/DELETE.
4. Kalenderdata blir filtrerte til tillatne titlar for den verifiserte brukaren. Ta høgd for blokkerte titlar og eventuelle bibliotek-/aldersgrenser før fellesdata blir delte. Ikkje bruk same svarcache for ulike rettar.
5. Svaret inneheld berre tittel, medietype, episode-/sesongnummer, utgjevingsdato/-type, trygg biletreferanse og kjelde. Ikkje filstiar, tenaradresser, nøklar, brukarar, førespurnadseigarar, kødata eller avspelingsøkter.
6. Appen bruker dette for «Kjem snart» og kalenderen. Innlogging, bibliotek og førespurnader held fram direkte mot tenestene.

## Vern og drift

- Fast tillatelsesliste for endepunkt; ingen open proxy og ingen omdirigering av innloggingscookie til andre opphav.
- Avgrensa 28-dagars vindauge, svarstorleik, tidsavbrot, samtidige kall og trafikk per innlogga konto.
- Kort metadata-cache (til dømes fem minutt), ny rettskontroll per svar. Loggar inneheld ikkje cookie, API-nøkkel eller fullstendige tenestesvar.
- Tydleg skilje mellom tom kalender, mellombels kjeldefeil og utgått innlogging. Vis tidspunktet for siste kjeldeoppdatering.
- Ikkje legg private kalenderdata eller administratornøklar på GitHub. Ei iCal-lenkje med administratornøkkel er ikkje ei personleg, lesebeskytta innlogging.

## Ferdig-kriterium før dette kan publiserast

- Vanleg Seerr-konto får tillatne kalenderpostar utan å få tilgang til Radarr/Sonarr sine andre funksjonar.
- Utlogga, utgått eller blokkert konto får ikkje kalenderdata; brukarbyte arvar ikkje gamle data.
- Feil i éi kjelde fjernar ikkje den andre kjelda; feil i nedlastingskøen stoppar ikkje kalenderen.
- Ingen kinodatoar blir feilmerkte som heimeutgjeving. Sonarr-episodar beheld faktisk dato og nummer.
- Verifiser med minst éin admin og éin ordinær konto på den ekte emulatoren. Ingen ekte førespurnader, sletting eller avspelingskommandoar under testen.

Grunnlag kontrollert 6. september 2026: [Seerr sine rutetilgangar](https://github.com/seerr-team/seerr/blob/develop/server/routes/index.ts), [tenesterutene](https://github.com/seerr-team/seerr/blob/develop/server/routes/service.ts), [filmdatoar](https://github.com/seerr-team/seerr/blob/develop/server/models/Movie.ts).
