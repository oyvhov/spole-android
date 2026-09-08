# Personvern i Spole

Sist oppdatert: 8. september 2026. Gjeld Spole 0.14.0 og nyare.

Spole er ein klient for medietenester du sjølv driftar. Utviklaren har ingen tenar, ingen konto
og ingen database. Det finst ingen stad for oss å samle data om deg, og vi gjer det ikkje.

## Kort sagt

- Spole sender medieførespurnadene dine berre til adressene du sjølv skriv inn.
- Ingenting blir sendt til utviklaren. Det finst ingen analyse, ingen sporing og ingen reklame.
- Innloggingar blir lagra kryptert på eininga og forlèt henne aldri.
- To adresser blir kontakta i tillegg til dine eigne tenarar: TMDB for plakatar og GitHub for
  tilrådingslista. Begge får berre IP-adressa di og kva fil du bad om — aldri kven du er.

## Kva som blir lagra på eininga

| Kva | Kvar | Kryptert |
|---|---|---|
| Tilgangsteikn og Seerr-sesjonar | app-privat lagring | Ja, med Android Keystore (AES-GCM) |
| Tenaradresser, brukar-ID og val | app-privat lagring | Nei — dei er ikkje hemmelege |
| Eit tilfeldig einings-ID | app-privat lagring | Nei |
| Mellomlagra titlar og plakatadresser | app-privat SQLite | Nei |
| Feilrapport etter ein krasj | app-privat fil | Nei — adresser og teikn er fjerna |

App-privat lagring er berre lesbar for Spole. Tilgangsteikna er i tillegg ekskluderte frå både
sky-sikkerheitskopiering og einingsoverføring, slik at dei ikkje følgjer med til ei ny eining.

**Passord blir aldri lagra.** Dei blir sende éin gong til tenesta du loggar inn på, og deretter
er det tilgangsteiknet frå tenesta appen held på.

**Avspelingar, køar og aktivitetsfeeden blir aldri skrivne til disk.** Berre titlar og
plakatadresser blir mellomlagra, slik at framsida har noko å vise medan første oppdatering går.

## Kva som blir sendt, og kvar

**Til dine eigne tenarar** (Jellyfin, Emby, Seerr, Radarr, Sonarr): innlogginga di, søkeorda dine,
førespurnadene dine og eit einings-ID slik at tenaren kan vise «Spole på Android» i eiga
einingsliste. Kva desse tenarane loggar, er opp til oppsettet ditt.

**Ved avspeling i den integrerte Jellyfin-spelaren** blir video og undertekstar henta frå din
Jellyfin-tenar. Tittel-ID, avspelingsøkt, posisjon, pause/stopp og valde spor blir sende tilbake
til same tenar under din eigen konto, slik at Jellyfin kan lagre framdrifta. Android får
tittel og avspelingsstatus gjennom ei lokal medieøkt for system- og hovudtelefonkontrollar.
Video blir bufra i minnet; spelaren lagrar ikkje ei nedlasta filmfil eller ein varig avspelingslogg.

**Til `image.tmdb.org`**: adressene til plakatar og bakgrunnsbilete som Seerr viser til. TMDB ser
IP-adressa di og kva bilete du bad om. Ingen kontoinformasjon og ingen tilgangsteikn blir sende
dit — appen nektar å feste tenesteteikn til noko som ikkje er tenaren din.

**Til `raw.githubusercontent.com`**: éi statisk JSON-fil med tilrådingar. GitHub ser IP-adressa di
og kva fil du bad om. Ingenting om deg blir sendt. Rada kan slåast av i Innstillingar →
Tilpass framsida → Anbefalingar, og då blir fila ikkje henta.

Ingen andre adresser blir kontakta.

## Einings-ID

Spole lagar eit tilfeldig einings-ID første gongen appen startar, og sender det til Jellyfin og
Emby slik at dei kan skilje denne eininga frå andre i einingslista si. Det er ikkje eit
maskinvare-ID: det er tilfeldig, unikt for denne installasjonen, og blir borte når du avinstallerer
appen.

Installasjonar frå før 0.14.0 tek med seg det ID-en dei alt hadde, slik at tenaren din ikkje får ei
duplisert einingsoppføring etter oppdateringa.

## Krasjrapportar

Om Spole stoppar uventa, blir det skrive ei fil på eininga med versjon, einingsmodell og kva som
gjekk gale. Tenaradresser og alt som liknar eit tilgangsteikn blir fjerna før fila blir skriven.

Fila blir aldri sendt nokon stad av seg sjølv. Ho ligg der til du deler henne frå Innstillingar,
eller slettar henne same stad.

## Ukryptert trafikk

Sjølvhosta tenarar står ofte på HTTP inne på eige nett. Spole tillèt det berre for `localhost`,
`.local`-namn og literale private adresser (10.x, 172.16–31.x, 192.168.x, ::1, fc00::/7,
fe80::/10). Ei HTTP-adresse mot ein offentleg vert blir avvist, ikkje åtvara om.

Bruker du HTTP, viser Innstillingar kva tilkoplingar det gjeld. Utanfor ditt eige nett bør du
bruke HTTPS gjennom din eigen proxy.

## Sletting

Avinstaller appen. Alt i tabellen over forsvinn med han. Du kan òg logge ut av éi teneste om
gongen i Innstillingar; det slettar tilgangsteiknet for den tenesta med ein gong.

Spole kan ikkje slette noko frå tenarane dine, og gjer det ikkje. Å trekkje tilbake ein førespurnad
i appen gjer nøyaktig det Seerr sjølv ville gjort.

## Barn

Spole samlar ingen data og har ikkje ei aldersgrense av personvernomsyn. Innhaldet appen viser er
det som ligg på dine eigne tenarar.

## Endringar

Denne fila blir oppdatert saman med appen. Datoen øvst seier når. Vesentlege endringar blir òg
nemnde i `CHANGELOG.md`.

## Kontakt

Spørsmål om personvern kan sendast som ei sak i GitHub-repoet.
