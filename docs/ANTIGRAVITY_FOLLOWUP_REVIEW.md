# Vurdering av Antigravity-framlegget

## Konklusjon

Framlegget har gode mål, men er ikkje ei trygg implementasjonsplan slik det står. Det blandar
produktarbeid, ein stor intern refaktor, Android-eigarpolicy og dokumentopprydding i éi levering.
Nokre påstandar er allereie forelda i denne arbeidsmappa: det finst 258 dokumentfiler, ikkje 95,
og DTS har ein konkret instrumentert regresjonstest. Skjermfesting (B-1) er uttrykkeleg utelate på
brukaren si melding.

Gjort no:

- S-1: Ny PIN brukar 100 000 PBKDF2-HMAC-SHA256-iterasjonar. Gyldige V2-PIN-ar med lågare kostnad
  blir styrkte etter korrekt kontroll, og øydelagde/ulesbare lagra PIN-data feilar lukka.
- B-2: Lokal, profilavgrensa leggjetid med foreldrekontroll, 60-sekundars kontroll medan
  barneskalet er ope, og `BedtimeScreen` etter pågåande episode.
- P-1: [Emby-avspelingstestmatrise](EMBY_PLAYBACK_TEST_MATRIX.md) dokumenterer åtte edge-casar og
  den eksisterande DTS-regresjonstesten.

## A-1 og A-2: starta, med vidare oppdelingsplan

`ReelstackViewModel` er om lag 3000 linjer og skal delast, men det er ein separat refaktor med eit stabilt
åtferdsgrunnlag først. Ingen brukar er i produksjon enno, så det finst ingen migreringsomsyn for
offentlege API-ar; likevel blir kvar flytting kompilert og testa før neste klynge flyttar.

**Første til femte pass, 21. september 2026, er gjort:**

- `ui/state/HomeUiState.kt` (82 linjer) avgrensar nøyaktig kva Home kan lese. `HomeScreen` brukar
  kontrakta direkte; den gamle `ReelstackUiState`-overloaden er berre ein mellombels testadapter.
- `ReelstackViewModel.homeUiState` er ein eigen `StateFlow`, så PIN, ark og søk som ikkje endrar
  Home-felta ikkje gjer Home om att.
- `ReelstackApp.kt` er redusert frå 739 til 606 linjer. Navigasjonsraden ligg i
  `ui/AppNavigationRail.kt` (155) og den gjenbrukte fokus-/navigasjonskontrollen i
  `ui/NavigationControls.kt` (61).
- `ui/HomeFeedCoordinator.kt` eig no Home sine avbrotsjobbar, cache-hydrering,
  stale-while-revalidate-feed, avspelingsoppdatering, Jellyfin sin valfrie
  varslingskanal, retry-klokke og Home sine radval. Roten delegerer med smale
  funksjonsgrenser for kontoar, førespurnader og detaljark, og er redusert til 2 733 linjer.
  Profilbyte avbryt no alle tre Home-jobbane samla, lukkar den gamle autentiserte
  Jellyfin-kanalen og opnar kanalen på nytt for den nye profilen før ho får laste innhald.
- `ui/AccountRefreshCoordinator.kt` eig no profilnamn-/avataroppdateringar og si eiga jobb-
  avbryting. `ProfileViewModel` er framleis eigar av PIN og sjølve profilbytet; det nye laget
  les berre den aktive profilen etter at valet er gjort.
- `ui/state/DiscoverUiState.kt` (51 linjer) avgrensar Discover og det globale søket på same
  måte som Home. `DiscoverScreen` tek kontrakta direkte, med ein mellombels adapter berre for
  eksisterande UI-testar.
- `ui/DiscoverSearchCoordinator.kt` (160 linjer) eig 350-ms debounce, parallelle bibliotek-/
  Seerr-søk, paginering og avbryting. Både første søk og «last meir» blir no avbrotne ved
  profilbyte, utlogging og fjerning av teneste; gamle profilar kan difor ikkje skrive attende
  treff i den nye profilen. `DiscoverUiStateTest` karakteriserer den avgrensa kontrakta.
- `ui/AppOverlayHost.kt` (90 linjer) samlar profilmeny, alle ark, bibliotekvala og
  oppdateringsbanneret over destinasjonane. Det gjer at `ReelstackApp` berre eig vindauge-,
  livssyklus- og TV-fokuspolitikk, medan ark framleis kan overleve eit tabbyte.

Det er medvite at andre passet heiter `HomeFeedCoordinator`, ikkje Android sin `ViewModel`:
Home må framleis gjere atomiske oppdateringar av den felles, eldre `ReelstackUiState` medan
Library, Profile og Discover ikkje er skilde. Å lage ein kunstig, parallell `ViewModel` no ville
ha gitt to moglege eigarar av same liste. Når resten av state er delt, kan koordinatoren få si eiga
`StateFlow<HomeUiState>` og bli den endelege `HomeViewModel` utan å endre Home-komposablar.

| Klynge | Ny eigar | Les frå | Skriv til | Første karakteriseringstest |
| --- | --- | --- | --- | --- |
| Heim og avspelingar | `HomeFeedCoordinator` → `HomeViewModel` / `HomeUiState` | synkroniserte rader, aktiv økt | opne detalj/avspeling | profilbyte held radene skilde |
| Bibliotek | `LibraryViewModel` / `LibraryUiState` | bibliotekside og filter | sti, filter, last meir | filter + tilbake gjev same sti |
| Oppdag og globalt søk | `DiscoverViewModel` / `DiscoverUiState` | Jellyfin, Emby og Seerr | debounce, kansellering, side | sein respons kan ikkje overskrive ny søkjetekst |
| Kontoar, profilar og barn | `ProfileViewModel` / `ProfileUiState` | profilregister, PIN, tenester | byte, utlogging, PIN-flyt | ulesbar PIN kan ikkje opne vaksenprofil |
| App-orkestrering | `ReelstackViewModel` som tynn rot | understraumar | ark, navigasjon og snackbar | éin UI-hending går berre til éin eigar |

Målstrukturen etter at karakteriseringstestane er på plass er:

```text
ui/viewmodels/
  ReelstackViewModel.kt       # rot, navigasjon og samansetjing
  HomeViewModel.kt
  LibraryViewModel.kt
  DiscoverViewModel.kt
  ProfileViewModel.kt
  SettingsViewModel.kt
ui/state/
  HomeUiState.kt
  LibraryUiState.kt
  DiscoverUiState.kt
  ProfileUiState.kt
```

Den første, avgrensa kontrakta bør sjå slik ut. Ho gjer at Home eig sine data utan at
`ReelstackApp.kt` må kjenne til lagrings- eller nettverksdetaljar:

```kotlin
data class HomeUiState(
    val resume: List<LibraryMedia> = emptyList(),
    val nextUp: List<LibraryMedia> = emptyList(),
    val favourites: List<LibraryMedia> = emptyList(),
    val refreshing: Boolean = false,
    val failedServices: Set<ServiceKind> = emptySet(),
)

class HomeViewModel(private val homeRepository: HomeRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = homeRepository.home
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() = viewModelScope.launch { homeRepository.refresh() }
}
```

Roten skal berre setje saman state og sende kommandoar til rett eigar. Han skal ikkje kopiere
Home-synkroniseringa:

```kotlin
data class AppUiState(
    val home: HomeUiState = HomeUiState(),
    val library: LibraryUiState = LibraryUiState(),
    val profile: ProfileUiState = ProfileUiState(),
)

class ReelstackViewModel(
    private val home: HomeViewModel,
    private val library: LibraryViewModel,
    private val profile: ProfileViewModel,
) : ViewModel() {
    val uiState = combine(home.uiState, library.uiState, profile.uiState) { h, l, p ->
        AppUiState(home = h, library = l, profile = p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    fun refreshHome() = home.refresh()
}
```

Dette er målkontraktar, ikkje kode som skal limast inn i éin omgang. Prosjektet har allereie
`LibraryViewModel` (222 linjer), `ProfileViewModel` (253) og `RequestsViewModel` (68); dei skal
nyttast vidare, ikkje lagast på nytt. Home og Discover sine Compose-kontraktar og observeringsstraumar
er no på plass. Attståande steg er å flytte Library og innstillingar til tilsvarande avgrensa
kontraktar, og til slutt ark- og navigasjonsorkestreringa.

Del deretter resten av `ReelstackApp.kt` (660 linjer) etter destinasjon, ikkje etter vilkår:
`HomeDestination`, `LibraryDestination`, `DiscoverDestination`, `SettingsDestination` og
`AppSheetHost`. Ikkje set vilkårlege linjetal som godkjenning. Kvar flytting skal ha éin eigar og
ei karakteriseringstest; endeleg linjetal er eit resultat, ikkje eit mål.

## B-1 er ikkje med

`startLockTask()` på ei vanleg, ikkje-administrert Android-eining kan bli avbroten av systemet sitt
eige avfestingsløp. Appen kan difor ikkje truverdig love at den same Spole-PIN-en er einaste veg ut.
Slik åtferd krev ein eigarstyrt/administrert eining og eit eige produktval for provisioning. Ho skal
ikkje merkast som foreldretryggleik før det valet er teke.

## AI-1: dokumentarkiv

Ikkje flytt eit førehandsbestemt tal filer. Først lag ei maskinlesbar oversikt med eigar, sist
verifisert dato, status (`gjeldande`, `historisk`, `release-artefakt`) og lenkjer inn. Berre filer
som er både historiske og utan inngåande lenkjer kan flyttast til `docs/archive/YYYY/`; kvar flytting
skal oppdatere indeks og lenkjekontroll. `docs/INDEX.md` bør bli laga frå denne oversikta, ikkje
handskrivast frå eit ubekrefta tal.
