# Verifisering av Spole 1.0.0-beta39

7. november 2026. Produksjonspakke `app.reelstack`, versjonskode 176. Bygd oppå beta38 (`e01eb0b`).

## Bakgrunn

Brukaren bad om ein gjennomgang av heile appen og framsida på Loftet. Loftet er ein Google TV Streamer med Android 14 og
1920 × 1080. Han har Spole på bokmål og éi tilgjengelegheitsteneste slått på: Stova Heim-knapp (`app.stova.debug`).

## Gjennomgangen på Loftet

Loftet hadde beta37. Spole viste sjølv «Spole v1.0.0-beta38 er klar», og beta38 vart lagd inn gjennom oppdateringa i
appen. Første installasjonsdato (28.10.2026) er uendra.

Gått gjennom med fjernkontrollen, utan avspeling og utan førespurnader:

- Heim: toppbiletet og alle radene ned til «Oppdater biblioteket»
- Filmar, Seriar og Samlingar
- Oppdag
- Søk, med tastaturet og resultat
- Ei filmside og ei serieside
- Profilmenyen
- Innstillingane

Retta i denne utgåva:

- **Oppdatering i appen.** Det første trykket på «Installer oppdatering» gjorde ingenting. Loggen synte
  `START … result code=2` (`START_TASK_TO_FRONT`). Ei gøymd installasjonsoppgåve frå førre oppdatering, med akkurat same
  førespurnad (same `content://app.reelstack.updates/updates/spole-update.apk`), vart henta fram og lukka seg med ein gong.
  Det andre trykket opna installasjonen.
  - Etter oppdateringa låg det att ei ny slik oppgåve, så feilen ville kome att ved neste oppdatering.
  - `updateInstallIntent` legg no til `FLAG_ACTIVITY_CLEAR_TASK`, slik at installasjonen alltid startar på nytt.
  - Det er den installerte utgåva som opnar installasjonen. Rettinga verkar difor først når beta39 oppdaterer seg til
    neste utgåve.
- **Overskriftene på Heim.** «Anbefalinger», «Nylig tilgjengelig» og «Kommer snart» stod framleis i den store storleiken
  etter beta38. `SectionTitle` brukar no `titleMedium` på TV, som dei andre radene.
- **Innstillingane på TV** slutta 72 dp over botnen med «Legg til barneprofil» skoren gjennom midten av linja. Lista går
  no ned til botnen og tonar ut der, som detaljsidene gjer. Ho har plass nok under siste rad til at fokus aldri står i
  toninga.
- **Oppdag.** Når ein tittel mangla bakgrunnsbilete, vart plakaten lagd bak sida i staden. Ein ståande plakat blir ikkje
  skoren til i ei liggjande ramme, så han stod som ei smal stripe midt på, mørk på begge sider («Eşref Tek»). No blir det
  berre lagt eit bilete bak sida når det finst ein ekte bakgrunn.
- **Bokmål.** 15 tekstar med nynorske ord er retta, mellom anna:
  - «Kommande» på episodane som kjem → «Kommende»
  - «Tøm biletbuffer» → «Tøm bildebuffer»
  - «Velg noko å sjå på» → «Velg noe å se på»
- **Korta som stopp for tilgjengelegheitstenester.** Sjå *Yting* under.

Funne, men ikkje retta i denne utgåva:

- Under «Kommer snart» står «Noen utgivelsesdatoer kunne ikke hentes. Prøv å oppdatere.» kvar gong. Kalenderen
  svelgjer feilen for kvar tittel utan å logge han, så det er ikkje funne kva for tittel eller sesong som feilar.
- «Nye episoder» og «Nylig tilgjengelig» viser delvis dei same episodane, til dømes Dexter: Resurrection S2 - E2.
- Rader på bibliotek- og detaljsidene endar i ein hard kant 43 px før skjermkanten og skjer gjennom eit kort. Det gjeld
  «Neste», «Medvirkende» og episodane. Radene på Heim tonar ut i staden.
- Med profilmenyen open forsvinn menylina, og «Spill av» i toppbiletet får fokusring. Då ser det ut som om to ting har
  fokus.
- Lydspor og undertekstar står med namnet frå tenaren, til dømes «Norwegian - SUBRIP» og «Norwegian - PGSSUB».
- Samlingar viser eit årstal under kvar samling, til dømes «Action · 1967».
- Utgivingsnotata i oppdateringsvindauget kjem frå GitHub og er difor på nynorsk også i bokmålsappen.
- Etter installasjonen viste Android «app.reelstack.ReelstackApplication» med eit generisk ikon. Manifestet har namn og
  ikon, så dette ser ut til å kome frå Android sjølv.

## Yting

### Målingane

Målt på Loftet nedover heile Heim med 0,9 s mellom trykka, frå `dumpsys gfxinfo framestats` og `simpleperf`.

| | beta38 | beta39 |
| --- | --- | --- |
| Bilete innan éitt vsync | 81–85 % | 76–85 % |
| Hovudtråden i `getCurrentSemanticsNodes` | 37,6 % | 29,6 % |
| av det: å finne nodane | 19,4 % | 7,0 % |
| av det: sortere dei etter plassering | 18,0 % | 22,4 % |

Kvart kort på Heim, i smarte hyller og på Oppdag la ti nodar til tilgjengelegheitstreet:

- biletet og avrundinga av det (`clip` gir ein eigen node med form i Compose 1.11)
- framdrifta
- teksten under kortet: tittelen, episoden og reserven for linja

På TV er kortet no éin node. Han har tittel, episode og framdrift i namnet, og det som ligg under er fjerna
(`cardSemantics`). Ein skjermlesar stoppar éin gong per kort i staden for tre.

Det å finne nodane fall frå 19 til 7 % av hovudtråden. Sorteringa fall ikkje. Compose 1.11 sorterer med
`SemanticsNode.getChildren()`, og der er også nodane under eit kort som er fjerna med `clearAndSetSemantics` med. Det er
stadfesta i bytekoden.

### Kvifor biletrytmen ikkje vart betre

Dei fleste seine bileta på Loftet er avgrensa av grafikkbrikka, ikkje av hovudtråden. I tre målingar var 9 av 17, 16 av
22 og 14 av 20 seine bilete over 16 ms på grafikkbrikka.

Tida på grafikkbrikka per bilete:

| Del av Heim | Grafikkbrikka per bilete |
| --- | --- |
| Radene i midten | 9–13 ms |
| Medan toppbiletet er synleg | 17–30 ms |
| Dei store korta nedst (Anbefalinger, Nylig tilgjengelig, Kommer snart) | 16–28 ms |

Prøvd og ikkje teke med:

- **Utan kanttoning på radene:** grafikkbrikka er uendra (median 8–9 ms, 90. persentil 19–20 ms). Toninga er ikkje
  årsaka.
- **Utan det maskerte laget for toppbiletet:** toppbiletet tek framleis 17–21 ms. Laget kostar berre nokre få ms.
- **Utan bufferet bakover på Heim og radene:**
  - Sorteringa og finninga fall til 26 %, men førebuinga steig frå 8 til 16 %, og biletrytmen vart ikkje betre.
  - Rader du har gått forbi måtte òg byggjast på nytt når du gjekk opp att.
  - Bufferet står som i beta38.

Neste steg for Loftet:

- Gjere toppbiletet og dei store korta nedst billigare å teikne for grafikkbrikka.
- Få ned talet på nodar som ikkje treng å vere der: avrundingane og dei indre test-merka.

## Bygg og testar

- Einingstestar: 1 663 køyrde, 0 feil, 1 hoppa over. Lint: 0 feil, 219 åtvaringar. Omsetjingar: 1 695 nøklar OK.
  Release-samsvar OK.
- Desse testane feila av rett grunn før rettinga:
  - `UpdateInstallIntentTest.theInstallerStartsInAFreshTaskEvenWhenAnOldOneIsLeft`
  - `HomeSectionTitleTest.theCalendarHeadingIsAsQuietAsTheShelves`: 24,5 dp
  - `TvSettingsLayoutTest.theSettingsRunOnToTheBottomOfTheScreenInsteadOfStoppingShortWithACut`: slutta på 468 av 540 dp
  - `DiscoverTvFocusTest.aTitleWithOnlyAPosterPutsNoPictureBehindThePage`
  - `HomeTvCardSemanticsTest`: 10 nodar i eit kort med fortsett å sjå, 8 i eit filmkort. Testen er skriven om til å
    sjekke det tenestene ser: kortet fjernar det som ligg under, og ber ikkje tekstane under seg.
- Tre testar fann korta på teksten under dei. No finn dei korta på namnet (`hasCardLabel`).
- Prøvebygga av beta39 vart lagde inn på Loftet med `install -r`. Første installasjonsdato er uendra.

## Publisering

- Release-commit `c089f4a`, tagg `v1.0.0-beta39`, pusha med `git push --atomic origin HEAD:main v1.0.0-beta39`
  (spola fram frå `e01eb0b`).
- Signert APK: 28 157 852 byte, SHA-256 `9b135d30…7d639a59`, sertifikat `36fa94f0…`, `app.reelstack` 176, ikkje
  debuggable, `profileable` frå skalet. Alle 20 native bibliotek er byte for byte like beta38.
- Seks assets med digest kontrollert mot GitHub. Releasen er publisert som prerelease (ikkje `latest`), og den
  offentlege nedlastinga utan innlogging har same SHA-256 som bygget.
- CI «Bygg og test» på `c089f4a`: grøn ([køyring 42405875567](https://github.com/oyvhov/spole-android/actions/runs/42405875567)).

## Den offentlege APK-en på Loftet

Lagd inn med `install -r`. Første installasjonsdato (28.10.2026) er uendra. Sett på eininga:

- «Kommer snart» er like stor som overskriftene over.
- Innstillingane går ned til botnen og tonar ut. «Legg til barneprofil» står heil.
- «Tøm bildebuffer» står på bokmål.
- Episodane som kjem, er merkte «Kommende».

Funne etterpå: forklaringa under «Tøm bildebuffer» hadde «utan». Ho er retta til «uten» i commiten etter releasen og
kjem med i neste utgåve.
