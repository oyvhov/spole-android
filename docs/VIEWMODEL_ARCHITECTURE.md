# ViewModel og funksjonseigarskap

Oppdeling i arbeidskopien 17. oktober 2026. `ReelstackViewModel.kt` er redusert frå
4 150 til 457 linjer. Modellen eig navigasjon, kontolivssyklus og det offentlege API-et
som mobil-, nettbrett- og TV-skjermane brukar.

## Ansvarsgrenser

`ReelstackFeatures` koplar saman komponentane for éi autentisert økt. Kvar komponent
får dei konkrete repository-/klientavhengigheitene sine, ein underordna coroutine-scope
og eksplisitte tilbakekall for handlingar som kryssar funksjonsgrenser. Ingen av
koordinatorane tek imot `AppContainer` eller `ReelstackViewModel`. IO-dispatcher kan
bytast ut i testar.

| Eigarskap | Komponent |
| --- | --- |
| Heimfeed, cachehydrering, oppfrisking og øktkanal | `HomeFeedCoordinator` |
| Kontoopplysningar og verifisert identitet | `AccountRefreshCoordinator` |
| Biblioteksider, filter, bibliotekval og sidecache | `LibraryCoordinator` |
| Smarte hyller, redigering, førehandsvising og heimrader | `SmartShelfCoordinator` |
| Titteldetaljar, personhistorikk, sesongar og episodar | `DetailsCoordinator` |
| Sett-status, favorittar og fjerning frå hald fram | `MediaActionsCoordinator` |
| Førespurnadskladd, innsending, historikk og følgjeval | `RequestsCoordinator` |
| Nedlastingsførebuing, duplikatvern og framdriftsobservasjon | `OfflineDownloadsCoordinator` |
| Profilval, PIN og innlogging på barnekonto | `ProfileCoordinator` |
| Barnet sitt bibliotek og episodeval | `KidsLibraryCoordinator` |
| Tenaroppdaging, adressevalidering og innlogging | `ConnectionSetupCoordinator` |
| Kalenderfølgje, fjernstyring av avspeling og heiminnstillingar | `HomeActionsCoordinator` |
| Oppdag-rader og kataloggrid | `DiscoverHubCoordinator` |
| Søk og paginering, med separate instansar for globalt søk og Oppdag | `DiscoverSearchCoordinator` |

Dei tidlegare `LibraryViewModel`, `RequestsViewModel` og `ProfileViewModel` er fjerna.
Dei hadde eigne tilstandskopiar og berre delvis tilkopling til den verkelege appflyten.
Dei nye einingstestane rettar seg mot komponentane som appen faktisk kallar.

## Tilstand og livssyklus

`ReelstackUiState` er eitt atomisk skjermsnapshot. Dette lèt ei vellukka førespurnad
oppdatere kladden, historikken og tittelstatusen i same overgang. `SessionUiState` gir
funksjonane ein skrivetilgang som kan tilbakekallast. UI får berre lesbare StateFlow-ar.
Heim, Oppdag, innloggingskladd og renderingsøkt er projeksjonar med synkron `value` og
filtrering av uendra verdiar; dei lagrar ingen parallelle tilstandskopiar.

Ved kontobyte, profilbyte eller utlogging skjer dette i rekkjefølgje:

1. Tilbakekall skrivetilgangen til det gamle komponentsettet.
2. Lukk øktkanalen og kanseller alle underordna jobbar.
3. Byt skjermsnapshot til den nye økta, utan media eller opne ark frå førre konto.
4. Opprett nye komponentar med tomme mellomlager og ny navigasjonshistorikk.
5. Hent data for den nye identiteten. Utlogging ventar på pågåande skriving før
   siste tømming av lagra snapshots.

Tilbakekalling sperrar òg publisering frå ein sein callback eller ein `finally`-blokk
som ikkje samarbeider med coroutine-kansellering. Generasjonen i `SessionScope`
skil også mellom byte bort frå og tilbake til same konto. Når `ViewModelStore` blir
tømt, blir heile komponentsettet lukka.

Detaljlasting har ein eigen jobb som blir kansellert når ein opnar ein annan tittel
eller lukkar arket. Kvar førespurnad har òg eit løpenummer og den fanga økta, så eit
svar som kjem etter Tilbake, lukking, ny tittel eller kontobyte blir forkasta sjølv om
kallet ikkje let seg kansellere. Startarlenkjer (`spole://open`, sjå
[LAUNCHER_LINK.md](LAUNCHER_LINK.md)) går gjennom same `DetailsCoordinator`. Biblioteksidene har LRU-utkasting etter åtte sider. Dei gamle
tilstandsmodellane kan ikkje lenger overskrive ein open førespurnadskladd.

## Verifikasjon

Dei målretta JVM-testane dekkjer tilbakekalling, seine svar, synkrone projeksjonar,
bibliotekval, avbroten detaljlasting, førespurnader, duplikatvern, kvote og PIN.
`ViewModelFeatureIntegrationTest` prøver det offentlege ViewModel-API-et og det
verkelege komponentsettet på den isolerte Android-eininga. Eksisterande prøver for
innlogging, historikk, profilgrenser, episode-/serienavigasjon og TV-navigasjon blir
køyrde saman med denne. Bygg- og testresultat blir førte i
[oppgraderingsloggen](PLAYER_UPGRADE_2026-10-17.md).

Oppdelinga endrar ingen lagringsformat, serverendepunkt eller OSD-kontrollar. Ho ligg
innanfor den eksisterande appmodulen. Gradle-modulgrenser og vidare oppdeling av
nettspelarmodellen er eigne arkitekturspørsmål; dei er ikkje utførte av denne endringa.
