<p align="center">
  <img src="design/brand/spole-icon.svg" width="96" alt="Spole-logo" />
</p>

<h1 align="center">Spole</h1>
<p align="center">Filmane dine. Seriane dine. Éin stad.</p>
<p align="center">Android TV · Google TV · Nettbrett · Telefon</p>
<p align="center">
  <a href="https://github.com/oyvhov/spole-android/releases"><strong>Last ned APK</strong></a>
  &nbsp; · &nbsp;
  <a href="docs/APP_GUIDE.md">Kom i gang</a>
  &nbsp; · &nbsp;
  <a href="https://github.com/oyvhov/spole-android/issues">Meld ein feil</a>
</p>

Spole samlar Jellyfin, Emby, Seerr, Radarr og Sonarr i ein innfødd Android-app.
Bla i biblioteka dine, spel frå Jellyfin og følg førespurnader frå Seerr.
Grensesnittet tilpassar seg fjernkontroll og berøring, med nynorsk og engelsk som språkval.

**Under aktiv utvikling.** Siste publiserte APK er
[0.16.0-alpha24](https://github.com/oyvhov/spole-android/releases/tag/v0.16.0-alpha24).
Nyare arbeid ligg på [utviklingsgreina](https://github.com/oyvhov/spole-android/tree/spole-feature-and-hardening-pass).
Alphaversjonar ligg under **Releases**; GitHub si «latest»-lenkje tek ikkje nødvendigvis med desse.

## På skjermane dine

### TV
![Spole på TV: Heim med aktive avspelingar og innhaldsrader](docs/images/devices/tv-home.png)

Styr appen med fjernkontrollen. TV-oppsettet har sidepanel, horisontale innhaldsrader,
eigne innstillingar og ein spelar med søkjelinje, lyd, undertekst og neste episode.

### Nettbrett
![Spole på nettbrett: Heim med breitt oppsett og sidenavigasjon](docs/images/devices/tablet-home.png)

Eit breitt oppsett med sidanavigasjon og meir synleg innhald.

### Telefon
<p align="center">
  <img src="docs/images/devices/phone-home.png" width="32%" alt="Spole Heim på telefon" />
  &nbsp;
  <img src="docs/images/devices/phone-discover.png" width="32%" alt="Oppdag på telefon" />
  &nbsp;
  <img src="docs/images/devices/phone-settings.png" width="32%" alt="Innstillingar på telefon" />
</p>

Botnnavigasjon og innstillingar grupperte for mindre skjermar.

*Bileta er tekne av appen i emulator 13. september 2026, med demodata og utan private
kontoar. Dei viser utviklingsutgåva, som kan skilje seg frå publisert APK.
[Sjå alle bileta og korleis dei er anonymiserte](docs/images/devices/README.md).*

## Kva kan eg bruke Spole til?

| Teneste | I Spole |
| --- | --- |
| **Jellyfin** | Bibliotek, hald fram, favorittar og avspeling med lyd- og undertekstval. Quick Connect eller brukarnamn og passord. |
| **Emby** | Innlogging og innhald frå biblioteket. Den innebygde spelaren støttar førebels Jellyfin. |
| **Seerr** | Oppdag innhald, be om filmar og sesongar og følg dine eigne førespurnader. |
| **Radarr / Sonarr** | Oversikt over komande innhald og nedlasting når kontoen har tilgang. |

Tenestene er valfrie. Du treng dine eigne tenarar og kontoar; Spole leverer ikkje filmar eller seriar.
Kontoen sine rettar avgjer kva du kan sjå og gjere.

- **Hald fram der du slapp.** Avspeling i appen frå Jellyfin, med neste episode og val for tidspunkt.
- **Gjer appen personleg.** Tema og aksentfargar, jul og Halloween, synlege Heim-rader og val for TV-sidepanelet.
- **Følg førespurnadene dine.** Frå førespurd til tilgjengeleg, med valfrie varsel.
- **Oppdater i appen.** Spole kan hente ei publisert APK frå GitHub og kontrollere henne før Android spør om installasjon.

**Klart i kjeldekoden for neste APK:** endre rekkjefølgja på Heim-radene,
vurdering på filmkort, tenestestatus samla i innstillingane og éin knapp for å
logge ut av alle tenester på denne eininga.

## Kom i gang

1. Last ned APK-en frå [Releases](https://github.com/oyvhov/spole-android/releases) og installer på Android 8.0 eller nyare.
2. Vel **Kom i gang**. Opne eller lim inn ei oppsettslenkje frå den som driftar tenaren, eller skriv inn adressene.
3. Vel **Godkjenn på mobilen** med Quick Connect, eller bruk brukarnamn og passord.
4. Har du Seerr på same Jellyfin-oppsett, kan Spole logge inn på begge i den same flyten. Seerr må støtte denne innloggingsmåten.

Andre innloggingsmåtar finst under **Andre innloggingsmåtar**. Du kan også prøve appen med demodata.

**Deler du med familie eller vener?** Del APK-lenkja og ei oppsettslenkje frå Spole.
Lenkja inneheld tenesteadresser, aldri passord eller tilgangsteikn. Kvar brukar loggar inn
med sin eigen konto. [Les brukar- og administratorguiden](docs/APP_GUIDE.md).

## Personvern

Innloggingar blir lagra kryptert på eininga. Appen brukar tenesteadressene du legg inn.
Spole har ingen sentral kontotenar. TMDB blir brukt til bilete, og GitHub til
tilrådingsliste og oppdateringar; desse tenestene får IP-adressa og ressursførespurnaden.
Appoppdateringar sender òg Spole-versjonen. Kontoopplysningar blir ikkje sende til GitHub.

Skjermbileta i denne presentasjonen bruker demodata. Ingen ekte namn, e-postadresser,
serveradresser, innloggingskodar eller tilgangsteikn er publiserte i dei.
[Personvernerklæringa for gjeldande utviklingsutgåve](https://github.com/oyvhov/spole-android/blob/spole-feature-and-hardening-pass/docs/PRIVACY.md).

## Tilbakemeldingar og utvikling

[Meld feil eller ønsk ein funksjon](https://github.com/oyvhov/spole-android/issues).
Ta med appversjon, eining og kva du trykte på. Skjul namn, adresser og innloggingsopplysningar før du deler bilete eller loggar.

Appen er bygd med Kotlin og Jetpack Compose. Bruk
[utviklingsgreina](https://github.com/oyvhov/spole-android/tree/spole-feature-and-hardening-pass)
for den nyaste kjeldekoden, og release-taggen for å finne kjelda til ein bestemt APK.
Les prosjektet si AGENTS.md og docs/AI_INSTRUCTIONS.md før endringar.

Spole er ikkje tilknytt eller godkjend av Jellyfin, Emby, Seerr, Radarr, Sonarr eller TMDB.
Namna og merka tilhøyrer eigarane sine.
