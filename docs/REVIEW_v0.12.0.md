# Spole · gjennomgang av heilskapen

## Retning

Bevar matte, mørke flater, varm typografi og Spole-merket. Filmkunst får bere uttrykket;
lime markerer handlingar og framdrift. Eit lokalt teikna filmstripe-element gir førstegongsoppsettet
eigen identitet utan nettverksavhengig pynt. Ingen ny backend eller ekstra innlogging er innført.

## Gjennomgangen og endringane

| Område | Funn | Endring |
| --- | --- | --- |
| Heim | Bibliotekrader låg tett inntil avspelinga | Jamnare mellomrom og framleis separate tenesterader |
| Anbefalingar | Feil kjeldetekst; opning sletta eit pågåande Oppdag-søk | Rett tekst for felleslista; søk og resultat blir bevarte |
| Anbefalingar | Ukjend status kunne framstå som klar for førespurnad | Nøytral «Sjå tilgjenge» når status ikkje er stadfesta |
| Detaljar | Omtalen stoppa etter fire linjer; faste felt klipte stor skrift | «Les heile omtalen», fleksible tekstfelt og rulling inni fast ark |
| Detaljar | Lite informasjon om dei som medverkar | Namn og roller frå tenestene; Seerr-portrett der dei er tilgjengelege |
| Overgangar | Kalender, detaljar og førespurnad brukte ulike høgder | Same 82 % innhaldsramme frå første komposisjon |
| Oppdag | Berre film-/seriefilter | Tilgjengefilter i tillegg; tydeleg skilje mellom venting og ferdig |
| Aktivitet | Alle eigne førespurnader i same lange liste | Alle / På veg / Klare med tal og ein felles visuell framdrift |
| Førespurnad | Tekstpilene forklarte flyten dårleg | Tre visuelle steg; same språk som statuskorta |
| Førstegongsoppsett | Identitetslaust startbilete | Lokal filmstripe med kort inngangsanimasjon og eksisterande innloggingsval |
| Store skrifter | Merke kunne kollidere med teksten på cover | Reservert topprom og kort som kan vekse |
| Innlogging | Store bokstavar i protokollen gav adressefeil | Felles normalisering for alle tenester og fellesinnlogging; undermapper beheld store/små bokstavar |
| Adressefelt | Automatisk retting og uklare adressefeil | URL-tastatur utan automatisk retting/stor forbokstav; konkrete feil for mellomrom, port og protokoll |

## Kva som framleis er viktig

- Utgjevingsradene er gjennomgått på nytt: «Nyleg tilgjengeleg» er eit snitt av personleg bibliotektilgjenge og ny digital dato (filmar), eller ny premieredato for ein tilgjengeleg episode. Kalenderen er framleis Radarr/Sonarr. Sjå [planen for delt kalender](SHARED_CALENDAR_PLAN.md).
- Konto- og administratorgrenser er bevarte. Ingen ekte førespurnader eller avspelingskommandoar blir sende under review.
- Testemulatoren er skild frå review-data. Etter ny personleg innlogging er Seerr, Jellyfin, Emby og Radarr kontrollerte som aktive i produksjonsappen. Heim viser nye bibliotekepisodar og Radarr-filmar i kalenderen. Sonarr er framleis ikkje tilkopla. Sjå [verifiseringa](VERIFICATION_v0.12.0.md) for nøyaktige avgrensingar.
- Ein ny, synleg `Spole_Review`-emulator på port 5560 er opna for personleg innlogging. Data ligg utanfor byggmappa i `C:/JellyBin/.spole-review-avds`. Instrumentering skal berre køyre på `Spole_Instrumentation`, port 5562.
- Medverkande avheng av metadata på tenaren. Manglande bilete gir initialar; manglande credits skjuler delen.
- Dette er ei samla kvalitetsforbetring, ikkje ei påstand om full Seerr-administrasjon. Paginering, godkjenningsverktøy og eit komplett historikkarkiv er framleis eigne produktsteg.

API-grunnlag: [Seerr sitt skjema](https://github.com/seerr-team/seerr/blob/develop/seerr-api.yml),
[Emby sine personfelt](https://dev.emby.media/reference/RestAPI/ItemsService/getUsersByUseridItems.html).
