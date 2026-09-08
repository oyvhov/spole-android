# Språk i Spole

## Status i nattarbeidet 9. september 2026

Språkgrunnlaget er implementert, med eit fungerande val mellom nynorsk, engelsk og «Følg eininga» i Innstillingar. **Engelsk er ei førehandsvising**, ikkje ei ferdig omsetjing av heile appen.

Migrert i denne bolken: hovudnavigasjon, oppstart/onboarding, hovudoverskrifter og søk på Heim, søk/filter i Oppdag, sentrale aktivitets- og innstillingstekstar, personvern-/krediteringstekstar, widget og dei lokale avspelingskontrollane. Nokre kort, kontopanel, detaljar, førespurnader, nettverksfeil og statusar frå modell/repository har framleis hardkoda nynorsk. Metadata frå tenestene er ikkje maskinomsette.

## Lagring og migrering

- `AppLanguage` lagrar stabile språkkodar: tom streng for system, `nn` og `en`.
- `AppLanguages` lagrar berre språkvalet i `spole_language`. Det rører ikkje innloggingar, serveradresser eller bibliotekval.
- Ei eksisterande installasjon blir attkjend gjennom appval eller kjende tilkoplingsnøklar. Ho får nynorsk første gong, slik at ei oppdatering ikkje brått endrar språket. Nye installasjonar følgjer eininga.
- `LocalizedActivity` gjer eit lokalt ressursoppsett for hovudaktiviteten og spelaren. Språkbyte gjenskaper skjermen, men bevarer ViewModel og lagra skjermtilstand. Språket på sjølve telefonen blir ikkje endra.
- Widgeten brukar det same språkvalet når han blir oppdatert. Tenarbaserte titlar og undertekstar blir ikkje endra.
- Engelsk er grunnspråk i `values`; nynorsk ligg i `values-b+nn`. Ein språkvariant utan eiga omsetjing brukar Android si vanlege ressursmatching og til slutt engelsk for migrerte tekstar.
- Språkressursane blir ikkje delte i eigne AAB-språkpakker. Språkbyte treng derfor ikkje ekstra nedlasting av desse små tekstfilene.
- Android sitt eige appspråkval er ikkje kopla på enno. Det blir vurdert når heile omsetjinga er ferdig; no er Innstillingar i Spole kjelda til språkvalet.

## Legg til eller endre ein tekst

1. Lag ein stabil nøkkel med skjerm/handling, til dømes `player_resume`. Ikkje bruk sjølve teksten som ID eller lagringstilstand.
2. Skriv heile setninga i begge språkfiler. Bruk `%1$s`, `%2$d` og `plurals` der tal og grammatikk krev det. Ikkje bygg setningar med fleire tekstbitar.
3. Bruk `stringResource`/`pluralStringResource` i Compose. For callbackar utan Compose-kontekst, hent teksten i den omsluttande composable-funksjonen først. Bruk eit språktilpassa `Context` utanfor Compose.
4. Behald tenar-ID, medietype, menyval og filteridentitet uavhengig av synleg tekst. Spelarmenyane brukar no `PlayerMenu`, ikkje «Lydspor» som styringsverdi.
5. Køyr ressurs- og UI-testane. Prøv òg lang tekst og stor skrift på smal skjerm.

## Legg til eit nytt språk

1. Kopier dei omsetjande tekstressursane til ei BCP-47-mappe, til dømes `values-b+sv`. Bruk `values-b+pt+BR` for ein landvariant. Ikkje omset Spole eller tenestenamna.
2. Legg ein ny kode og språket sitt eige namn i `AppLanguage`. Innstillingslista brukar enum-oppføringane automatisk; inga skjerm- eller tenestelogikk skal kopierast.
3. Omset alle nøklar, bevar parameter og legg til språkrette fleirtalsformer. Ressurstesten kontrollerer at registrerte språk har alle nøklane; utvid grammatikkontrollen ved nye fleirtalskategoriar.
4. Få teksten gjennomgått av nokon som kan språket, og køyr kjerneflytane med den nye språkfila før språket blir tilbode som ferdig.

Bidrag kan gjerast gjennom vanlege GitHub-endringar. Ingen eigen omsetjingsserver er nødvendig, men det private repoet krev tilgang for bidragsytarar.

## Lita ordliste

| Nynorsk | Engelsk | Meining |
| --- | --- | --- |
| Heim | Home | Personleg oversikt |
| Oppdag | Discover | Søk og nye titlar |
| Førespurnad | Request | Ei handling frå den personlege Seerr-kontoen |
| Følg | Follow | Varsel/oppfølging utan nødvendigvis ei ny førespurnad |
| I biblioteket | In your library | Stadfesta tilgjenge, ikkje berre godkjend førespurnad |
| Kjem snart | Coming soon | Heimeutgjevingar og episodar, ikkje kinolansering |
| Anbefalingar | Recommendations | Den redaksjonelle GitHub-lista |

## Testar og det som står att

- JVM: migreringsval, stabile kodar, ressursdekning og parameter/fleirtal.
- Android: faktiske ressursoppslag, isolert migrering, språkbyte fram og tilbake og gjenskaping av Innstillingar.
- Den automatiske testkøyraren brukar nynorsk som fixture-språk. Han skal aldri køyrast mot den innlogga review-installasjonen.
- Gjenstår før full språkstøtte: alle modell-/nettverkstekstar, attståande skjermar, full språktilpassing av datoar og varsel, automatisk Android-appspråkval, gjennomgått engelsk og full språk-/tilgjengelegheitsmatrise.

Sjå [veikartet](../ROADMAP.md) og [testmatrisa](QA_MATRIX.md). Denne fila er implementeringsrettleiing; endelege testtal står i natt-/verifiseringsrapporten.
