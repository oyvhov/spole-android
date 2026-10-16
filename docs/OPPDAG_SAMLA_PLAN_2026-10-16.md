# Samla Oppdag: implementasjonsplan

> **For agentar:** Påkravd underferdigheit: superpowers:executing-plans (eller
> superpowers:subagent-driven-development). Stega brukar avkryssing (`- [ ]`).

**Mål:** Slå Oppdag og Aktivitet saman til éin Seerr-stad med rader, folk, personside og eit
førespurnadsark med Utgåve, Kvalitet og Lagringsstad, og sleppe det som 1.0.0-beta14.

**Arkitektur:** Ny `SeerrDiscoverClient` for dei nye Seerr-endepunkta, ein `DiscoverHubCoordinator`
som lastar kvar rad sjølvstendig inn i `ReelstackUiState.discoverHub`, og nye Compose-filer for
Oppdag-sida, «Dine førespurnader» og personsida. Aktivitet-fana forsvinn; varsel og sending peikar på
Oppdag. Førespurnadsarket får val som berre visest når Seerr-rettane tillèt det.

**Teknologi:** Kotlin, Jetpack Compose (Material 3), kotlinx.serialization, kotlinx-coroutines,
JUnit 4 med Robolectric for Compose-einingstestar, instrumentering på isolert TV-profil 5566.

**Spesifikasjon:** [OPPDAG_SAMLA_2026-10-16.md](OPPDAG_SAMLA_2026-10-16.md)

## Globale krav

- Arbeidsmappe `C:/JellyBin/spole-sesong`, grein `oppdag-samla`, basert på `origin/main` (beta13, kode 150).
- Brukartekst på nynorsk i `values-b+nn`, bokmål i `values-b+nb`, engelsk i `values`; same nøklar i alle tre
  (`TranslationParityTest`).
- Lime (`Primary`) berre på valt fane og «Send førespurnad». Framdrift i tekstfarge. Ingen fylte knappar på Oppdag.
- Ingen fast høgd rundt tekst; alle nye skjermar skal tole skriftstorleik 2.0.
- Skilje i lister: 1 dp, `TextColor.copy(alpha = .08f)`.
- Seerr-kall: `api/v1/...` med `EndpointValidator.resolve`, Seerr-hovud frå personleg økt eller API-nøkkel.
- Barnemodus skal ikkje hente Seerr (uendra: barneskalet viser ikkje Oppdag).
- Ingen instrumentering på 5560/5564. Android-testar berre på 5566.

**Kommando G (einingstestar, PowerShell i `C:\JellyBin\spole-sesong`):**

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot'; $env:GRADLE_USER_HOME='C:\JellyBin\.gradle-home'; $env:TEMP='C:\JellyBin\.gradle-tmp'; $env:TMP=$env:TEMP; $env:JAVA_TOOL_OPTIONS='-Djava.io.tmpdir=C:\JellyBin\.gradle-tmp'
./gradlew.bat testDebugUnitTest --tests "<MØNSTER>" --console=plain
```

**Kommando B (APK-ar):** same miljø, `./gradlew.bat assembleDebug assembleDebugAndroidTest --console=plain --max-workers=2`

**Kommando A (Android-test på 5566, etter B):**
`wsl.exe -u root -e bash /mnt/c/JellyBin/.spole-tools/test-5566.sh <Klasse>[#metode] ...`

## Fokus for gjennomgang

1. **Seerr med API-nøkkel (ikkje personleg økt):** radene skal laste; «Dine førespurnader» er tom og
   «Be om» ber om innlogging. Test i Task 4 (`rowsLoadWithAnApiKey`).
2. **4K-serie:** sesongstatus skal lesast frå 4K-lista når Utgåve er 4K, både i arket og i sjekken før
   POST. Test i Task 9 (`fourKSeriesChecksFourKSeasons`).
3. **Profilbyte medan rader lastar:** seine svar skal ikkje hamne i ein annan profil. Test i Task 4
   (`lateRowsFromAnotherScopeAreDropped`).
4. **Lagra meny med «ACTIVITY»:** `menu_order`/`menu_hidden` frå før skal reinskast utan krasj. Test i
   Task 1 (`storedActivityIsDroppedFromTheMenu`).
5. **Skriftstorleik 2.0 på telefon:** faner og liner skal bryte, ikkje klippast. Test i Task 2
   (`tabsAndLinesSurviveLargeText`).

---

### Task 1: Aktivitet-fana ut, djuplenkje og sending

**Filer:**
- Endre: `app/src/main/java/app/reelstack/data/model/Personalization.kt:157`
- Endre: `app/src/main/java/app/reelstack/ui/ReelstackViewModel.kt` (`AppTab` linje 64, `ReelstackUiState` ved `showRequestHistory` linje 289, `sendRequest` linje 3169–3253)
- Endre: `app/src/main/java/app/reelstack/ui/ReelstackApp.kt` (polling 257–267, ruting 490–493, `tabs` 560–567, korte namn 588–595)
- Endre: `app/src/main/java/app/reelstack/MainActivity.kt:113-116`
- Endre: `app/src/main/java/app/reelstack/ui/components/TvMenuSettings.kt:31-33,102-108`
- Test: `app/src/test/java/app/reelstack/data/model/TouchMenuTest.kt`, `.../data/network/TvRefinementTest.kt`, `.../ui/screens/NavigationSettingsUiTest.kt`, androidTest med `ACTIVITY`

**Grensesnitt:**
- Produserer: `ReelstackUiState.myRequestsOpen: Boolean`, `ReelstackViewModel.openMyRequests()`, `ReelstackViewModel.closeMyRequests()`.

- [ ] **Steg 0: Hjelpeskript utanfor build-mappa**

```bash
mkdir -p /c/JellyBin/.spole-tools && cp /c/JellyBin/spole-sesong/app/build/beta9/test-5566.sh /c/JellyBin/.spole-tools/test-5566.sh && sed -i 's#\$B/beta9/#/mnt/c/JellyBin/.spole-tools/#g' /c/JellyBin/.spole-tools/test-5566.sh && grep -n "spole-tools" /c/JellyBin/.spole-tools/test-5566.sh
```

Forventa: linjene for logg og `OUT` peikar på `/mnt/c/JellyBin/.spole-tools/`.

- [ ] **Steg 1: Skriv den feilande testen**

I `TouchMenuTest.kt`, legg til:

```kotlin
@Test fun storedActivityIsDroppedFromTheMenu() {
    val stored = Personalization(menuOrder = listOf("ACTIVITY", "DISCOVER", "HOME", "LIBRARY", "SETTINGS"),
        hiddenMenuItems = setOf("ACTIVITY"))
    assertEquals(listOf("DISCOVER", "HOME", "LIBRARY", "SETTINGS"), stored.visibleMenu())
    assertEquals(listOf("HOME", "LIBRARY", "DISCOVER", "SETTINGS"), DEFAULT_MENU)
    assertTrue(stored.withMenuVisible("ACTIVITY", true).hiddenMenuItems.isEmpty())
}
```

- [ ] **Steg 2: Køyr og sjå at han feilar**

Kommando G med mønster `app.reelstack.data.model.TouchMenuTest`. Forventa: FAIL (`DEFAULT_MENU` har framleis ACTIVITY).

- [ ] **Steg 3: Implementer**

`Personalization.kt:157`:

```kotlin
val DEFAULT_MENU = listOf("HOME", "LIBRARY", "DISCOVER", "SETTINGS")
```

`ReelstackViewModel.kt:64`:

```kotlin
enum class AppTab { HOME, LIBRARY, DOWNLOADS, DISCOVER, SETTINGS }
```

I `ReelstackUiState`, rett under `val showRequestHistory: Boolean = false,`:

```kotlin
    /** «Dine førespurnader» is a page inside Oppdag; a notification or a tap on «Alle» opens it. */
    val myRequestsOpen: Boolean = false,
```

I `ReelstackViewModel`, rett etter `selectTab`:

```kotlin
    /** Oppdag with «Dine førespurnader» open: where a request notification lands. */
    fun openMyRequests() {
        selectTab(AppTab.DISCOVER)
        _uiState.update { it.copy(myRequestsOpen = true) }
        if (_uiState.value.requestHistory.items.isEmpty()) loadRequestHistory(false)
    }

    fun closeMyRequests() = _uiState.update { it.copy(myRequestsOpen = false) }
```

I `sendRequest`, i suksessgreina (linje ~3239–3241), byt

```kotlin
                        activeSheet = null,
                        requestDraft = null,
                        selectedTab = AppTab.ACTIVITY,
```

med

```kotlin
                        // Back to the title it was asked from, whose status line now says «Førespurd».
                        activeSheet = if (current.contentDetails?.key == id) AppSheet.TitleDetails(id) else null,
                        requestDraft = null,
```

`ReelstackApp.kt` polling (linje ~263):

```kotlin
                    if (viewModel.uiState.value.selectedTab == AppTab.DISCOVER) 30_000L else 300_000L,
```

`ReelstackApp.kt` ruting: fjern `AppTab.ACTIVITY -> ActivityScreen(...)`-greina og la `AppTab.DISCOVER` velje side:

```kotlin
                    AppTab.DISCOVER -> if (state.myRequestsOpen) ActivityScreen(state, screenInsets, viewModel::openActivityDetails,
                        viewModel::setFollowNotification, viewModel::refreshTrackedRequests, viewModel::openSeerrAccount,
                        viewModel::cancelTrackedRequest, viewModel::openRequestHistory,
                        viewModel::closeRequestHistory, viewModel::loadRequestHistory)
                    else DiscoverScreen(
```

(resten av `DiscoverScreen(...)`-kallet som før). Legg til ein tilbakehandsamar rett etter
`BackHandler(enabled = state.globalSearchOpen)`:

```kotlin
    BackHandler(enabled = state.myRequestsOpen && state.selectedTab == AppTab.DISCOVER && state.activeSheet == null && !state.globalSearchOpen) {
        viewModel.closeMyRequests()
    }
```

`tabs`: fjern linja med `AppTab.ACTIVITY`. Korte namn: fjern `AppTab.ACTIVITY -> R.string.nav_activity_short`.

`MainActivity.kt:114`:

```kotlin
                    if (pendingRequests) reelstackViewModel.openMyRequests()
```

`TvMenuSettings.kt`: `names` utan ACTIVITY, og Oppdag med same ikon som fana:

```kotlin
    val names = mapOf("HOME" to stringResource(R.string.nav_home), "LIBRARY" to stringResource(R.string.nav_library),
        "DISCOVER" to stringResource(R.string.nav_discover), "SETTINGS" to stringResource(R.string.nav_settings))
```

og i `menuIcon` byt `"DISCOVER" -> SpoleIcons.Search` med `"DISCOVER" -> SpoleIcons.Discover` og fjern `"ACTIVITY" -> ...`.

Køyr så `grep -rn "AppTab.ACTIVITY\|\"ACTIVITY\"" app/src` og rett kvar treff: i testar blir
`menu-*-ACTIVITY` til `menu-*-DISCOVER` og ordningslister mistar `"ACTIVITY"`; `wide-tab-ACTIVITY`
i `TvNavigationIntegrationTest` blir fjerna saman med steget som brukar det.

- [ ] **Steg 4: Køyr testane**

Kommando G med mønster `app.reelstack.data.model.TouchMenuTest` og så `app.reelstack.data.network.TvRefinementTest`
og `app.reelstack.ui.screens.NavigationSettingsUiTest`. Forventa: PASS.

- [ ] **Steg 5: Commit**

```bash
git add -A app/src && git commit -m "Fold Activity into Discover navigation" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: «Dine førespurnader» som liner med faner og Tenaren

**Filer:**
- Opprett: `app/src/main/java/app/reelstack/ui/screens/MyRequestsScreen.kt`
- Opprett: `app/src/main/java/app/reelstack/data/model/MyRequests.kt`
- Opprett: `app/src/main/res/values/discover_hub.xml`, `values-b+nn/discover_hub.xml`, `values-b+nb/discover_hub.xml`
- Endre: `app/src/main/java/app/reelstack/ui/ReelstackApp.kt` (DISCOVER-greina frå Task 1)
- Slett: `ActivityScreen`, `ActivityScopeMenu`, `ActivityRow`, `ActivityFilter`, `PersonalActivityFilter` i `SecondaryScreens.kt`; `ui/screens/RequestHistoryScreen.kt`; `ui/components/ActivityRequestCard.kt`; `TrackedRequestCard` i `RequestFlowUi.kt` når han er ubrukt
- Test: `app/src/test/java/app/reelstack/data/model/MyRequestsTest.kt`, `app/src/test/java/app/reelstack/ui/screens/MyRequestsScreenTest.kt`

**Grensesnitt:**
- Konsumerer: `myRequestsOpen`, `closeMyRequests()` (Task 1); `TrackedRequest`, `RequestStage`, `groupActivityRequests`, `activityDayGroup`.
- Produserer: `enum class MyRequestsTab { ALL, IN_PROGRESS, READY, SERVER }`; `fun myRequestsPreview(requests: List<TrackedRequest>, limit: Int): List<TrackedRequest>`; `fun myRequestsFor(tab, requests): List<TrackedRequest>`; `@Composable fun MyRequestsScreen(...)`; `@Composable internal fun RequestLine(request: TrackedRequest, onClick: () -> Unit, modifier: Modifier = Modifier)`.

- [ ] **Steg 1: Skriv dei feilande testane (modell)**

`MyRequestsTest.kt`:

```kotlin
package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MyRequestsTest {
    private fun request(key: String, stage: RequestStage, updated: Long, title: String = key) =
        TrackedRequest(key, mediaId = key.hashCode().and(0xffff), mediaType = "movie", title = title,
            artworkUrl = "https://image.tmdb.org/t/p/w500/$key.jpg", seasons = emptySet(), stage = stage, updatedAt = updated)

    @Test fun inFlightComesFirstThenTheLatestReady() {
        val list = listOf(request("a", RequestStage.AVAILABLE, 10), request("b", RequestStage.DOWNLOADING, 1),
            request("c", RequestStage.AVAILABLE, 30), request("d", RequestStage.REQUESTED, 2))
        assertEquals(listOf("d", "b", "c"), myRequestsPreview(list, 3).map { it.key })
    }

    @Test fun tabsSplitByStage() {
        val list = listOf(request("a", RequestStage.AVAILABLE, 1), request("b", RequestStage.IMPORTING, 2))
        assertEquals(listOf("b"), myRequestsFor(MyRequestsTab.IN_PROGRESS, list).map { it.key })
        assertEquals(listOf("a"), myRequestsFor(MyRequestsTab.READY, list).map { it.key })
        assertEquals(2, myRequestsFor(MyRequestsTab.ALL, list).size)
    }
}
```

- [ ] **Steg 2: Køyr og sjå at han feilar**

Kommando G, mønster `app.reelstack.data.model.MyRequestsTest`. Forventa: FAIL (ukjende funksjonar).

- [ ] **Steg 3: Implementer modellen**

`MyRequests.kt`:

```kotlin
package app.reelstack.data.model

/** The tabs of «Dine førespurnader». SERVER is the administrator's view of the whole server. */
enum class MyRequestsTab { ALL, IN_PROGRESS, READY, SERVER }

private val TrackedRequest.ready: Boolean get() = stage == RequestStage.AVAILABLE

/** On the way first, oldest wait first; then what arrived most recently. */
fun myRequestsPreview(requests: List<TrackedRequest>, limit: Int): List<TrackedRequest> {
    val titled = requests.filter { it.hasTitleMetadata }
    val waiting = titled.filterNot { it.ready }.sortedBy { it.updatedAt }
    val arrived = titled.filter { it.ready }.sortedByDescending { it.updatedAt }
    return (waiting + arrived).take(limit)
}

fun myRequestsFor(tab: MyRequestsTab, requests: List<TrackedRequest>): List<TrackedRequest> = when (tab) {
    MyRequestsTab.ALL -> myRequestsPreview(requests, Int.MAX_VALUE) + requests.filterNot { it.hasTitleMetadata }
    MyRequestsTab.IN_PROGRESS -> requests.filter { !it.ready && it.hasTitleMetadata }.sortedBy { it.updatedAt }
    MyRequestsTab.READY -> requests.filter { it.ready && it.hasTitleMetadata }.sortedByDescending { it.updatedAt }
    MyRequestsTab.SERVER -> emptyList()
}
```

- [ ] **Steg 4: Køyr modelltesten**

Kommando G, mønster `app.reelstack.data.model.MyRequestsTest`. Forventa: PASS.

- [ ] **Steg 5: Strengar**

`values-b+nn/discover_hub.xml` (og tilsvarande `values-b+nb` med «Dine forespørsler», «På vei», «Serveren»,
«Du har ikke bedt om noe ennå.», «Forespørsel nr. %1$d», «Hent eldre»; og `values` med «Your requests»,
«All», «On the way», «Ready», «Server», «You haven't requested anything yet.», «Request no. %1$d», «Load older»):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="my_requests_title">Dine førespurnader</string>
    <string name="my_requests_tab_all">Alle</string>
    <string name="my_requests_tab_in_progress">På veg</string>
    <string name="my_requests_tab_ready">Klare</string>
    <string name="my_requests_tab_server">Tenaren</string>
    <string name="my_requests_empty">Du har ikkje bede om noko enno.</string>
    <string name="my_requests_unresolved">Førespurnad nr. %1$d</string>
    <string name="my_requests_older">Hent eldre</string>
</resources>
```

- [ ] **Steg 6: Skriv den feilande UI-testen**

`app/src/test/java/app/reelstack/ui/screens/MyRequestsScreenTest.kt`:

```kotlin
package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h900dp-xhdpi", application = app.reelstack.SheetTestApplication::class)
class MyRequestsScreenTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val requests = listOf(
        TrackedRequest("a", 1, "movie", "Coraline", "https://image.tmdb.org/t/p/w500/a.jpg", emptySet(), stage = RequestStage.AVAILABLE, updatedAt = 5),
        TrackedRequest("b", 2, "tv", "Severance", "https://image.tmdb.org/t/p/w500/b.jpg", setOf(3), stage = RequestStage.DOWNLOADING, percent = 64, updatedAt = 1),
    )

    private fun show(admin: Boolean = false, fontScale: Float = 1f, opened: MutableList<String> = mutableListOf()) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                ReelstackTheme {
                    MyRequestsScreen(ReelstackUiState(trackedRequests = requests, adminView = admin), PaddingValues(),
                        onOpen = { opened += it }, onBack = {}, onLoadOlder = {})
                }
            }
        }
    }

    @Test fun linesOpenTheTitleAndHaveNoButtons() {
        val opened = mutableListOf<String>()
        show(opened = opened)
        rule.onNodeWithTag("my-request-b").assertIsDisplayed().performClick()
        assertEquals(listOf("b"), opened)
        rule.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag("my-request-b"))).assertCountEquals(0)
    }

    @Test fun serverTabIsForAdministratorsOnly() {
        show(admin = false)
        rule.onNodeWithTag("my-requests-tab-SERVER").assertDoesNotExist()
    }

    @Test fun tabsAndLinesSurviveLargeText() {
        show(fontScale = 2f)
        rule.onNodeWithTag("my-requests-tab-READY").assertIsDisplayed()
        rule.onNodeWithText("Severance", substring = true).assertIsDisplayed()
    }
}
```

- [ ] **Steg 7: Køyr og sjå at han feilar**

Kommando G, mønster `app.reelstack.ui.screens.MyRequestsScreenTest`. Forventa: FAIL (`MyRequestsScreen` finst ikkje).

- [ ] **Steg 8: Implementer skjermen**

`MyRequestsScreen.kt`:

```kotlin
package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.mediaCardIndication
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.ReelPage
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor

private val LineDivider @Composable get() = TextColor.copy(alpha = .08f)

/** Requests as lines. Opening one goes to its title page, where every action on it lives. */
@Composable
fun MyRequestsScreen(state: ReelstackUiState, contentPadding: PaddingValues, onOpen: (String) -> Unit,
    onBack: () -> Unit, onLoadOlder: () -> Unit, onEvent: (String) -> Unit = {}) {
    val tabs = MyRequestsTab.entries.filter { it != MyRequestsTab.SERVER || state.adminView }
    var tab by rememberSaveable { mutableStateOf(MyRequestsTab.ALL) }
    val older = state.requestHistory.items.filterNot { old ->
        state.trackedRequests.any { it.mediaId == old.mediaId && it.mediaType == old.mediaType && it.is4k == old.is4k }
    }
    val lines = when (tab) {
        MyRequestsTab.ALL -> myRequestsFor(tab, state.trackedRequests) + older
        else -> myRequestsFor(tab, state.trackedRequests)
    }
    val list = rememberLazyListState()
    ReelPage(media = true) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("my-requests"),
            contentPadding = PaddingValues(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp)) {
            item(key = "head") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 0.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("my-requests-back")) {
                        Icon(SpoleIcons.ArrowBack, stringResource(R.string.action_back))
                    }
                    Text(stringResource(R.string.my_requests_title), style = MaterialTheme.typography.headlineSmall)
                }
            }
            item(key = "tabs") {
                Column {
                    FlowTabs(tabs, tab, { tab = it }) { choice ->
                        val count = when (choice) {
                            MyRequestsTab.SERVER -> null
                            MyRequestsTab.ALL -> state.trackedRequests.size + older.size
                            else -> myRequestsFor(choice, state.trackedRequests).size
                        }
                        stringResource(when (choice) {
                            MyRequestsTab.ALL -> R.string.my_requests_tab_all
                            MyRequestsTab.IN_PROGRESS -> R.string.my_requests_tab_in_progress
                            MyRequestsTab.READY -> R.string.my_requests_tab_ready
                            MyRequestsTab.SERVER -> R.string.my_requests_tab_server
                        }) to count
                    }
                    HorizontalDivider(color = LineDivider)
                    state.trackingError?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp)) }
                }
            }
            if (tab == MyRequestsTab.SERVER) {
                var lastGroup: Int? = null
                state.activity.forEach { event ->
                    val group = app.reelstack.data.model.activityDayGroup(event)
                    if (group != lastGroup) {
                        lastGroup = group
                        item(key = "day-${event.id}") {
                            Text(stringResource(group), color = Muted, style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                        }
                    }
                    item(key = "event-${event.id}") { ServerEventLine(event) { onEvent(event.id) } }
                }
            } else if (lines.isEmpty()) {
                item(key = "empty") {
                    Text(stringResource(R.string.my_requests_empty), color = Muted, modifier = Modifier.padding(vertical = 24.dp))
                }
            } else {
                items(lines, key = { "line-${it.key}" }) { request -> RequestLine(request, { onOpen(request.key) }) }
                if (tab == MyRequestsTab.ALL && state.requestHistory.hasMore) item(key = "older") {
                    LaunchedEffect(state.requestHistory.items.size) { onLoadOlder() }
                    Text(stringResource(R.string.my_requests_older), color = Muted, modifier = Modifier.padding(vertical = 18.dp))
                }
            }
        }
    }
}

/** Text tabs with a line under the chosen one. Wraps at large text instead of clipping. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowTabs(tabs: List<MyRequestsTab>, selected: MyRequestsTab, onSelect: (MyRequestsTab) -> Unit,
    label: @Composable (MyRequestsTab) -> Pair<String, Int?>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(top = 4.dp)) {
        tabs.forEach { choice ->
            val (text, count) = label(choice)
            val interaction = remember { MutableInteractionSource() }
            val chosen = choice == selected
            Column(Modifier.selectable(chosen, interactionSource = interaction, indication = mediaCardIndication(),
                    role = Role.Tab) { onSelect(choice) }
                .focusOutline(interaction, RoundedCornerShape(8.dp)).padding(vertical = 10.dp)
                .testTag("my-requests-tab-${choice.name}")) {
                Text(if (count == null) text else "$text  $count", color = if (chosen) TextColor else Muted,
                    style = MaterialTheme.typography.titleSmall)
                Box(Modifier.padding(top = 6.dp).height(2.dp).fillMaxWidth()
                    .background(if (chosen) TextColor else androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
}

/** One request: cover, title, status and, while on the way, a thin line of progress. */
@Composable
internal fun RequestLine(request: TrackedRequest, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    Column(modifier.fillMaxWidth().clickable(interactionSource = interaction, indication = mediaCardIndication(),
            role = Role.Button, onClick = onClick).focusOutline(interaction, RoundedCornerShape(10.dp))
        .testTag("my-request-${request.key}")) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MediaArtwork(request.artworkUrl, null, Modifier.width(48.dp).aspectRatio(2f / 3f).clip(shape).background(SurfaceRaised),
                fallbackRes = R.drawable.media_placeholder, contentScale = ContentScale.Crop,
                source = app.reelstack.data.model.ServiceKind.SEERR)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (request.hasTitleMetadata) request.title else
                    stringResource(R.string.my_requests_unresolved, request.requestId ?: request.mediaId),
                    style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(requestLineStatus(request), color = Muted, style = MaterialTheme.typography.bodySmall)
                if (request.stage == RequestStage.DOWNLOADING && request.percent != null) {
                    Box(Modifier.padding(top = 2.dp).fillMaxWidth().height(2.dp).background(TextColor.copy(alpha = .12f))) {
                        Box(Modifier.fillMaxWidth(request.percent / 100f).height(2.dp).background(TextColor))
                    }
                }
            }
        }
        HorizontalDivider(color = LineDivider)
    }
}

@Composable
internal fun requestLineStatus(request: TrackedRequest): String = listOfNotNull(
    request.seasons.sorted().takeIf { it.isNotEmpty() }?.let { seasons ->
        androidx.compose.ui.res.pluralStringResource(R.plurals.my_requests_seasons, seasons.size, seasons.joinToString(", ")) },
    app.reelstack.localization.requestStageLabel(request.stage),
    request.percent?.takeIf { request.stage == RequestStage.DOWNLOADING }?.let { "$it %" },
).joinToString("  ·  ")

@Composable
private fun ServerEventLine(event: ActivityEvent, onClick: () -> Unit) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().clickable(interactionSource = interaction, indication = mediaCardIndication(),
        role = Role.Button, onClick = onClick).focusOutline(interaction, RoundedCornerShape(10.dp))
        .testTag("server-event-${event.id}")) {
        Text(event.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
        Text(listOfNotNull(event.time.text(context), event.detail.text(context), event.source?.displayName)
            .filter { it.isNotBlank() }.joinToString("  ·  "),
            color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp, bottom = 10.dp))
        HorizontalDivider(color = LineDivider)
    }
}
```

Legg til i dei tre `discover_hub.xml` (`values`: «Season %1$s» / «Seasons %1$s»; `nb`: «Sesong %1$s» / «Sesong %1$s»):

```xml
    <plurals name="my_requests_seasons">
        <item quantity="one">Sesong %1$s</item>
        <item quantity="other">Sesong %1$s</item>
    </plurals>
```

`ReelstackApp.kt`, DISCOVER-greina:

```kotlin
                    AppTab.DISCOVER -> if (state.myRequestsOpen) app.reelstack.ui.screens.MyRequestsScreen(state, screenInsets,
                        onOpen = viewModel::openActivityDetails, onBack = viewModel::closeMyRequests,
                        onLoadOlder = { viewModel.loadRequestHistory(true) }, onEvent = viewModel::openActivityDetails)
                    else DiscoverScreen(
```

Fjern så `ActivityScreen`, `ActivityScopeMenu`, `ActivityRow`, `ActivityFilter`, `PersonalActivityFilter`
og `localizedLabel`-funksjonane deira frå `SecondaryScreens.kt`, `RequestHistoryScreen.kt`,
`ActivityRequestCard.kt`, og `TrackedRequestCard` når `grep -rn "TrackedRequestCard(" app/src/main` er tom.
`openActivityDetails` skal ikkje lenger leggje titlar inn i `discover`: byt linje ~2472

```kotlin
            _uiState.update { it.copy(discover = it.discover.filterNot { entry -> entry.id == media.id } + media, searchQuery = "") }
            openDiscoverDetails(media.id)
```

med

```kotlin
            _uiState.update { it.copy(detailMedia = media) }
            openDiscoverDetails(media.id)
```

og legg `val detailMedia: DiscoverMedia? = null,` til `ReelstackUiState` (under `myRequestsOpen`), og
`listOfNotNull(detailMedia) +` først i `knownDiscoverMedia`. I `openDiscoverDetails` byt første linje til

```kotlin
        val media = _uiState.value.knownDiscoverMedia.firstOrNull { it.id == id } ?: return
```

- [ ] **Steg 9: Køyr testane**

Kommando G, mønster `app.reelstack.ui.screens.MyRequestsScreenTest` og `app.reelstack.data.model.*`. Forventa: PASS.
Køyr så heile `testDebugUnitTest` (Kommando G utan `--tests`) og rett kompileringsfeil i testar som
brukte dei sletta komposisjonane: slett testar for `ActivityScreen`/`RequestHistoryScreen`/`TrackedRequestCard`
i `app/src/test` som berre prøvde den gamle UI-en.

- [ ] **Steg 10: Commit**

```bash
git add -A app/src && git commit -m "Show your requests as quiet lines inside Discover" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Seerr-oppdaging i nettverkslaget

**Filer:**
- Opprett: `app/src/main/java/app/reelstack/data/network/SeerrDiscoverClient.kt`
- Opprett: `app/src/main/java/app/reelstack/data/model/DiscoverHub.kt`
- Endre: `app/src/main/java/app/reelstack/data/network/ServicePayloadParser.kt` (`RemoteDiscoverItem` 138–151, `discover` 403–429, cast 529–534, nye funksjonar)
- Endre: `app/src/main/java/app/reelstack/data/network/ServiceClients.kt:1616` (`requireSuccess` → `internal`)
- Endre: `app/src/main/java/app/reelstack/data/model/Models.kt` (`DiscoverMedia` 212–226, `CastMember` 272)
- Endre: `app/src/main/java/app/reelstack/data/repository/MediaSyncRepository.kt` (konstruktør 115, `discoverMedia` 685–699)
- Test: `app/src/test/java/app/reelstack/data/network/SeerrDiscoverClientTest.kt`

**Grensesnitt:**
- Produserer: `enum class DiscoverRowKind(val path: String)`, `data class DiscoverGenre`, `data class DiscoverPerson`,
  `fun peopleFrom(casts: List<List<CastMember>>, perTitle: Int = 2, limit: Int = 12): List<DiscoverPerson>`,
  `SeerrDiscoverClient.row(connection, kind, page)`, `.genres(connection, mediaType)`, `.genrePage(connection, genre, page)`,
  `.similar(connection, mediaType, id)`, `.recommendations(connection, mediaType, id)`;
  `MediaSyncRepository.discoverRow(...)`, `.discoverGenres(...)`, `.discoverGenrePage(...)`, `.similar(...)`, `.recommended(...)`;
  `DiscoverMedia.backdropUrl: String?`, `CastMember.seerrPersonId: Int?`.

- [ ] **Steg 1: Skriv dei feilande testane**

`SeerrDiscoverClientTest.kt`:

```kotlin
package app.reelstack.data.network

import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Test

class SeerrDiscoverClientTest {
    private class Fake(private val bodies: Map<String, String>) : JsonHttpTransport {
        val asked = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            asked += url
            val key = bodies.keys.firstOrNull { url.contains(it) } ?: return HttpResponse(404, "{}")
            return HttpResponse(200, bodies.getValue(key))
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = HttpResponse(405, "{}")
    }

    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "cookie", userId = "7", sessionCookie = true)
    private val trending = """{"page":1,"totalPages":3,"results":[
        {"id":11,"mediaType":"movie","title":"Love of Your Life","releaseDate":"2026-10-07","posterPath":"/p.jpg","backdropPath":"/b.jpg","mediaInfo":{"status":2}},
        {"id":12,"mediaType":"person","name":"Not a title"}]}"""

    @Test fun aRowKeepsTitlesWithTheirBackdrop() {
        val page = SeerrDiscoverClient(Fake(mapOf("discover/trending" to trending))).row(seerr, DiscoverRowKind.TRENDING)
        assertEquals(listOf(11), page.items.map { it.remoteId })
        assertEquals("https://image.tmdb.org/t/p/w1280/b.jpg", page.items.single().backdropUrl)
        assertTrue(page.hasMore)
    }

    @Test fun genresComeWithABackdrop() {
        val genres = SeerrDiscoverClient(Fake(mapOf("genreslider/movie" to
            """[{"id":27,"name":"Grøssar","backdrops":["/g.jpg"]},{"id":35,"name":"Komedie","backdrops":[]}]"""))).genres(seerr, "movie")
        assertEquals(listOf("Grøssar", "Komedie"), genres.map { it.name })
        assertEquals("https://image.tmdb.org/t/p/w780/g.jpg", genres.first().backdropUrl)
        assertNull(genres.last().backdropUrl)
    }

    @Test fun castKeepsTheSeerrPersonId() {
        val details = ServicePayloadParser.mediaDetails("""{"title":"X","credits":{"cast":[{"id":501,"name":"Margaret Qualley","character":"Maya","profilePath":"/m.jpg"}]}}""")
        assertEquals(501, details.cast.single().seerrPersonId)
    }

    @Test fun peopleAreTakenTwoPerTitleWithoutRepeats() {
        fun cast(vararg ids: Int) = ids.map { CastMember("P$it", seerrPersonId = it) }
        assertEquals(listOf(1, 2, 3), peopleFrom(listOf(cast(1, 2, 9), cast(2, 3)), perTitle = 2).map { it.id })
    }
}
```

- [ ] **Steg 2: Køyr og sjå at testane feilar**

Kommando G, mønster `app.reelstack.data.network.SeerrDiscoverClientTest`. Forventa: FAIL (kompilering).

- [ ] **Steg 3: Modellane**

`DiscoverHub.kt`:

```kotlin
package app.reelstack.data.model

/** Seerr's rows on the Discover page, in the order they are drawn. */
enum class DiscoverRowKind(val path: String) {
    TRENDING("discover/trending"),
    UPCOMING_MOVIES("discover/movies/upcoming"),
    UPCOMING_SERIES("discover/tv/upcoming"),
}

/** One row's own state; a row that fails disappears without stopping the others. */
data class DiscoverRowState(
    val items: List<DiscoverMedia> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = false,
)

data class DiscoverPerson(val id: Int, val name: String, val portraitUrl: String?, val role: String? = null)

data class DiscoverGenre(val id: Int, val name: String, val mediaType: String, val backdropUrl: String?)

/** What a row's «Alle» opens. */
sealed interface DiscoverGridTarget {
    data class Row(val kind: DiscoverRowKind) : DiscoverGridTarget
    data class Genre(val genre: DiscoverGenre) : DiscoverGridTarget
}

data class DiscoverHubState(
    val rows: Map<DiscoverRowKind, DiscoverRowState> = emptyMap(),
    val people: List<DiscoverPerson> = emptyList(),
    val genres: List<DiscoverGenre> = emptyList(),
    val grid: DiscoverGridTarget? = null,
    val gridRow: DiscoverRowState = DiscoverRowState(),
    /** The profile and server the rows were loaded for; a late answer for another is dropped. */
    val scope: String? = null,
) {
    val allMedia: List<DiscoverMedia> get() = rows.values.flatMap { it.items } + gridRow.items
    val anyFailed: Boolean get() = rows.values.any { it.failed }
}

/** The first billed people of the first titles, without repeats. */
fun peopleFrom(casts: List<List<CastMember>>, perTitle: Int = 2, limit: Int = 12): List<DiscoverPerson> =
    casts.flatMap { cast -> cast.filter { it.seerrPersonId != null }.take(perTitle) }
        .distinctBy { it.seerrPersonId }
        .take(limit)
        .map { DiscoverPerson(it.seerrPersonId!!, it.name, it.portraitUrl, it.role) }
```

`Models.kt`: legg `val backdropUrl: String? = null,` sist i `DiscoverMedia`, og gjer `CastMember` til

```kotlin
data class CastMember(val name: String, val role: String? = null, val portraitUrl: String? = null,
    val remoteId: String? = null, val seerrPersonId: Int? = null)
```

- [ ] **Steg 4: Parseren**

I `ServicePayloadParser.kt`: legg `val backdropUrl: String? = null,` sist i `RemoteDiscoverItem`. Trekk
kroppen i `discover` ut i ein funksjon og bruk han:

```kotlin
    fun discover(payload: String): List<RemoteDiscoverItem> =
        json.parseToJsonElement(payload).jsonObject.array("results").mapNotNull { (it as? JsonObject)?.let(::discoverItem) }

    internal fun discoverItem(item: JsonObject): RemoteDiscoverItem? {
        val remoteId = item.int("id") ?: return null
        val mediaType = item.string("mediaType") ?: if (item.string("title") != null) "movie" else "tv"
        if (mediaType != "movie" && mediaType != "tv") return null
        val title = item.string("title") ?: item.string("name") ?: return null
        val date = item.string("releaseDate") ?: item.string("firstAirDate")
        val year = date?.take(4)?.takeIf { it.all(Char::isDigit) }
        val mediaStatus = item.obj("mediaInfo")?.int("status") ?: 1
        return RemoteDiscoverItem(
            id = "seerr-$mediaType-$remoteId", remoteId = remoteId, mediaType = mediaType, title = title,
            metadata = year.orEmpty(), artworkUrl = item.string("posterPath")?.let { safeTmdbArtwork(it) },
            inLibrary = mediaStatus == 5, requested = mediaStatus in 2..4, seerrStatus = mediaStatus,
            overview = item.string("overview"), facts = discoverFacts(item, mediaType, year),
            genres = objectNameArray(item, "genres"),
            backdropUrl = item.string("backdropPath")?.let { tmdbImage(it, "w1280") },
        )
    }

    /** Seerr's genre slider: a genre and a few backdrops from its titles. */
    fun genreSlider(payload: String, mediaType: String): List<app.reelstack.data.model.DiscoverGenre> =
        (runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray).orEmpty().mapNotNull { element ->
            val genre = element as? JsonObject ?: return@mapNotNull null
            app.reelstack.data.model.DiscoverGenre(
                id = genre.int("id") ?: return@mapNotNull null,
                name = genre.string("name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null,
                mediaType = mediaType,
                backdropUrl = genre.array("backdrops").firstOrNull()?.jsonPrimitive?.contentOrNull?.let { tmdbImage(it, "w780") },
            )
        }

    /** TMDB image of a chosen size, for a path Seerr gave; anything else is not an image we fetch. */
    internal fun tmdbImage(path: String, size: String): String? =
        if (path.startsWith("/")) "https://image.tmdb.org/t/p/$size$path" else null
```

I `mediaDetails`, cast-lista:

```kotlin
                app.reelstack.data.model.CastMember(name, person.string("character"),
                    person.string("profilePath")?.let { tmdbImage(it, "w185") }, seerrPersonId = person.int("id"))
```

Bruk importen `kotlinx.serialization.json.contentOrNull` om han ikkje finst.

- [ ] **Steg 5: Klienten**

`ServiceClients.kt:1616`: `private fun HttpResponse.requireSuccess` → `internal fun HttpResponse.requireSuccess`.

`SeerrDiscoverClient.kt`:

```kotlin
package app.reelstack.data.network

import app.reelstack.data.model.DiscoverGenre
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind

/** Seerr's discovery, person and request-option endpoints. One instance lives in the repository. */
class SeerrDiscoverClient(private val transport: JsonHttpTransport = HttpTransport()) {
    fun row(connection: ServiceConnection, kind: DiscoverRowKind, page: Int = 1): SeerrSearchPage =
        page(connection, "api/v1/${kind.path}?page=$page&language=nb", page)

    fun genrePage(connection: ServiceConnection, genre: DiscoverGenre, page: Int = 1): SeerrSearchPage {
        val type = if (genre.mediaType == "tv") "tv" else "movies"
        return page(connection, "api/v1/discover/$type/genre/${genre.id}?page=$page&language=nb", page)
    }

    fun genres(connection: ServiceConnection, mediaType: String): List<DiscoverGenre> {
        require(mediaType == "movie" || mediaType == "tv")
        return ServicePayloadParser.genreSlider(get(connection, "api/v1/discover/genreslider/$mediaType?language=nb"), mediaType)
    }

    fun similar(connection: ServiceConnection, mediaType: String, id: Int): List<RemoteDiscoverItem> =
        related(connection, mediaType, id, "similar")

    fun recommendations(connection: ServiceConnection, mediaType: String, id: Int): List<RemoteDiscoverItem> =
        related(connection, mediaType, id, "recommendations")

    private fun related(connection: ServiceConnection, mediaType: String, id: Int, kind: String): List<RemoteDiscoverItem> {
        require(mediaType == "movie" || mediaType == "tv")
        return ServicePayloadParser.discover(get(connection, "api/v1/$mediaType/$id/$kind?page=1&language=nb"))
    }

    private fun page(connection: ServiceConnection, path: String, page: Int): SeerrSearchPage {
        val body = get(connection, path)
        return SeerrSearchPage(ServicePayloadParser.discover(body), page, ServicePayloadParser.totalPages(body))
    }

    internal fun get(connection: ServiceConnection, path: String): String {
        require(connection.kind == ServiceKind.SEERR)
        val response = transport.get(EndpointValidator.resolve(connection.baseUrl, path), seerrHeaders(connection))
        response.requireSuccess(connection.kind)
        return response.body
    }
}

internal fun seerrHeaders(connection: ServiceConnection): Map<String, String> =
    if (connection.sessionCookie) seerrCookieHeaders(connection.token) else mapOf("X-Api-Key" to connection.token)
```

- [ ] **Steg 6: Repository**

`MediaSyncRepository` konstruktør, etter `seerrServiceClient`:

```kotlin
    private val seerrDiscoverClient: app.reelstack.data.network.SeerrDiscoverClient = app.reelstack.data.network.SeerrDiscoverClient(),
```

`discoverMedia`: legg til `backdropUrl = item.backdropUrl,`. Nye funksjonar under `search`:

```kotlin
    fun discoverRow(connection: ServiceConnection, kind: app.reelstack.data.model.DiscoverRowKind, page: Int = 1): SearchPage =
        seerrDiscoverClient.row(connection, kind, page).let { SearchPage(it.items.map(::discoverMedia), it.page, it.hasMore) }

    fun discoverGenres(connection: ServiceConnection): List<app.reelstack.data.model.DiscoverGenre> =
        seerrDiscoverClient.genres(connection, "movie") + seerrDiscoverClient.genres(connection, "tv")

    fun discoverGenrePage(connection: ServiceConnection, genre: app.reelstack.data.model.DiscoverGenre, page: Int = 1): SearchPage =
        seerrDiscoverClient.genrePage(connection, genre, page).let { SearchPage(it.items.map(::discoverMedia), it.page, it.hasMore) }

    fun similar(connection: ServiceConnection, media: DiscoverMedia): List<DiscoverMedia> =
        seerrDiscoverClient.similar(connection, requireNotNull(media.mediaType), requireNotNull(media.remoteId)).map(::discoverMedia)

    fun recommended(connection: ServiceConnection, media: DiscoverMedia): List<DiscoverMedia> =
        seerrDiscoverClient.recommendations(connection, requireNotNull(media.mediaType), requireNotNull(media.remoteId)).map(::discoverMedia)
```

- [ ] **Steg 7: Køyr testane**

Kommando G, mønster `app.reelstack.data.network.SeerrDiscoverClientTest`, så `app.reelstack.data.network.*`. Forventa: PASS.

- [ ] **Steg 8: Commit**

```bash
git add -A app/src && git commit -m "Read Seerr's discovery rows, genres and people" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Koordinator som lastar kvar rad for seg

**Filer:**
- Opprett: `app/src/main/java/app/reelstack/ui/DiscoverHubCoordinator.kt`
- Endre: `app/src/main/java/app/reelstack/ui/ReelstackViewModel.kt` (`ReelstackUiState` + `knownDiscoverMedia` 307–316, `mapDiscoverMedia` 319–324, koordinator ved 530, offentlege funksjonar)
- Endre: `app/src/main/java/app/reelstack/ui/state/DiscoverUiState.kt` (felt `hub`, `trackedRequests`)
- Test: `app/src/test/java/app/reelstack/ui/DiscoverHubLoaderTest.kt`

**Grensesnitt:**
- Konsumerer: `MediaSyncRepository.discoverRow/discoverGenres/discoverGenrePage`, `MediaSyncRepository.details(connection, media)` (cast) frå Task 3.
- Produserer: `interface DiscoverHubSource`, `class DiscoverHubLoader(source)`, `suspend fun DiscoverHubLoader.load(scope: String, currentScope: () -> String?, emit: ((DiscoverHubState) -> DiscoverHubState) -> Unit)`;
  `ReelstackUiState.discoverHub: DiscoverHubState`; `ReelstackViewModel.refreshDiscoverHub(force: Boolean = false)`,
  `.openDiscoverGrid(target: DiscoverGridTarget)`, `.loadMoreDiscoverGrid()`, `.closeDiscoverGrid()`.

- [ ] **Steg 1: Skriv dei feilande testane**

`DiscoverHubLoaderTest.kt`:

```kotlin
package app.reelstack.ui

import app.reelstack.data.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DiscoverHubLoaderTest {
    private fun media(id: Int) = DiscoverMedia("seerr-movie-$id", "T$id", "2026", 0, false, remoteId = id, mediaType = "movie")

    private class Source(val failing: Set<DiscoverRowKind> = emptySet()) : DiscoverHubSource {
        override suspend fun row(kind: DiscoverRowKind, page: Int) =
            if (kind in failing) error("down") else DiscoverRowPage(listOf(DiscoverMedia("seerr-movie-${kind.ordinal}", "x", "", 0, false,
                remoteId = kind.ordinal + 1, mediaType = "movie")), page, false)
        override suspend fun genres() = listOf(DiscoverGenre(27, "Grøssar", "movie", null))
        override suspend fun cast(media: DiscoverMedia) = listOf(CastMember("P${media.remoteId}", seerrPersonId = media.remoteId))
    }

    private suspend fun loadWith(source: DiscoverHubSource, current: () -> String = { "s" }): DiscoverHubState {
        var state = DiscoverHubState()
        DiscoverHubLoader(source).load("s", current) { change -> state = change(state) }
        return state
    }

    @Test fun aFailingRowDoesNotStopTheOthers() = runTest {
        val state = loadWith(Source(failing = setOf(DiscoverRowKind.UPCOMING_MOVIES)))
        assertTrue(state.rows.getValue(DiscoverRowKind.UPCOMING_MOVIES).failed)
        assertEquals(1, state.rows.getValue(DiscoverRowKind.TRENDING).items.size)
        assertEquals(1, state.rows.getValue(DiscoverRowKind.UPCOMING_SERIES).items.size)
        assertEquals(listOf("Grøssar"), state.genres.map { it.name })
        assertEquals(listOf(1), state.people.map { it.id })
    }

    @Test fun lateRowsFromAnotherScopeAreDropped() = runTest {
        val state = loadWith(Source(), current = { "another profile" })
        assertTrue(state.rows.values.all { it.items.isEmpty() })
    }

    @Test fun rowsLoadWithAnApiKey() = runTest {
        // The loader never asks who the user is: an API-key connection gets the same rows.
        assertEquals(3, loadWith(Source()).rows.size)
    }
}
```

- [ ] **Steg 2: Køyr og sjå at dei feilar**

Kommando G, mønster `app.reelstack.ui.DiscoverHubLoaderTest`. Forventa: FAIL (kompilering).

- [ ] **Steg 3: Implementer koordinatoren**

`DiscoverHubCoordinator.kt`:

```kotlin
package app.reelstack.ui

import app.reelstack.AppContainer
import app.reelstack.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiscoverRowPage(val items: List<DiscoverMedia>, val page: Int, val hasMore: Boolean)

/** Everything the Discover rows need from Seerr, so the loading order can be tested without a server. */
interface DiscoverHubSource {
    suspend fun row(kind: DiscoverRowKind, page: Int): DiscoverRowPage
    suspend fun genres(): List<DiscoverGenre>
    suspend fun cast(media: DiscoverMedia): List<CastMember>
}

/** Starts every row at once and lets each land when it is ready. */
class DiscoverHubLoader(private val source: DiscoverHubSource) {
    suspend fun load(scope: String, currentScope: () -> String?, emit: ((DiscoverHubState) -> DiscoverHubState) -> Unit) = coroutineScope {
        fun land(change: (DiscoverHubState) -> DiscoverHubState) { if (currentScope() == scope) emit(change) }
        land { it.copy(scope = scope, rows = DiscoverRowKind.entries.associateWith { kind -> (it.rows[kind] ?: DiscoverRowState()).copy(loading = true, failed = false) }) }
        val rows = DiscoverRowKind.entries.map { kind ->
            async {
                val result = runCatching { source.row(kind, 1) }
                land { state -> state.copy(rows = state.rows + (kind to result.fold(
                    onSuccess = { DiscoverRowState(it.items, page = it.page, hasMore = it.hasMore) },
                    onFailure = { DiscoverRowState(failed = true) }))) }
                if (kind == DiscoverRowKind.TRENDING) result.getOrNull()?.items.orEmpty() else emptyList()
            }
        }
        val genres = async { runCatching { source.genres() }.getOrNull()?.let { list -> land { it.copy(genres = list) } } }
        val trending = rows.first().await()
        val casts = trending.take(6).map { media -> async { runCatching { source.cast(media) }.getOrDefault(emptyList()) } }.awaitAll()
        land { it.copy(people = peopleFrom(casts)) }
        rows.awaitAll(); genres.await()
    }
}

/** Owns the Discover rows in the root state. */
internal class DiscoverHubCoordinator(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val readState: () -> ReelstackUiState,
    private val updateState: ((ReelstackUiState) -> ReelstackUiState) -> Unit,
) {
    private var job: Job? = null
    private var gridJob: Job? = null

    private fun seerr() = readState().connections.firstOrNull { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank() }

    private fun scopeKey(state: ReelstackUiState = readState()) =
        "${state.activeProfileId}:${seerr()?.baseUrl}:${seerr()?.userId}"

    fun refresh(force: Boolean) {
        val connection = seerr() ?: return
        if (readState().isKidMode) return
        val key = scopeKey()
        if (!force && readState().discoverHub.scope == key && readState().discoverHub.rows.values.none { it.failed }) return
        job?.cancel()
        val repository = container.mediaSyncRepository
        val source = object : DiscoverHubSource {
            override suspend fun row(kind: DiscoverRowKind, page: Int) = withContext(Dispatchers.IO) {
                repository.discoverRow(connection, kind, page).let { DiscoverRowPage(it.items, it.page, it.hasMore) } }
            override suspend fun genres() = withContext(Dispatchers.IO) { repository.discoverGenres(connection) }
            override suspend fun cast(media: DiscoverMedia) = withContext(Dispatchers.IO) { repository.details(connection, media).cast }
        }
        job = scope.launch {
            DiscoverHubLoader(source).load(key, { scopeKey() }) { change ->
                updateState { state -> state.copy(discoverHub = change(state.discoverHub)) }
            }
        }
    }

    fun openGrid(target: DiscoverGridTarget) {
        updateState { it.copy(discoverHub = it.discoverHub.copy(grid = target, gridRow = DiscoverRowState(loading = true))) }
        loadGrid(target, 1)
    }

    fun loadMore() {
        val hub = readState().discoverHub
        val target = hub.grid ?: return
        if (hub.gridRow.loading || !hub.gridRow.hasMore) return
        updateState { it.copy(discoverHub = it.discoverHub.copy(gridRow = it.discoverHub.gridRow.copy(loading = true))) }
        loadGrid(target, hub.gridRow.page + 1)
    }

    fun closeGrid() = updateState { it.copy(discoverHub = it.discoverHub.copy(grid = null, gridRow = DiscoverRowState())) }

    private fun loadGrid(target: DiscoverGridTarget, page: Int) {
        val connection = seerr() ?: return
        gridJob?.cancel()
        gridJob = scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { when (target) {
                is DiscoverGridTarget.Row -> container.mediaSyncRepository.discoverRow(connection, target.kind, page)
                is DiscoverGridTarget.Genre -> container.mediaSyncRepository.discoverGenrePage(connection, target.genre, page)
            } } }
            updateState { state ->
                if (state.discoverHub.grid != target) return@updateState state
                val old = state.discoverHub.gridRow
                state.copy(discoverHub = state.discoverHub.copy(gridRow = result.fold(
                    onSuccess = { DiscoverRowState((if (page == 1) emptyList() else old.items) + it.items, page = it.page, hasMore = it.hasMore) },
                    onFailure = { old.copy(loading = false, failed = true) })))
            }
        }
    }
}
```

- [ ] **Steg 4: Kople til view-modellen og tilstanden**

`ReelstackUiState`: under `detailMedia` legg til `val discoverHub: app.reelstack.data.model.DiscoverHubState = app.reelstack.data.model.DiscoverHubState(),`.
`knownDiscoverMedia`: start med `listOfNotNull(detailMedia) + discoverHub.allMedia + discover + ...`.
`mapDiscoverMedia`: legg til

```kotlin
        discoverHub = discoverHub.copy(
            rows = discoverHub.rows.mapValues { (_, row) -> row.copy(items = row.items.map(transform)) },
            gridRow = discoverHub.gridRow.copy(items = discoverHub.gridRow.items.map(transform)),
        ),
```

I `ReelstackViewModel`, etter `globalSearch`-koordinatoren:

```kotlin
    private val discoverHub = DiscoverHubCoordinator(
        container = container,
        scope = operationScope,
        readState = { _uiState.value },
        updateState = { transform -> _uiState.update(transform) },
    )

    fun refreshDiscoverHub(force: Boolean = false) = discoverHub.refresh(force)
    fun openDiscoverGrid(target: app.reelstack.data.model.DiscoverGridTarget) = discoverHub.openGrid(target)
    fun loadMoreDiscoverGrid() = discoverHub.loadMore()
    fun closeDiscoverGrid() = discoverHub.closeGrid()
```

I `selectTab`, etter `if (tab == AppTab.LIBRARY) browseLibrary(false)`:

```kotlin
        if (tab == AppTab.DISCOVER) discoverHub.refresh(force = false)
```

Ved utlogging og profilbyte, der `discover = emptyList()` blir sett (linje ~1616 og ~3775), legg til
`discoverHub = app.reelstack.data.model.DiscoverHubState(),` i same `copy`.

`DiscoverUiState`: legg til `val hub: app.reelstack.data.model.DiscoverHubState = app.reelstack.data.model.DiscoverHubState(),`
og `val trackedRequests: List<app.reelstack.data.model.TrackedRequest> = emptyList(),`, og i
`toDiscoverUiState()` `hub = discoverHub, trackedRequests = trackedRequests,`. Gjer det same i
`discoverUiState`-mappinga i `ReelstackViewModel` (linje ~477–483) om ho ikkje brukar `toDiscoverUiState()`.

- [ ] **Steg 5: Køyr testane**

Kommando G, mønster `app.reelstack.ui.DiscoverHubLoaderTest`, så heile `testDebugUnitTest`. Forventa: PASS.

- [ ] **Steg 6: Commit**

```bash
git add -A app/src && git commit -m "Load each Discover row on its own" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Oppdag-sida (telefon og TV)

**Filer:**
- Opprett: `app/src/main/java/app/reelstack/ui/screens/DiscoverHub.kt`
- Endre: `app/src/main/java/app/reelstack/ui/screens/SecondaryScreens.kt` (`DiscoverScreen` 245–532, `DiscoverCard` 658–743)
- Endre: `app/src/main/java/app/reelstack/ui/ReelstackApp.kt` (DISCOVER-kallet)
- Endre: dei tre `discover_hub.xml`
- Test: `app/src/test/java/app/reelstack/ui/screens/DiscoverHubScreenTest.kt`

**Grensesnitt:**
- Konsumerer: `DiscoverUiState.hub`, `.trackedRequests` (Task 4), `RequestLine`, `requestLineStatus` (Task 2), `myRequestsPreview` (Task 2).
- Produserer: `@Composable fun DiscoverHubContent(state: DiscoverUiState, contentPadding: PaddingValues, actions: DiscoverHubActions)`;
  `data class DiscoverHubActions(val onTitle: (String) -> Unit, val onPerson: (Int) -> Unit, val onMyRequests: () -> Unit, val onRequestLine: (String) -> Unit, val onGrid: (DiscoverGridTarget) -> Unit, val onRetry: () -> Unit, val onSearch: () -> Unit)`;
  `@Composable internal fun PosterOnlyCard(media: DiscoverMedia, onClick: () -> Unit, modifier: Modifier = Modifier, onFocused: () -> Unit = {})`.

- [ ] **Steg 1: Strengar**

I `values-b+nn/discover_hub.xml` (bokmål og engelsk tilsvarande: «Populært nå»/«Popular now», «Filmer som kommer»/«Upcoming films»,
«Serier som kommer»/«Upcoming series», «Folk»/«People», «Sjangere»/«Genres», «Alle %1$d»/«All %1$d», «Alle»/«All»,
«Seerr svarte ikke på alt.»/«Seerr didn't answer everything.», «Søk etter filmer, serier og folk»/«Search films, series and people»):

```xml
    <string name="discover_row_trending">Populært no</string>
    <string name="discover_row_upcoming_movies">Filmar som kjem</string>
    <string name="discover_row_upcoming_series">Seriar som kjem</string>
    <string name="discover_people">Folk</string>
    <string name="discover_genres">Sjangrar</string>
    <string name="discover_all_count">Alle %1$d</string>
    <string name="discover_all">Alle</string>
    <string name="discover_rows_failed">Seerr svarte ikkje på alt.</string>
    <string name="discover_search_hint">Søk etter filmar, seriar og folk</string>
```

- [ ] **Steg 2: Skriv den feilande UI-testen**

`DiscoverHubScreenTest.kt` (same oppsett som `MyRequestsScreenTest`, `qualifiers = "w412dp-h900dp-xhdpi"`):

```kotlin
    private fun media(id: Int) = DiscoverMedia("seerr-movie-$id", "Tittel $id", "2026", R.drawable.media_placeholder, false,
        remoteId = id, mediaType = "movie", artworkUrl = "https://image.tmdb.org/t/p/w500/$id.jpg")

    private fun hubState(failed: Boolean = false) = DiscoverUiState(
        connections = listOf(ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "c", userId = "7", sessionCookie = true)),
        hub = DiscoverHubState(rows = mapOf(
            DiscoverRowKind.TRENDING to DiscoverRowState(listOf(media(1), media(2))),
            DiscoverRowKind.UPCOMING_MOVIES to DiscoverRowState(failed = failed),
            DiscoverRowKind.UPCOMING_SERIES to DiscoverRowState(listOf(media(3))),
        ), people = listOf(DiscoverPerson(9, "Margaret Qualley", null))),
        trackedRequests = listOf(TrackedRequest("r", 4, "movie", "Coraline", "https://image.tmdb.org/t/p/w500/r.jpg", emptySet())),
    )

    @Test fun postersCarryNoTextOrButtons() {
        rule.setContent { ReelstackTheme { DiscoverHubContent(hubState(), PaddingValues(), actions()) } }
        rule.onNodeWithTag("discover-poster-seerr-movie-1").assertIsDisplayed()
        rule.onAllNodes(hasAnyAncestor(hasTestTag("discover-poster-seerr-movie-1")) and hasText("Tittel 1")).assertCountEquals(0)
        rule.onAllNodes(hasAnyAncestor(hasTestTag("discover-poster-seerr-movie-1")) and hasClickAction()).assertCountEquals(0)
    }

    @Test fun aFailedRowIsGoneAndSaysSoOnce() {
        rule.setContent { ReelstackTheme { DiscoverHubContent(hubState(failed = true), PaddingValues(), actions()) } }
        rule.onNodeWithTag("discover-row-UPCOMING_MOVIES").assertDoesNotExist()
        rule.onNodeWithTag("discover-rows-retry").performScrollTo().assertIsDisplayed()
    }

    @Test fun yourRequestsComeFirstAsLines() {
        val opened = mutableListOf<String>()
        rule.setContent { ReelstackTheme { DiscoverHubContent(hubState(), PaddingValues(), actions(onLine = { opened += it })) } }
        rule.onNodeWithTag("my-request-r").performClick()
        assertEquals(listOf("r"), opened)
    }

    private fun actions(onLine: (String) -> Unit = {}) = DiscoverHubActions({}, {}, {}, onLine, {}, {}, {})
```

- [ ] **Steg 3: Køyr og sjå at han feilar**

Kommando G, mønster `app.reelstack.ui.screens.DiscoverHubScreenTest`. Forventa: FAIL.

- [ ] **Steg 4: Implementer `DiscoverHub.kt`**

```kotlin
package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.focusScale
import app.reelstack.ui.components.isTelevision
import app.reelstack.ui.components.mediaCardIndication
import app.reelstack.ui.state.DiscoverUiState
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor

data class DiscoverHubActions(
    val onTitle: (String) -> Unit,
    val onPerson: (Int) -> Unit,
    val onMyRequests: () -> Unit,
    val onRequestLine: (String) -> Unit,
    val onGrid: (DiscoverGridTarget) -> Unit,
    val onRetry: () -> Unit,
    val onSearch: () -> Unit,
)

@Composable
private fun DiscoverRowKind.title(): String = stringResource(when (this) {
    DiscoverRowKind.TRENDING -> R.string.discover_row_trending
    DiscoverRowKind.UPCOMING_MOVIES -> R.string.discover_row_upcoming_movies
    DiscoverRowKind.UPCOMING_SERIES -> R.string.discover_row_upcoming_series
})

/** The rows of Oppdag. On a television the title in focus owns the top of the page. */
@Composable
fun DiscoverHubContent(state: DiscoverUiState, contentPadding: PaddingValues, actions: DiscoverHubActions,
    header: @Composable () -> Unit = {}) {
    val tv = isTelevision()
    var focused by remember { mutableStateOf<DiscoverMedia?>(null) }
    val first = state.hub.rows[DiscoverRowKind.TRENDING]?.items?.firstOrNull()
    val shown = focused ?: first
    val mine = myRequestsPreview(state.trackedRequests, if (tv) 12 else 3)
    val posterWidth = (if (tv) 120.dp else 112.dp) * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
    Box(Modifier.fillMaxSize().testTag("discover-hub")) {
        if (tv && shown != null) FocusBackdrop(shown)
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (tv) 22.dp else 24.dp),
            contentPadding = PaddingValues(top = if (tv) 0.dp else ReelLayout.PageTop,
                bottom = contentPadding.calculateBottomPadding() + 24.dp)) {
            item(key = "top") {
                Column(Modifier.padding(horizontal = ReelLayout.Gutter)) {
                    header()
                    if (tv && shown != null) FocusHeader(shown)
                }
            }
            if (mine.isNotEmpty()) item(key = "mine") {
                Section(stringResource(R.string.my_requests_title),
                    trailing = { RowEndLink(stringResource(R.string.discover_all_count, state.trackedRequests.size),
                        "discover-my-requests-all", actions.onMyRequests) }) {
                    if (tv) LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        items(mine, key = { "mine-${it.key}" }) { request ->
                            RequestPoster(request, posterWidth) { actions.onRequestLine(request.key) }
                        }
                    } else Column(Modifier.padding(horizontal = ReelLayout.Gutter)) {
                        mine.forEach { RequestLine(it, { actions.onRequestLine(it.key) }) }
                    }
                }
            }
            DiscoverRowKind.entries.forEach { kind ->
                val row = state.hub.rows[kind] ?: return@forEach
                if (row.failed || (!row.loading && row.items.isEmpty())) return@forEach
                item(key = "row-$kind") {
                    Section(kind.title(), Modifier.testTag("discover-row-$kind")) {
                        LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter),
                            horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 10.dp)) {
                            if (row.loading && row.items.isEmpty()) items(6) {
                                Box(Modifier.width(posterWidth).aspectRatio(2f / 3f).clip(RoundedCornerShape(ReelLayout.ArtworkCorner)).background(SurfaceRaised))
                            }
                            items(row.items, key = { "$kind-${it.id}" }) { media ->
                                PosterOnlyCard(media, { actions.onTitle(media.id) }, Modifier.width(posterWidth), onFocused = { focused = media })
                            }
                            if (row.items.isNotEmpty()) item(key = "$kind-all") {
                                RowEndTile(posterWidth) { actions.onGrid(DiscoverGridTarget.Row(kind)) }
                            }
                        }
                    }
                }
            }
            if (state.hub.people.isNotEmpty()) item(key = "people") {
                Section(stringResource(R.string.discover_people), Modifier.testTag("discover-people")) {
                    LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(state.hub.people, key = { "person-${it.id}" }) { person -> PersonPortrait(person) { actions.onPerson(person.id) } }
                    }
                }
            }
            if (state.hub.genres.isNotEmpty()) item(key = "genres") {
                Section(stringResource(R.string.discover_genres)) {
                    LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.hub.genres, key = { "genre-${it.mediaType}-${it.id}" }) { genre ->
                            GenreCard(genre) { actions.onGrid(DiscoverGridTarget.Genre(genre)) }
                        }
                    }
                }
            }
            if (state.hub.anyFailed) item(key = "failed") {
                Row(Modifier.padding(horizontal = ReelLayout.Gutter), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.discover_rows_failed), color = Muted, style = MaterialTheme.typography.bodySmall)
                    RowEndLink(stringResource(R.string.action_retry), "discover-rows-retry", actions.onRetry)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = ReelLayout.Gutter), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            trailing()
        }
        content()
    }
}

/** A poster and nothing else: title, year and status live in the header or on the title page. */
@Composable
internal fun PosterOnlyCard(media: DiscoverMedia, onClick: () -> Unit, modifier: Modifier = Modifier, onFocused: () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    val focusedNow by interaction.collectIsFocusedAsState()
    LaunchedEffect(focusedNow) { if (focusedNow) onFocused() }
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    val scale = focusScale(focusedNow, false)
    Box(modifier.aspectRatio(2f / 3f).graphicsLayerScale(scale).focusOutline(interaction, shape).clip(shape)
        .background(SurfaceRaised)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = media.title }
        .testTag("discover-poster-${media.id}")) {
        MediaArtwork(media.artworkUrl, null, Modifier.matchParentSize(), fallbackRes = media.artworkRes, contentScale = ContentScale.Crop,
            source = ServiceKind.SEERR)
    }
}

private fun Modifier.graphicsLayerScale(scale: Float) = this.then(androidx.compose.ui.Modifier.graphicsLayer { scaleX = scale; scaleY = scale })

/** On TV your requests are posters too; work in flight carries a thin line along the bottom. */
@Composable
private fun RequestPoster(request: TrackedRequest, width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    Box(Modifier.width(width).aspectRatio(2f / 3f).focusOutline(interaction, shape).clip(shape).background(SurfaceRaised)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = request.title }.testTag("my-request-${request.key}")) {
        MediaArtwork(request.artworkUrl, null, Modifier.matchParentSize(), fallbackRes = R.drawable.media_placeholder,
            contentScale = ContentScale.Crop, source = ServiceKind.SEERR)
        if (request.stage == RequestStage.DOWNLOADING && request.percent != null) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = .5f))) {
                Box(Modifier.fillMaxWidth(request.percent / 100f).height(3.dp).background(TextColor))
            }
        }
    }
}

@Composable
private fun RowEndTile(width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.width(width).aspectRatio(2f / 3f).focusOutline(interaction, RoundedCornerShape(ReelLayout.ArtworkCorner))
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.discover_all), color = Muted)
            Icon(SpoleIcons.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RowEndLink(text: String, tag: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(Modifier.heightIn(min = 44.dp).focusOutline(interaction, RoundedCornerShape(10.dp))
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .padding(horizontal = 6.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Muted, style = MaterialTheme.typography.labelLarge)
        Icon(SpoleIcons.ChevronRight, null, tint = Muted, modifier = Modifier.size(16.dp))
    }
}

/** A face and a name: the only text under a picture on this page. */
@Composable
internal fun PersonPortrait(person: DiscoverPerson, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val size = if (isTelevision()) 96.dp else 72.dp
    Column(Modifier.width(size + 8.dp).focusOutline(interaction, RoundedCornerShape(12.dp))
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .testTag("person-${person.id}"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size).clip(CircleShape).background(SurfaceRaised), contentAlignment = Alignment.Center) {
            if (person.portraitUrl != null) MediaArtwork(person.portraitUrl, null, Modifier.matchParentSize(),
                fallbackRes = R.drawable.media_placeholder, contentScale = ContentScale.Crop, source = ServiceKind.SEERR)
            else Text(person.name.split(' ').filter(String::isNotBlank).take(2).map { it.first() }.joinToString(""), color = Muted)
        }
        Text(person.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun GenreCard(genre: DiscoverGenre, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.width(168.dp).aspectRatio(16f / 9f).focusOutline(interaction, shape).clip(shape).background(SurfaceRaised)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .testTag("genre-${genre.mediaType}-${genre.id}")) {
        genre.backdropUrl?.let { MediaArtwork(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, source = ServiceKind.SEERR) }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(.3f to Color.Transparent, 1f to Color.Black.copy(alpha = .7f))))
        Text(genre.name, style = MaterialTheme.typography.titleSmall, color = TextColor,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
    }
}

@Composable
private fun FocusBackdrop(media: DiscoverMedia) {
    Box(Modifier.fillMaxWidth().fillMaxHeight(.62f)) {
        MediaArtwork(media.backdropUrl ?: media.artworkUrl, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop,
            source = ServiceKind.SEERR)
        val ground = MaterialTheme.colorScheme.background
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(0f to ground, .45f to ground.copy(alpha = .6f), .8f to Color.Transparent)))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(.4f to Color.Transparent, 1f to ground)))
    }
}

/** Title, facts, status and two lines of synopsis for whatever has focus. */
@Composable
private fun FocusHeader(media: DiscoverMedia) {
    Column(Modifier.fillMaxWidth(.55f).padding(top = 48.dp, bottom = 8.dp).testTag("discover-focus-header"),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(media.title, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text((listOf(media.metadata) + media.facts).filter(String::isNotBlank).distinct().joinToString("  ·  "),
            color = Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(app.reelstack.localization.localizedSeerrStatus(media.seerrStatus, media.inLibrary, media.requested),
            style = MaterialTheme.typography.bodyLarge)
        media.overview?.takeIf(String::isNotBlank)?.let {
            Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
```

Bruk `import androidx.compose.ui.graphics.graphicsLayer` og fjern `graphicsLayerScale`-omvegen om
importen er der (bruk `.graphicsLayer { scaleX = scale; scaleY = scale }` direkte).

- [ ] **Steg 5: Rutenett for «Alle»**

I same fil:

```kotlin
/** Everything in one row or one genre, a page at a time, as posters. */
@Composable
fun DiscoverGridContent(title: String, row: DiscoverRowState, contentPadding: PaddingValues, onTitle: (String) -> Unit,
    onMore: () -> Unit, onBack: () -> Unit) {
    val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(posterCell()), state = grid,
        contentPadding = PaddingValues(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().app.reelstack.ui.components.steadyRemoteRows(grid).testTag("discover-grid-all")) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = onBack) { Icon(SpoleIcons.ArrowBack, stringResource(R.string.action_back)) }
                Text(title, style = MaterialTheme.typography.headlineSmall)
            }
        }
        items(row.items.size, key = { row.items[it].id }) { index ->
            val media = row.items[index]
            if (index >= row.items.size - 6) LaunchedEffect(row.items.size) { onMore() }
            PosterOnlyCard(media, { onTitle(media.id) })
        }
    }
}
```

(`steadyRemoteRows` blir importert med `import app.reelstack.ui.components.steadyRemoteRows` og brukt som
`Modifier.fillMaxSize().steadyRemoteRows(grid)`.)

- [ ] **Steg 6: Bruk det i `DiscoverScreen`**

I `DiscoverScreen` (SecondaryScreens.kt), byt innhaldet i `ReelPage(media = true) { ... }` med:

```kotlin
    ReelPage(media = true) {
        val grid = state.hub.grid
        when {
            grid != null -> DiscoverGridContent(
                title = when (grid) { is DiscoverGridTarget.Row -> rowTitle(grid.kind); is DiscoverGridTarget.Genre -> grid.genre.name },
                row = state.hub.gridRow, contentPadding = contentPadding, onTitle = onDetails, onMore = onGridMore, onBack = onGridBack)
            state.searchQuery.isBlank() -> DiscoverHubContent(state, contentPadding, hubActions, header = { discoverTop() })
            else -> DiscoverSearchGrid(...)  // den eksisterande LazyVerticalGrid-blokka, uendra bortsett frå DiscoverCard → PosterOnlyCard
        }
    }
```

Konkret: flytt den eksisterande `LazyVerticalGrid(...)`-blokka til ein privat
`@Composable private fun DiscoverSearchGrid(...)` med dei same parametrane ho les; flytt
søkjefelt- og kontohovudet (`DiscoverHeader(...)`) til ein lokal `val discoverTop: @Composable () -> Unit`
som blir brukt både av hub-en og søkjegridet; ta bort `DiscoverFilterBar`-rada; og i søkjegridet byt

```kotlin
                DiscoverCard(media, media.id in state.requestingMediaIds,
                    allowed = ..., onRequest = { onRequest(media.id) }, onDetails = { onDetails(media.id) })
```

med

```kotlin
                PosterOnlyCard(media, { onDetails(media.id) }, Modifier.fillMaxWidth())
```

Nye parametrar på `DiscoverScreen`: `hubActions: DiscoverHubActions`, `onGridMore: () -> Unit = {}`,
`onGridBack: () -> Unit = {}`. Fjern dei daude parametrane `prepareSearch`, `globalSearch`,
`onSearchFocusConsumed` og blokkene som les dei (søkjehistorikk-chipsa og fokus-effektane); behald
`searchTransitionModifier`. Legg til i SecondaryScreens.kt:

```kotlin
@Composable
private fun rowTitle(kind: DiscoverRowKind): String = stringResource(when (kind) {
    DiscoverRowKind.TRENDING -> R.string.discover_row_trending
    DiscoverRowKind.UPCOMING_MOVIES -> R.string.discover_row_upcoming_movies
    DiscoverRowKind.UPCOMING_SERIES -> R.string.discover_row_upcoming_series
})
```

`ReelstackApp.kt`, DISCOVER-kallet:

```kotlin
                    else DiscoverScreen(
                        state = discoverState,
                        contentPadding = screenInsets,
                        onSearch = viewModel::setSearchQuery,
                        onRequest = viewModel::requestMedia,
                        onDetails = viewModel::openDiscoverDetails,
                        onAccountClick = viewModel::openSeerrAccount,
                        onLibraryDetails = viewModel::openLibraryDetails,
                        onLoadMore = viewModel::loadMoreSearchResults,
                        searchTransitionModifier = searchTransition,
                        hubActions = app.reelstack.ui.screens.DiscoverHubActions(
                            onTitle = viewModel::openDiscoverDetails, onPerson = viewModel::openPerson,
                            onMyRequests = viewModel::openMyRequests, onRequestLine = viewModel::openActivityDetails,
                            onGrid = viewModel::openDiscoverGrid, onRetry = { viewModel.refreshDiscoverHub(force = true) },
                            onSearch = viewModel::openGlobalSearch),
                        onGridMore = viewModel::loadMoreDiscoverGrid,
                        onGridBack = viewModel::closeDiscoverGrid,
                    )
```

`viewModel::openPerson` kjem i Task 7; til då: legg til ein tom
`fun openPerson(id: Int) = Unit` i view-modellen i dette steget, som Task 7 byter ut. Bruk
`grep -n "fun openGlobalSearch" app/src/main/java/app/reelstack/ui/ReelstackViewModel.kt` for å finne
namnet på funksjonen som opnar det globale søket, og bruk det namnet.

`BackHandler` for rutenettet i ReelstackApp.kt, ved dei andre:

```kotlin
    BackHandler(enabled = state.discoverHub.grid != null && state.selectedTab == AppTab.DISCOVER && state.activeSheet == null && !state.globalSearchOpen) {
        viewModel.closeDiscoverGrid()
    }
```

Slett `DiscoverCard`, `DiscoverFilterBar`, `DiscoverFilter`, `LibraryFilter` og `localizedLabel` deira
når `grep -rn "DiscoverCard(\|DiscoverFilterBar(" app/src/main` er tom etter Task 6 (det globale søket
brukar `DiscoverCard` til Task 6 byter det).

- [ ] **Steg 7: Køyr testane**

Kommando G, mønster `app.reelstack.ui.screens.DiscoverHubScreenTest`, så heile `testDebugUnitTest`.
Forventa: PASS. Eldre testar som brukar `discover-filters`, `discover-status-filter` eller
`discover-cover-*` skal skrivast om til `discover-poster-*` eller fjernast saman med filteret.

- [ ] **Steg 8: Commit**

```bash
git add -A app/src && git commit -m "Draw Discover as quiet rows of posters" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Søk med folk

**Filer:**
- Endre: `ServicePayloadParser.kt` (ny `searchPeople`), `ServiceClients.kt` (`SeerrSearchPage` + `search`)
- Endre: `MediaSyncRepository.kt` (`SearchPage`, `search`)
- Endre: `ui/state/GlobalSearchUiState.kt` (`SearchSlice.people`), `ui/DiscoverSearchCoordinator.kt`
- Endre: `ReelstackViewModel.kt` (`searchPeople`-felt), `DiscoverUiState.kt`
- Endre: `SecondaryScreens.kt` (`DiscoverSearchGrid`), `ui/screens/GlobalSearchScreen.kt`
- Test: `SeerrDiscoverClientTest.kt` (ny test)

**Grensesnitt:**
- Produserer: `ServicePayloadParser.searchPeople(payload): List<DiscoverPerson>`, `SeerrSearchPage.people`,
  `MediaSyncRepository.SearchPage.people`, `SearchSlice.people`, `ReelstackUiState.searchPeople`,
  `DiscoverUiState.searchPeople`, `@Composable internal fun PersonLine(person: DiscoverPerson, onClick: () -> Unit)`.

- [ ] **Steg 1: Feilande test**

I `SeerrDiscoverClientTest`:

```kotlin
    @Test fun searchKeepsPeopleWithTheirPicture() {
        val people = ServicePayloadParser.searchPeople("""{"results":[
            {"id":501,"mediaType":"person","name":"Margaret Qualley","profilePath":"/m.jpg","knownForDepartment":"Acting"},
            {"id":11,"mediaType":"movie","title":"X"}]}""")
        assertEquals(listOf(501), people.map { it.id })
        assertEquals("https://image.tmdb.org/t/p/w185/m.jpg", people.single().portraitUrl)
        assertEquals("Acting", people.single().role)
    }
```

- [ ] **Steg 2: Køyr og sjå at han feilar.** Kommando G, mønster `app.reelstack.data.network.SeerrDiscoverClientTest`. Forventa: FAIL.

- [ ] **Steg 3: Implementer**

Parser:

```kotlin
    fun searchPeople(payload: String): List<app.reelstack.data.model.DiscoverPerson> =
        json.parseToJsonElement(payload).jsonObject.array("results").mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            if (item.string("mediaType") != "person") return@mapNotNull null
            app.reelstack.data.model.DiscoverPerson(
                id = item.int("id") ?: return@mapNotNull null,
                name = item.string("name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null,
                portraitUrl = item.string("profilePath")?.let { tmdbImage(it, "w185") },
                role = item.string("knownForDepartment"),
            )
        }
```

`SeerrSearchPage`: legg til `val people: List<app.reelstack.data.model.DiscoverPerson> = emptyList(),`; i
`SeerrServiceClient.search`: `people = if (page == 1) ServicePayloadParser.searchPeople(response.body) else emptyList(),`.
`MediaSyncRepository.SearchPage`: `data class SearchPage(val items: List<DiscoverMedia>, val page: Int, val hasMore: Boolean, val people: List<app.reelstack.data.model.DiscoverPerson> = emptyList())`
og `search` sender `result.people` vidare. `SearchSlice`: `val people: List<app.reelstack.data.model.DiscoverPerson> = emptyList(),`;
`DiscoverSearchCoordinator.setQuery` set `people = discoverResult.getOrNull()?.people.orEmpty(),`;
`discoverSlice`/`withDiscoverSlice` les og skriv `state.searchPeople`; legg `val searchPeople: List<app.reelstack.data.model.DiscoverPerson> = emptyList(),`
til `ReelstackUiState` og `DiscoverUiState` (og `toDiscoverUiState`).

Linja (i `DiscoverHub.kt`):

```kotlin
/** A person in search results: picture, name, what they are known for. */
@Composable
internal fun PersonLine(person: DiscoverPerson, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().clickable(interactionSource = interaction, indication = mediaCardIndication(),
            role = Role.Button, onClick = onClick).focusOutline(interaction, RoundedCornerShape(10.dp))
        .testTag("search-person-${person.id}")) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(SurfaceRaised)) {
                person.portraitUrl?.let { MediaArtwork(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, source = ServiceKind.SEERR) }
            }
            Column(Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.titleSmall)
                person.role?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
            }
            Icon(SpoleIcons.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
        androidx.compose.material3.HorizontalDivider(color = TextColor.copy(alpha = .08f))
    }
}
```

I `DiscoverSearchGrid` og `GlobalSearchScreen`, før titlane:

```kotlin
        if (state.searchPeople.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.discover_people), color = Muted, style = MaterialTheme.typography.labelLarge)
            }
            items(state.searchPeople, key = { "search-person-${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { person ->
                PersonLine(person) { onPerson(person.id) }
            }
        }
```

med ny parameter `onPerson: (Int) -> Unit` på begge, kopla til `viewModel::openPerson`.
`GlobalSearchScreen` byter òg `DiscoverCard(...)` mot `PosterOnlyCard(media, { onDetails(media.id) }, Modifier.fillMaxWidth())`.
Slett så `DiscoverCard` og filtera (sjå Task 5 steg 6).

- [ ] **Steg 4: Køyr testane.** Kommando G, mønster `app.reelstack.data.network.*` og `app.reelstack.ui.screens.*`. Forventa: PASS.

- [ ] **Steg 5: Commit**

```bash
git add -A app/src && git commit -m "Find people with their pictures in search" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Personsida

**Filer:**
- Endre: `SeerrDiscoverClient.kt` (`person`, `personCredits`), `ServicePayloadParser.kt` (`person`, `personCredits`)
- Opprett: `app/src/main/java/app/reelstack/data/model/PersonPage.kt`
- Opprett: `app/src/main/java/app/reelstack/ui/PersonSheet.kt`
- Endre: `ReelstackViewModel.kt` (`AppSheet.Person`, `personPage`, `openPerson`), `ui/ReelstackSheets.kt` (ruting), `knownDiscoverMedia`
- Endre: `MediaSyncRepository.kt` (`person`)
- Test: `SeerrDiscoverClientTest.kt`, `app/src/test/java/app/reelstack/data/model/PersonPageTest.kt`

**Grensesnitt:**
- Produserer: `data class PersonPage(val person: DiscoverPerson, val department: String?, val birthday: String?, val biography: String?, val inLibrary: List<DiscoverMedia>, val more: List<DiscoverMedia>, val loading: Boolean = false, val failed: Boolean = false)`;
  `fun splitCredits(credits: List<DiscoverMedia>): Pair<List<DiscoverMedia>, List<DiscoverMedia>>`;
  `AppSheet.Person(val id: Int)`; `ReelstackUiState.personPage: PersonPage?`; `ReelstackViewModel.openPerson(id: Int)`.

- [ ] **Steg 1: Feilande testar**

`PersonPageTest.kt`:

```kotlin
package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonPageTest {
    private fun media(id: Int, status: Int?, year: String) = DiscoverMedia("seerr-movie-$id", "T$id", year, 0,
        inLibrary = status == 5, remoteId = id, mediaType = "movie", seerrStatus = status)

    @Test fun ownedTitlesGoFirstAndTheRestNewestFirst() {
        val (owned, more) = splitCredits(listOf(media(1, 5, "2019"), media(2, null, "2024"), media(3, 4, "2021"), media(4, 1, "2026")))
        assertEquals(listOf(1, 3), owned.map { it.remoteId })
        assertEquals(listOf(4, 2), more.map { it.remoteId })
    }
}
```

I `SeerrDiscoverClientTest`:

```kotlin
    @Test fun aPersonHasCreditsWithoutRepeats() {
        val client = SeerrDiscoverClient(Fake(mapOf(
            "person/501/combined_credits" to """{"cast":[{"id":11,"mediaType":"movie","title":"A","releaseDate":"2024-01-01"}],
                "crew":[{"id":11,"mediaType":"movie","title":"A","releaseDate":"2024-01-01"},{"id":12,"mediaType":"tv","name":"B","firstAirDate":"2025-02-02"}]}""",
            "person/501" to """{"id":501,"name":"Margaret Qualley","biography":"Bio","birthday":"1994-10-23","knownForDepartment":"Acting","profilePath":"/m.jpg"}""")))
        assertEquals("Margaret Qualley", client.person(seerr, 501).name)
        assertEquals(listOf(12, 11), client.personCredits(seerr, 501).map { it.remoteId })
    }
```

- [ ] **Steg 2: Køyr og sjå at dei feilar.** Kommando G, mønster `app.reelstack.data.*`. Forventa: FAIL.

- [ ] **Steg 3: Implementer data og modell**

`PersonPage.kt`:

```kotlin
package app.reelstack.data.model

data class PersonPage(
    val person: DiscoverPerson,
    val department: String? = null,
    val birthday: String? = null,
    val biography: String? = null,
    val inLibrary: List<DiscoverMedia> = emptyList(),
    val more: List<DiscoverMedia> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
)

/** What you have (available or partly) first; the rest newest first. */
fun splitCredits(credits: List<DiscoverMedia>): Pair<List<DiscoverMedia>, List<DiscoverMedia>> {
    val owned = credits.filter { it.inLibrary || it.seerrStatus == 4 || it.seerrStatus == 5 }
    val more = (credits - owned.toSet()).sortedByDescending { it.metadata }
    return owned to more
}
```

Parser:

```kotlin
    data class RemotePerson(val id: Int, val name: String, val portraitUrl: String?, val department: String?,
        val birthday: String?, val biography: String?)

    fun person(payload: String): RemotePerson {
        val item = json.parseToJsonElement(payload).jsonObject
        return RemotePerson(item.int("id") ?: 0, item.string("name").orEmpty(), item.string("profilePath")?.let { tmdbImage(it, "w342") },
            item.string("knownForDepartment"), item.string("birthday"), item.string("biography")?.takeIf(String::isNotBlank))
    }

    fun personCredits(payload: String): List<RemoteDiscoverItem> {
        val root = json.parseToJsonElement(payload).jsonObject
        return (root.array("cast") + root.array("crew")).mapNotNull { (it as? JsonObject)?.let(::discoverItem) }
            .distinctBy { it.mediaType to it.remoteId }
            .sortedByDescending { it.metadata }
    }
```

Klient:

```kotlin
    fun person(connection: ServiceConnection, id: Int): ServicePayloadParser.RemotePerson =
        ServicePayloadParser.person(get(connection, "api/v1/person/$id?language=nb"))

    fun personCredits(connection: ServiceConnection, id: Int): List<RemoteDiscoverItem> =
        ServicePayloadParser.personCredits(get(connection, "api/v1/person/$id/combined_credits?language=nb"))
```

Repository:

```kotlin
    fun person(connection: ServiceConnection, id: Int): app.reelstack.data.model.PersonPage {
        val person = seerrDiscoverClient.person(connection, id)
        val (owned, more) = app.reelstack.data.model.splitCredits(seerrDiscoverClient.personCredits(connection, id).map(::discoverMedia))
        return app.reelstack.data.model.PersonPage(
            app.reelstack.data.model.DiscoverPerson(person.id, person.name, person.portraitUrl, person.department),
            person.department, person.birthday, person.biography, owned, more)
    }
```

- [ ] **Steg 4: View-modell og ark**

`AppSheet`: `data class Person(val id: Int) : AppSheet`. `ReelstackUiState`: `val personPage: app.reelstack.data.model.PersonPage? = null,`.
`knownDiscoverMedia`: legg til `+ (personPage?.inLibrary.orEmpty() + personPage?.more.orEmpty())`. Byt den tomme `openPerson` frå Task 5 med:

```kotlin
    private var personJob: kotlinx.coroutines.Job? = null

    fun openPerson(id: Int) {
        val connection = _uiState.value.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank() } ?: return
        val known = _uiState.value.discoverHub.people.firstOrNull { it.id == id }
            ?: _uiState.value.searchPeople.firstOrNull { it.id == id }
            ?: _uiState.value.contentDetails?.cast?.firstOrNull { it.seerrPersonId == id }?.let {
                app.reelstack.data.model.DiscoverPerson(id, it.name, it.portraitUrl, it.role) }
            ?: app.reelstack.data.model.DiscoverPerson(id, "", null)
        _uiState.update { it.copy(activeSheet = AppSheet.Person(id),
            personPage = app.reelstack.data.model.PersonPage(known, loading = true)) }
        personJob?.cancel()
        personJob = operationScope.launch {
            val result = attempt { withContext(Dispatchers.IO) { container.mediaSyncRepository.person(connection, id) } }
            _uiState.update { state ->
                if (state.personPage?.person?.id != id) return@update state
                state.copy(personPage = result.getOrNull() ?: state.personPage.copy(loading = false, failed = true))
            }
        }
    }
```

`ReelstackSheets.kt`, der `AppSheet.TitleDetails` blir ruta (linje ~274–289), legg til ei grein som teiknar
`PersonSheet(state.personPage, onTitle = viewModel::openDiscoverDetails)` i den same `StableSheetDialog`.

`PersonSheet.kt`:

```kotlin
package app.reelstack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.PersonPage
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.isTelevision
import app.reelstack.ui.screens.PosterOnlyCard
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.SurfaceRaised

/** One page per person: picture, a little about them, what you have and what else they made. */
@Composable
fun PersonSheet(page: PersonPage?, onTitle: (String) -> Unit) {
    page ?: return
    val tv = isTelevision()
    var expanded by remember(page.person.id) { mutableStateOf(false) }
    var focusedTitle by remember(page.person.id) { mutableStateOf<String?>(null) }
    val width = if (tv) 120.dp else 104.dp
    LazyColumn(Modifier.fillMaxSize().testTag("person-sheet"), verticalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = PaddingValues(vertical = 24.dp)) {
        item(key = "head") {
            Row(Modifier.padding(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(if (tv) 168.dp else 112.dp).clip(CircleShape).background(SurfaceRaised)) {
                    page.person.portraitUrl?.let { MediaArtwork(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, source = ServiceKind.SEERR) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(page.person.name, style = if (tv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall)
                    Text(listOfNotNull(page.department, page.birthday?.let { stringResource(R.string.person_born, it) }).joinToString("  ·  "),
                        color = Muted, style = MaterialTheme.typography.bodyMedium)
                    page.biography?.let { bio ->
                        Text(bio, color = Muted, style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                        if (!expanded) Text(stringResource(R.string.person_read_more), style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.heightIn(min = 44.dp).clickable { expanded = true }.testTag("person-read-more"))
                    }
                }
            }
        }
        listOf(R.string.person_in_library to page.inLibrary, R.string.person_more_with to page.more).forEach { (label, titles) ->
            if (titles.isEmpty()) return@forEach
            item(key = "row-$label") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.padding(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(if (label == R.string.person_more_with) stringResource(label, page.person.name) else stringResource(label),
                            style = MaterialTheme.typography.titleLarge)
                        if (tv) focusedTitle?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium) }
                    }
                    LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(titles, key = { "${label}-${it.id}" }) { media ->
                            PosterOnlyCard(media, { onTitle(media.id) }, Modifier.width(width),
                                onFocused = { focusedTitle = listOf(media.title, media.metadata).filter(String::isNotBlank).joinToString("  ·  ") })
                        }
                    }
                }
            }
        }
    }
}
```

Strengar i dei tre `discover_hub.xml` (`nb`: «I biblioteket ditt», «Mer med %1$s», «Les mer», «født %1$s»;
`values`: «In your library», «More with %1$s», «Read more», «born %1$s»):

```xml
    <string name="person_in_library">I biblioteket ditt</string>
    <string name="person_more_with">Meir med %1$s</string>
    <string name="person_read_more">Les meir</string>
    <string name="person_born">fødd %1$s</string>
```

- [ ] **Steg 5: Køyr testane.** Kommando G, mønster `app.reelstack.data.*` og heile `testDebugUnitTest`. Forventa: PASS.

- [ ] **Steg 6: Commit**

```bash
git add -A app/src && git commit -m "Give every Seerr person one page" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Tittelsida: tekstsandlingar, Folk, Liknande og Tilrådd

**Filer:**
- Opprett: `app/src/main/java/app/reelstack/ui/components/TextActions.kt`
- Endre: `ui/ReelstackSheets.kt` (`castBlock` ~425–431, telefonknapp 612–648, `TvTitleRequestAction` 667–685)
- Endre: `ui/components/CastRail.kt` (klikk for Seerr-folk)
- Endre: `data/model/Models.kt` (`ContentDetails.similar`, `.recommended`)
- Endre: `ReelstackViewModel.kt` (`openDiscoverDetails`: parallell henting; `knownDiscoverMedia`)
- Endre: dei tre `discover_hub.xml`
- Test: `app/src/test/java/app/reelstack/ui/components/TextActionsTest.kt`

**Grensesnitt:**
- Konsumerer: `openPerson` (Task 7), `cancelTrackedRequest(key)`, `setFollowNotification(key, enabled)`, `similar/recommended` (Task 3).
- Produserer: `data class TextAction(val label: String, val icon: ImageVector, val tag: String, val onClick: () -> Unit)`;
  `@Composable fun TextActions(actions: List<TextAction>, modifier: Modifier = Modifier, focusFirst: Boolean = false)`;
  `fun seerrTitleActions(...)`; `ContentDetails.similar/recommended: List<DiscoverMedia>`.

- [ ] **Steg 1: Feilande test**

`TextActionsTest.kt` (Robolectric-oppsettet frå Task 2):

```kotlin
    @Test fun actionsAreTextWithoutAFilledSurface() {
        var clicked = ""
        rule.setContent { ReelstackTheme { TextActions(listOf(TextAction("Be om", SpoleIcons.Add, "title-request") { clicked = "be" },
            TextAction("Trekk tilbake", SpoleIcons.Close, "title-withdraw") { clicked = "trekk" })) } }
        rule.onNodeWithTag("title-withdraw").performClick()
        assertEquals("trekk", clicked)
        rule.onNodeWithTag("title-request").assertHasClickAction()
    }
```

- [ ] **Steg 2: Køyr og sjå at han feilar.** Kommando G, mønster `app.reelstack.ui.components.TextActionsTest`. Forventa: FAIL.

- [ ] **Steg 3: Implementer `TextActions.kt`**

```kotlin
package app.reelstack.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

data class TextAction(val label: String, val icon: ImageVector, val tag: String, val onClick: () -> Unit)

/** Actions as words with an icon, in a line that wraps. The first can take a remote's focus. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextActions(actions: List<TextAction>, modifier: Modifier = Modifier, focusFirst: Boolean = false) {
    if (actions.isEmpty()) return
    val first = remember { FocusRequester() }
    LaunchedEffect(focusFirst, actions.firstOrNull()?.tag) {
        if (focusFirst) { withFrameNanos { }; runCatching { first.requestFocus() } }
    }
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        actions.forEachIndexed { index, action ->
            val interaction = remember(action.tag) { MutableInteractionSource() }
            Row(Modifier.heightIn(min = 48.dp)
                .then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                .focusOutline(interaction, RoundedCornerShape(24.dp), glow = false)
                .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = action.onClick)
                .padding(horizontal = 16.dp).testTag(action.tag),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(action.icon, null, Modifier.size(20.dp))
                Text(action.label, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
```

- [ ] **Steg 4: Tittelarket**

Strengar (`nb`: «Be om», «Be om sesonger», «Trekk tilbake», «Varsel på», «Varsel av», «Lignende titler», «Anbefalt ut fra denne»;
`values`: «Request», «Request seasons», «Withdraw», «Alert on», «Alert off», «Similar titles», «Recommended from this»):

```xml
    <string name="title_action_request">Be om</string>
    <string name="title_action_request_seasons">Be om sesongar</string>
    <string name="title_action_withdraw">Trekk tilbake</string>
    <string name="title_action_notify_on">Varsel på</string>
    <string name="title_action_notify_off">Varsel av</string>
    <string name="title_similar">Liknande titlar</string>
    <string name="title_recommended">Tilrådd ut frå denne</string>
```

`ContentDetails`: legg til `val similar: List<DiscoverMedia> = emptyList(), val recommended: List<DiscoverMedia> = emptyList(),`.
`knownDiscoverMedia`: legg til `+ contentDetails?.similar.orEmpty() + contentDetails?.recommended.orEmpty()`.

`openDiscoverDetails`, inne i `operationScope.launch` før `val result = attempt {`:

```kotlin
            launch {
                val similar = attempt { withContext(Dispatchers.IO) { container.mediaSyncRepository.similar(connection, media) } }.getOrDefault(emptyList())
                val recommended = attempt { withContext(Dispatchers.IO) { container.mediaSyncRepository.recommended(connection, media) } }.getOrDefault(emptyList())
                _uiState.update { current ->
                    val details = current.contentDetails?.takeIf { it.key == media.id } ?: return@update current
                    current.copy(contentDetails = details.copy(similar = similar.take(20), recommended = recommended.take(20)))
                }
            }
```

I `RichTitleDetailsSheet`, rett etter `castBlock`-definisjonen:

```kotlin
    val tracked = state.trackedRequests.firstOrNull { discoverMedia != null && it.mediaId == discoverMedia.remoteId &&
        it.mediaType == discoverMedia.mediaType && !it.is4k }
    val seerrActions = buildList {
        if (discoverMedia != null && discoverMedia.canRequest && (state.configuredCount == 0 ||
                state.accounts[ServiceKind.SEERR]?.let { discoverMedia.mediaType == "tv" || !it.isPersonal || it.canRequestType(discoverMedia.mediaType ?: "movie") } == true)) {
            val needsAccount = state.connections.any { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() } && state.accounts[ServiceKind.SEERR]?.isPersonal != true
            add(app.reelstack.ui.components.TextAction(stringResource(if (discoverMedia.mediaType == "tv") R.string.title_action_request_seasons else R.string.title_action_request),
                app.reelstack.ui.components.SpoleIcons.Add, "title-request") { if (needsAccount) onSeerrAccount() else onAddMedia(discoverMedia.id) })
        }
        if (tracked != null && tracked.requestId != null && !tracked.availabilityOnly && tracked.stage == app.reelstack.data.model.RequestStage.REQUESTED)
            add(app.reelstack.ui.components.TextAction(stringResource(R.string.title_action_withdraw), app.reelstack.ui.components.SpoleIcons.Close,
                "title-withdraw") { onWithdraw(tracked.key) })
        if (tracked != null) add(app.reelstack.ui.components.TextAction(
            stringResource(if (tracked.notify) R.string.title_action_notify_on else R.string.title_action_notify_off),
            if (tracked.notify) app.reelstack.ui.components.SpoleIcons.Bell else app.reelstack.ui.components.SpoleIcons.BellOff,
            "title-notify") { onNotify(tracked.key, !tracked.notify) })
    }
    val relatedBlock: @Composable () -> Unit = {
        listOf(R.string.title_similar to details.similar, R.string.title_recommended to details.recommended).forEach { (label, titles) ->
            if (titles.isEmpty()) return@forEach
            Text(stringResource(label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp, bottom = 12.dp))
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(titles.size, key = { titles[it].id }) { index ->
                    app.reelstack.ui.screens.PosterOnlyCard(titles[index], { onRelated(titles[index].id) }, Modifier.width(if (tv) 120.dp else 104.dp))
                }
            }
        }
    }
```

Nye parametrar på `RichTitleDetailsSheet`: `onWithdraw: (String) -> Unit = {}`, `onNotify: (String, Boolean) -> Unit = { _, _ -> }`,
`onRelated: (String) -> Unit = {}`, `onSeerrPerson: (Int) -> Unit = {}`; kopla i `AppOverlayHost.kt` til
`viewModel::cancelTrackedRequest`, `viewModel::setFollowNotification`, `viewModel::openDiscoverDetails`, `viewModel::openPerson`.

Byt telefonknappen (linje ~612–648, `if (!tv && discoverMedia != null && discoverMedia.canRequest ...) { ... Button(...) }`) med:

```kotlin
        if (!tv) app.reelstack.ui.components.TextActions(seerrActions, Modifier.padding(top = 18.dp))
        relatedBlock()
```

og `TvTitleRequestAction(...)`-kallet med `app.reelstack.ui.components.TextActions(seerrActions, Modifier.padding(top = 16.dp), focusFirst = true)`
(behald `RequestIdentity` over handlingane på telefon). Slett `TvTitleRequestAction`.

`castBlock`: send `onSeerrPerson` vidare: `CastRail(details.cast, details.source, onPersonTitles, onPersonTitle, onSeerrPerson)`.

`CastRail.kt`: ny parameter `onSeerrPerson: (Int) -> Unit = {}`; klikket:

```kotlin
                .clickable(interactionSource = interaction, indication = mediaCardIndication(),
                    enabled = person.seerrPersonId != null || (person.remoteId != null && source != null), role = Role.Button) {
                    val seerrId = person.seerrPersonId
                    if (seerrId != null) onSeerrPerson(seerrId) else selected = if (selected == person) null else person
                }
```

og `key = { it.remoteId ?: it.seerrPersonId?.toString() ?: it.name }` (begge stader).

- [ ] **Steg 5: Køyr testane.** Kommando G, mønster `app.reelstack.ui.components.TextActionsTest`, så heile `testDebugUnitTest`. Forventa: PASS.
Testar som ventar `tv-title-request` skal bruke `title-request`.

- [ ] **Steg 6: Commit**

```bash
git add -A app/src && git commit -m "Turn title actions into words and add related rows" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Førespurnadsval i datalaget

**Filer:**
- Opprett: `app/src/main/java/app/reelstack/data/model/RequestOptions.kt`
- Endre: `data/model/ServiceAccount.kt` (nye rettar)
- Endre: `SeerrDiscoverClient.kt` (`requestOptions`, `fourKEnabled`), `ServicePayloadParser.kt` (`serviceServers`, `serviceDetails`)
- Endre: `ServiceClients.kt` (`SeerrServiceClient.request` 1453–1481)
- Endre: `MediaSyncRepository.kt` (`request`, `requestOptions`, `fourKEnabled`)
- Test: `app/src/test/java/app/reelstack/data/network/RequestOptionsTest.kt`

**Grensesnitt:**
- Produserer: `data class RequestProfile(id: Int, name: String)`, `data class RequestFolder(path: String, freeBytes: Long?)` med `.name`,
  `data class RequestOptions(serverId: Int, is4k: Boolean, profiles, folders, defaultProfileId: Int?, defaultFolder: String?)`,
  `data class RequestChoice(is4k: Boolean = false, serverId: Int? = null, profileId: Int? = null, rootFolder: String? = null)`,
  `fun formatFreeSpace(bytes: Long, locale: java.util.Locale): String`,
  `ServiceAccount.canRequest4k(type: String)`, `ServiceAccount.canChooseRequestOptions()`,
  `SeerrDiscoverClient.requestOptions(connection, mediaType, is4k): RequestOptions?`, `.fourKEnabled(connection, mediaType): Boolean`,
  `SeerrServiceClient.request(..., choice: RequestChoice = RequestChoice())`, `MediaSyncRepository.request(..., choice)`.

- [ ] **Steg 1: Feilande testar**

`RequestOptionsTest.kt`:

```kotlin
package app.reelstack.data.network

import app.reelstack.data.model.*
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class RequestOptionsTest {
    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "cookie", userId = "7", sessionCookie = true)

    private class Fake(private val bodies: Map<String, String>) : JsonHttpTransport {
        var posted: String? = null
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            val key = bodies.keys.filter { url.contains(it) }.maxByOrNull { it.length } ?: return HttpResponse(404, "{}")
            return HttpResponse(200, bodies.getValue(key))
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse { posted = jsonBody; return HttpResponse(201, "{}") }
    }

    private val radarr = mapOf(
        "service/radarr/0" to """{"server":{"id":0,"is4k":false,"activeProfileId":4,"activeDirectory":"/mnt/w/Filmar 10"},
            "profiles":[{"id":4,"name":"HD-1080p"},{"id":5,"name":"Ultra-HD"}],
            "rootFolders":[{"id":1,"path":"/mnt/w/Filmar 10","freeSpace":4815000000000},{"id":2,"path":"/data/film","freeSpace":null}]}""",
        "service/radarr" to """[{"id":0,"name":"Radarr","is4k":false,"isDefault":true},{"id":1,"name":"Radarr 4K","is4k":true,"isDefault":true}]""",
    )

    @Test fun optionsComeFromTheDefaultServerWithFreeSpace() {
        val options = SeerrDiscoverClient(Fake(radarr)).requestOptions(seerr, "movie", is4k = false)!!
        assertEquals(0, options.serverId)
        assertEquals(listOf("HD-1080p", "Ultra-HD"), options.profiles.map { it.name })
        assertEquals(4, options.defaultProfileId)
        assertEquals("Filmar 10", options.folders.first().name)
        assertEquals(4815000000000L, options.folders.first().freeBytes)
        assertNull(options.folders.last().freeBytes)
    }

    @Test fun freeSpaceIsSiWithOneDecimal() {
        assertEquals("4,8 TB", formatFreeSpace(4_815_000_000_000, Locale.forLanguageTag("nn-NO")))
        assertEquals("950,0 GB", formatFreeSpace(950_000_000_000, Locale.forLanguageTag("nn-NO")))
        assertEquals("4.8 TB", formatFreeSpace(4_815_000_000_000, Locale.ENGLISH))
    }

    @Test fun permissionsGateTheChoices() {
        fun account(bits: Long) = ServiceAccount(ServiceKind.SEERR, "7", "Ø", permissions = bits)
        assertTrue(account(8192).canChooseRequestOptions())
        assertTrue(account(16).canChooseRequestOptions())
        assertFalse(account(32).canChooseRequestOptions())
        assertTrue(account(2048).canRequest4k("movie"))
        assertFalse(account(2048).canRequest4k("tv"))
        assertTrue(account(1024).canRequest4k("tv"))
    }

    @Test fun thePostCarriesOnlyTheShownChoices() {
        val fake = Fake(mapOf("auth/me" to """{"id":7,"displayName":"Ø","permissions":8224}"""))
        SeerrServiceClient(fake).request(seerr, "movie", 11, "7", choice = RequestChoice(serverId = 0, profileId = 5, rootFolder = "/data/film"))
        val body = Json.parseToJsonElement(fake.posted!!).jsonObject
        assertEquals("5", body.getValue("profileId").jsonPrimitive.content)
        assertEquals("/data/film", body.getValue("rootFolder").jsonPrimitive.content)
        assertNull(body["is4k"])
    }

    @Test fun fourKSeriesChecksFourKSeasons() {
        val details = """{"name":"S","seasons":[{"seasonNumber":1}],"mediaInfo":{"status":1,"status4k":1,
            "seasons":[{"seasonNumber":1,"status":5,"status4k":1}]}}"""
        val fake = Fake(mapOf("auth/me" to """{"id":7,"displayName":"Ø","permissions":4128}""", "tv/11" to details))
        SeerrServiceClient(fake).request(seerr, "tv", 11, "7", seasons = setOf(1), choice = RequestChoice(is4k = true))
        assertEquals("true", Json.parseToJsonElement(fake.posted!!).jsonObject.getValue("is4k").jsonPrimitive.content)
    }
}
```

- [ ] **Steg 2: Køyr og sjå at dei feilar.** Kommando G, mønster `app.reelstack.data.network.RequestOptionsTest`. Forventa: FAIL.

- [ ] **Steg 3: Implementer**

`RequestOptions.kt`:

```kotlin
package app.reelstack.data.model

data class RequestProfile(val id: Int, val name: String)

data class RequestFolder(val path: String, val freeBytes: Long?) {
    /** The last folder name is what a person recognises; the full path is detail. */
    val name: String get() = path.trimEnd('/').substringAfterLast('/').ifBlank { path }
}

data class RequestOptions(
    val serverId: Int,
    val is4k: Boolean,
    val profiles: List<RequestProfile>,
    val folders: List<RequestFolder>,
    val defaultProfileId: Int?,
    val defaultFolder: String?,
)

/** What the request sheet sends; only the choices it showed. */
data class RequestChoice(val is4k: Boolean = false, val serverId: Int? = null, val profileId: Int? = null, val rootFolder: String? = null)

/** «4,8 TB»: SI units, one decimal, the reader's decimal mark. */
fun formatFreeSpace(bytes: Long, locale: java.util.Locale): String {
    val (value, unit) = when {
        bytes >= 1_000_000_000_000 -> bytes / 1e12 to "TB"
        bytes >= 1_000_000_000 -> bytes / 1e9 to "GB"
        else -> bytes / 1e6 to "MB"
    }
    return String.format(locale, "%.1f %s", value, unit)
}
```

`ServiceAccount.kt`:

```kotlin
fun ServiceAccount.canRequest4k(type: String): Boolean = isPersonal &&
    (isAdmin || permissions and 1024L != 0L || permissions and (if (type == "tv") 4096L else 2048L) != 0L)

/** Seerr's «advanced requests», or a request manager: may choose quality and location. */
fun ServiceAccount.canChooseRequestOptions(): Boolean = isPersonal && (isAdmin || permissions and (8192L or 16L) != 0L)
```

Parser:

```kotlin
    data class ServiceServer(val id: Int, val is4k: Boolean, val isDefault: Boolean)

    fun serviceServers(payload: String): List<ServiceServer> =
        (runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray).orEmpty().mapNotNull {
            val server = it as? JsonObject ?: return@mapNotNull null
            ServiceServer(server.int("id") ?: return@mapNotNull null,
                server["is4k"]?.jsonPrimitive?.booleanOrNull == true, server["isDefault"]?.jsonPrimitive?.booleanOrNull == true)
        }

    fun serviceDetails(payload: String, is4k: Boolean): app.reelstack.data.model.RequestOptions? {
        val root = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonObject ?: return null
        val server = root.obj("server") ?: return null
        return app.reelstack.data.model.RequestOptions(
            serverId = server.int("id") ?: return null,
            is4k = is4k,
            profiles = root.array("profiles").mapNotNull { (it as? JsonObject)?.let { p ->
                app.reelstack.data.model.RequestProfile(p.int("id") ?: return@mapNotNull null, p.string("name") ?: return@mapNotNull null) } },
            folders = root.array("rootFolders").mapNotNull { (it as? JsonObject)?.let { f ->
                app.reelstack.data.model.RequestFolder(f.string("path") ?: return@mapNotNull null, f["freeSpace"]?.jsonPrimitive?.longOrNull) } },
            defaultProfileId = server.int("activeProfileId"),
            defaultFolder = server.string("activeDirectory"),
        )
    }
```

(import `kotlinx.serialization.json.longOrNull` om han manglar.)

Klient:

```kotlin
    fun requestOptions(connection: ServiceConnection, mediaType: String, is4k: Boolean): app.reelstack.data.model.RequestOptions? {
        val service = if (mediaType == "tv") "sonarr" else "radarr"
        val servers = ServicePayloadParser.serviceServers(get(connection, "api/v1/service/$service"))
        val server = servers.firstOrNull { it.is4k == is4k && it.isDefault } ?: servers.firstOrNull { it.is4k == is4k } ?: return null
        return ServicePayloadParser.serviceDetails(get(connection, "api/v1/service/$service/${server.id}"), is4k)
    }

    fun fourKEnabled(connection: ServiceConnection, mediaType: String): Boolean {
        val root = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(get(connection, "api/v1/settings/public")) }.getOrNull()
            as? kotlinx.serialization.json.JsonObject ?: return false
        val key = if (mediaType == "tv") "series4kEnabled" else "movie4kEnabled"
        return (root[key] as? kotlinx.serialization.json.JsonPrimitive)?.content == "true"
    }
```

`SeerrServiceClient.request`: ny siste parameter `choice: app.reelstack.data.model.RequestChoice = app.reelstack.data.model.RequestChoice()`;
i serie-sjekken:

```kotlin
            val fresh = details(connection, mediaType, remoteId)
            val freshStatus = if (choice.is4k) fresh.status4k else fresh.seerrStatus
            val freshSeasons = if (choice.is4k) fresh.seasons4k else fresh.seasons
            if (freshStatus == 6 || seasons.any { number -> freshSeasons.none { it.number == number && it.canRequest } }) {
                serviceError(R.string.err_sesongane_er_endra)
            }
```

og i `buildJsonObject`:

```kotlin
            if (choice.is4k) put("is4k", true)
            choice.serverId?.let { put("serverId", it) }
            choice.profileId?.let { put("profileId", it) }
            choice.rootFolder?.let { put("rootFolder", it) }
```

`MediaSyncRepository.request`: ny parameter `choice: app.reelstack.data.model.RequestChoice = app.reelstack.data.model.RequestChoice()` som blir sendt vidare;
og

```kotlin
    fun requestOptions(connection: ServiceConnection, mediaType: String, is4k: Boolean) =
        seerrDiscoverClient.requestOptions(connection, mediaType, is4k)

    fun fourKEnabled(connection: ServiceConnection, mediaType: String) = seerrDiscoverClient.fourKEnabled(connection, mediaType)
```

- [ ] **Steg 4: Køyr testane.** Kommando G, mønster `app.reelstack.data.network.RequestOptionsTest`, så `app.reelstack.data.*`. Forventa: PASS.

- [ ] **Steg 5: Commit**

```bash
git add -A app/src && git commit -m "Read and send Seerr's quality, location and 4K choices" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Be om-arket med Utgåve, Kvalitet og Lagringsstad

**Filer:**
- Endre: `data/model/RequestFlow.kt` (`RequestDraft` 140–154)
- Endre: `ReelstackViewModel.kt` (`requestMedia` 2542–2592, `sendRequest` 3210, nye `setRequestEdition`, `setRequestProfile`, `setRequestFolder`)
- Endre: `ui/RequestFlowUi.kt` (`RequestComposer` 55–246), `ui/ReelstackSheets.kt` (callbacks), `ui/AppOverlayHost.kt`
- Endre: dei tre `discover_hub.xml`
- Test: `app/src/test/java/app/reelstack/ui/RequestOptionsSheetTest.kt`

**Grensesnitt:**
- Konsumerer: alt frå Task 9.
- Produserer: `RequestDraft.fourK: Boolean`, `.fourKAvailable: Boolean`, `.seasons4k: List<RequestSeason>`, `.options: RequestOptions?`,
  `.profileId: Int?`, `.folder: String?`, `val RequestDraft.choice: RequestChoice`, `val RequestDraft.visibleSeasons: List<RequestSeason>`;
  `ReelstackViewModel.setRequestEdition(fourK: Boolean)`, `.setRequestProfile(id: Int)`, `.setRequestFolder(path: String)`.

- [ ] **Steg 1: Feilande test**

`RequestOptionsSheetTest.kt` (Robolectric, telefonbreidd):

```kotlin
    private val options = RequestOptions(0, false, listOf(RequestProfile(4, "HD-1080p"), RequestProfile(5, "Ultra-HD")),
        listOf(RequestFolder("/mnt/w/Filmar 10", 4_815_000_000_000)), defaultProfileId = 4, defaultFolder = "/mnt/w/Filmar 10")
    private val media = DiscoverMedia("seerr-movie-11", "Love of Your Life", "2026", R.drawable.media_placeholder, false, remoteId = 11, mediaType = "movie")
    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "c", userId = "7", sessionCookie = true)

    private fun show(permissions: Long, draft: RequestDraft) = rule.setContent { ReelstackTheme {
        RequestComposer(ReelstackUiState(connections = listOf(seerr),
            accounts = mapOf(ServiceKind.SEERR to ServiceAccount(ServiceKind.SEERR, "7", "Ø", permissions = permissions)),
            requestDraft = draft), {_, _ -> }, {}, {}, {}, {}, {})
    } }

    @Test fun advancedUsersSeeQualityAndLocation() {
        show(8224, RequestDraft(media, loading = false, options = options, profileId = 4, folder = "/mnt/w/Filmar 10"))
        rule.onNodeWithTag("request-quality").assertTextContains("HD-1080p", substring = true)
        rule.onNodeWithTag("request-folder").assertTextContains("Filmar 10", substring = true)
        rule.onNodeWithTag("request-folder").assertTextContains("4,8 TB", substring = true)
    }

    @Test fun ordinaryUsersSeeNoChoices() {
        show(32, RequestDraft(media, loading = false))
        rule.onNodeWithTag("request-quality").assertDoesNotExist()
        rule.onNodeWithTag("request-edition").assertDoesNotExist()
    }
```

(`@Config(qualifiers = "w412dp-h900dp-xhdpi", ...)`, `Locale` for Robolectric: legg `@Config(qualifiers = "nn-w412dp-h900dp-xhdpi")` om
desimalteiknet elles blir punktum.)

- [ ] **Steg 2: Køyr og sjå at han feilar.** Kommando G, mønster `app.reelstack.ui.RequestOptionsSheetTest`. Forventa: FAIL.

- [ ] **Steg 3: Draft og view-modell**

`RequestDraft`: legg til sist

```kotlin
    val fourK: Boolean = false,
    val fourKAvailable: Boolean = false,
    val seasons4k: List<RequestSeason> = emptyList(),
    val options: RequestOptions? = null,
    val optionsLoading: Boolean = false,
    val profileId: Int? = null,
    val folder: String? = null,
)

val RequestDraft.visibleSeasons: List<RequestSeason> get() = if (fourK) seasons4k else seasons

val RequestDraft.choice: RequestChoice get() = RequestChoice(
    is4k = fourK,
    serverId = options?.serverId,
    profileId = profileId.takeIf { options != null },
    rootFolder = folder.takeIf { options != null },
)
```

Byt `draft.seasons` med `draft.visibleSeasons` i `RequestComposer`, `setRequestSeason`, `setSeasonWatch`
og `confirmRequest` (`grep -n "draft.seasons" app/src/main/java`).

I `requestMedia`, inne i `withContext(Dispatchers.IO)`, ved sida av `rules`:

```kotlin
                val actor = state.accounts[ServiceKind.SEERR]
                val type = media.mediaType ?: "movie"
                val fourKAvailable = async { actor?.canRequest4k(type) == true &&
                    attempt { container.mediaSyncRepository.fourKEnabled(requireNotNull(connection), type) }.getOrDefault(false) }
                val options = async { if (actor?.canChooseRequestOptions() == true)
                    attempt { container.mediaSyncRepository.requestOptions(requireNotNull(connection), type, is4k = false) }.getOrNull() else null }
```

returner dei i ei data-klasse i staden for `Triple` (`private data class DraftLoad(val remote: RemoteMediaDetails, val watched: Set<Int>, val rules: RequestRules?, val fourK: Boolean, val options: RequestOptions?)`),
og i `onSuccess`:

```kotlin
                    draft.copy(loading = false, seasons = remote.seasons, seasons4k = remote.seasons4k, selected = emptySet(),
                        mediaStatus = remote.seerrStatus, nextEpisode = remote.nextEpisode, watchedSeasons = watched, rules = rules,
                        fourKAvailable = fourK, options = options, profileId = options?.defaultProfileId ?: options?.profiles?.firstOrNull()?.id,
                        folder = options?.defaultFolder ?: options?.folders?.firstOrNull()?.path,
                        error = when {
                            remote.seerrStatus == 6 -> appString(R.string.flow_status_blocked_admin)
                            media.mediaType == "tv" && remote.seasons.isEmpty() -> appString(R.string.flow_status_no_seasons)
                            media.mediaType != "tv" && remote.seerrStatus in 2..5 -> appString(R.string.flow_status_movie_already)
                            else -> null
                        })
```

Ein film som alt finst i standard, kan framleis bestillast i 4K. Legg difor `val mediaStatus4k: Int? = null,`
til `RequestDraft`, set `mediaStatus4k = remote.status4k` i `onSuccess` over, og la feilen for «finst
alt» gjelde utgåva som er vald. I `setRequestEdition`, i den første `_uiState.update`, rekn ut feilen på nytt:

```kotlin
            error = if (draft.media.mediaType != "tv" && (if (fourK) draft.mediaStatus4k else draft.mediaStatus) in 2..5)
                appString(R.string.flow_status_movie_already) else null,
```

Og når filmen er tilgjengeleg i standard men 4K er mogleg, skal ikkje opninga av arket stoppe på
feilen: byt den tredje linja i `when` over til
`media.mediaType != "tv" && remote.seerrStatus in 2..5 && !(fourK && remote.status4k !in 2..5) -> appString(R.string.flow_status_movie_already)`.

Nye funksjonar:

```kotlin
    fun setRequestEdition(fourK: Boolean) {
        val state = _uiState.value
        val draft = state.requestDraft ?: return
        if (draft.sending || draft.fourK == fourK || (fourK && !draft.fourKAvailable)) return
        _uiState.update { it.copy(requestDraft = draft.copy(fourK = fourK, selected = emptySet(), options = null,
            profileId = null, folder = null, optionsLoading = state.accounts[ServiceKind.SEERR]?.canChooseRequestOptions() == true)) }
        if (state.accounts[ServiceKind.SEERR]?.canChooseRequestOptions() != true) return
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.token.isNotBlank() } ?: return
        operationScope.launch {
            val options = attempt { withContext(Dispatchers.IO) {
                container.mediaSyncRepository.requestOptions(connection, draft.media.mediaType ?: "movie", fourK) } }.getOrNull()
            _uiState.update { current ->
                val latest = current.requestDraft?.takeIf { it.media.id == draft.media.id && it.fourK == fourK } ?: return@update current
                current.copy(requestDraft = latest.copy(optionsLoading = false, options = options,
                    profileId = options?.defaultProfileId ?: options?.profiles?.firstOrNull()?.id,
                    folder = options?.defaultFolder ?: options?.folders?.firstOrNull()?.path))
            }
        }
    }

    fun setRequestProfile(id: Int) = _uiState.update { state ->
        val draft = state.requestDraft?.takeIf { !it.sending && it.options?.profiles?.any { p -> p.id == id } == true } ?: return@update state
        state.copy(requestDraft = draft.copy(profileId = id))
    }

    fun setRequestFolder(path: String) = _uiState.update { state ->
        val draft = state.requestDraft?.takeIf { !it.sending && it.options?.folders?.any { f -> f.path == path } == true } ?: return@update state
        state.copy(requestDraft = draft.copy(folder = path))
    }
```

`sendRequest`: `container.mediaSyncRepository.request(seerr, media, expectedUserId = account.id, seasons = draft.selected, choice = draft.choice)`.

- [ ] **Steg 4: UI i arket**

Strengar (`nb`: «Utgave», «Standard», «4K», «Kvalitet», «Lagringssted», «vanlig valg», «%1$s ledig», «Velg kvalitet», «Velg lagringssted»;
`values`: «Edition», «Standard», «4K», «Quality», «Location», «usual choice», «%1$s free», «Choose quality», «Choose location»):

```xml
    <string name="request_edition">Utgåve</string>
    <string name="request_edition_standard">Standard</string>
    <string name="request_edition_4k">4K</string>
    <string name="request_quality">Kvalitet</string>
    <string name="request_folder">Lagringsstad</string>
    <string name="request_usual_choice">vanleg val</string>
    <string name="request_free_space">%1$s ledig</string>
    <string name="request_choose_quality">Vel kvalitet</string>
    <string name="request_choose_folder">Vel lagringsstad</string>
```

`RequestComposer`: nye parametrar `onEdition: (Boolean) -> Unit = {}`, `onProfile: (Int) -> Unit = {}`, `onFolder: (String) -> Unit = {}`.
Inne i den rullbare kolonnen, rett før `if (ready && hasSelection && canAdd) { HorizontalDivider(...)` (varselblokka), legg til:

```kotlin
            if (ready) RequestChoiceRows(draft, onEdition, onProfile, onFolder)
```

og i fila:

```kotlin
@Composable
private fun RequestChoiceRows(draft: RequestDraft, onEdition: (Boolean) -> Unit, onProfile: (Int) -> Unit, onFolder: (String) -> Unit) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    var picking by remember(draft.media.id) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        if (draft.fourKAvailable) ChoiceRow(stringResource(R.string.request_edition), "request-edition") {
            listOf(false to R.string.request_edition_standard, true to R.string.request_edition_4k).forEach { (value, label) ->
                val chosen = draft.fourK == value
                Text(stringResource(label), color = if (chosen) MaterialTheme.colorScheme.onSurface else Muted,
                    modifier = Modifier.padding(start = 20.dp).heightIn(min = 44.dp)
                        .toggleable(chosen, role = Role.RadioButton, enabled = !draft.sending) { onEdition(value) }
                        .testTag("request-edition-${if (value) "4k" else "standard"}"))
            }
        }
        draft.options?.let { options ->
            val profile = options.profiles.firstOrNull { it.id == draft.profileId }
            ChoiceRow(stringResource(R.string.request_quality), "request-quality", onClick = { picking = "quality" }) {
                ValueColumn(profile?.name.orEmpty(), stringResource(R.string.request_usual_choice).takeIf { draft.profileId == options.defaultProfileId })
            }
            val folder = options.folders.firstOrNull { it.path == draft.folder }
            ChoiceRow(stringResource(R.string.request_folder), "request-folder", onClick = { picking = "folder" }) {
                ValueColumn(listOfNotNull(folder?.name, folder?.freeBytes?.let { stringResource(R.string.request_free_space, formatFreeSpace(it, locale)) })
                    .joinToString("  ·  "),
                    listOfNotNull(folder?.path, stringResource(R.string.request_usual_choice).takeIf { draft.folder == options.defaultFolder }).joinToString(" · "))
            }
            when (picking) {
                "quality" -> app.reelstack.ui.components.SpoleChoiceDialog(stringResource(R.string.request_choose_quality), { picking = null }) {
                    options.profiles.forEach { p -> app.reelstack.ui.components.SpoleChoiceRow(p.name, p.id == draft.profileId) { onProfile(p.id); picking = null } }
                }
                "folder" -> app.reelstack.ui.components.SpoleChoiceDialog(stringResource(R.string.request_choose_folder), { picking = null }) {
                    options.folders.forEach { f -> app.reelstack.ui.components.SpoleChoiceRow(f.name, f.path == draft.folder,
                        supportingText = listOfNotNull(f.path, f.freeBytes?.let { stringResource(R.string.request_free_space, formatFreeSpace(it, locale)) }).joinToString(" · ")) {
                        onFolder(f.path); picking = null } }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, tag: String, onClick: (() -> Unit)? = null, value: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = app.reelstack.ui.theme.Text.copy(alpha = .1f))
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .testTag(tag).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            value()
            if (onClick != null) Icon(app.reelstack.ui.components.SpoleIcons.ChevronRight, null, tint = Muted, modifier = Modifier.padding(start = 8.dp).size(18.dp))
        }
    }
}

@Composable
private fun ValueColumn(main: String, detail: String?) {
    Column(horizontalAlignment = Alignment.End) {
        Text(main, style = MaterialTheme.typography.bodyLarge)
        detail?.takeIf(String::isNotBlank)?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}
```

Kontroller signaturen til `SpoleChoiceDialog`/`SpoleChoiceRow` med
`grep -n "fun SpoleChoiceDialog\|fun SpoleChoiceRow" -A 3 app/src/main/java/app/reelstack/ui/components/SpoleChoiceDialog.kt`
og tilpass kalla (dei blir alt brukte slik i `SmartShelfPage.kt`).

Stadfest-knappen nedst: byt `Button(onClick = {...}, enabled = ..., modifier = ...)` med ein tekstknapp i
lime utan flate:

```kotlin
            } else androidx.compose.material3.TextButton(onClick = {
                if (draft.notify && state.notificationsEnabled && state.configuredCount > 0 && Build.VERSION.SDK_INT >= 33 && !LibraryNotifications.allowed(context))
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else onConfirm()
            }, enabled = ready && !busy && canAdd && draft.rules?.canRequest != false && !draft.quotaExceeded && draft.error == null && hasSelection,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Primary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).focusRequester(confirmFocus).testTag("confirm-request")) {
                if (draft.sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                Text(if (draft.sending) stringResource(R.string.flow_sending) else if (isSeries && draft.selected.isEmpty()) stringResource(R.string.flow_choose_seasons)
                    else if (isSeries) pluralStringResource(R.plurals.flow_send_seasons, draft.selected.size, draft.selected.size) else stringResource(R.string.flow_send),
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium)
                Icon(app.reelstack.ui.components.SpoleIcons.ArrowForward, null)
            }
```
Kople `onEdition = viewModel::setRequestEdition`, `onProfile = viewModel::setRequestProfile`,
`onFolder = viewModel::setRequestFolder` der `RequestComposer` blir kalla (`ReelstackSheets.kt`).

- [ ] **Steg 5: Køyr testane.** Kommando G, mønster `app.reelstack.ui.RequestOptionsSheetTest`, så heile `testDebugUnitTest`. Forventa: PASS.

- [ ] **Steg 6: Commit**

```bash
git add -A app/src && git commit -m "Let a request choose edition, quality and location" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 11: Dokumentasjon og opprydding

**Filer:**
- Endre: `docs/LAYOUT.md` (punkta for Oppdag og Aktivitet, linje ~76–86), `docs/REQUEST_FLOW.md` (linje 12 og 54), `docs/DESIGN_SYSTEM.md` (sidetittel-lina 163)
- Endre: `docs/OPPDAG_SAMLA_2026-10-16.md` (avvik under)
- Slett: ubrukte strengar (`activity_*`, `history_*`, `discover_subtitle`, `filter_*` som berre filtera brukte)

- [ ] **Steg 1: Docs**

`LAYOUT.md`, linja om fire faste stader:

```markdown
Four persistent destinations: **Heim**, **Bibliotek**, **Oppdag**, **Innstillingar**. Aktivitet is no longer a tab:
«Dine førespurnader» lives inside Oppdag (see `OPPDAG_SAMLA_2026-10-16.md`).
```

og byt Oppdag- og Aktivitet-punkta med:

```markdown
- **Oppdag:** search on top (phone) or a search icon in the header (TV); then «Dine førespurnader», Populært no,
  Filmar som kjem, Seriar som kjem, Folk and Sjangrar. Posters carry no text or buttons; on TV the focused title fills
  the header. Rows load independently; a failed row is gone with one quiet retry line.
- **Dine førespurnader:** text tabs Alle · På veg · Klare (+ Tenaren for administrators), one line per request with a
  thin progress line. Lines open the title page, where withdraw and alerts live.
```

`REQUEST_FLOW.md`: «Aktivitet opens after a successful send» → «the title page stays open and its status line says
Førespurd»; «tapping a notification opens Aktivitet» → «tapping a notification opens Oppdag with Dine førespurnader».
Legg til ein bolk «Utgåve, Kvalitet og Lagringsstad» med rettsbitane frå spesifikasjonen.

`OPPDAG_SAMLA_2026-10-16.md`, ny bolk «Avvik under bygginga»: TV-toppfeltet brukar data frå sjølve rada
(tittel, fakta, samandrag, status) og hentar ingenting ekstra ved fokus, fordi radene alt har det; 4K blir
tilbydd når Seerr sitt `settings/public` seier at 4K er på og brukaren har 4K-rett; søket på TV er det
eksisterande søkjefeltet i sidehovudet (som opnar tastaturet først ved OK), ikkje eit nytt ikon.

- [ ] **Steg 2: Strengar og lint**

```bash
grep -rhoE 'name="(activity|history|discover_subtitle|filter)_[a-z_]+"' app/src/main/res/values/*.xml | sort -u
```

For kvar nøkkel: `grep -rn "R.string.<nøkkel>" app/src` — er han ubrukt, slett han i alle tre språk.
Køyr Kommando G utan `--tests` og `./gradlew.bat lintDebug --console=plain`. Forventa: 0 lint-feil, alle einingstestar grøne.
Køyr `python3 scripts/check-translations.py` i WSL. Forventa: «Translation keys OK».

- [ ] **Steg 3: Commit**

```bash
git add -A docs app/src && git commit -m "Document the merged Discover and drop Activity leftovers" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 12: Verifisering og 1.0.0-beta14

**Filer:**
- Endre: `app/build.gradle.kts` (versionCode 151, versionName `1.0.0-beta14`), `CHANGELOG.md`, `README.md`
- Opprett: `docs/release-v1.0.0-beta14.md`, `docs/VERIFICATION_v1.0.0-beta14.md`

- [ ] **Steg 1: Android-testar på 5566**

Kommando B, så Kommando A med:
`SmartShelvesTest HomeMediaRowsTest HomeLayoutUiTest LibraryPresentationUiTest DesignRefreshUiTest TvNavigationIntegrationTest
TvSettingsRedesignTest WideNavigationSettingsTest RequestFlowUiTest RequestRulesUiTest TvControlsRegressionTest SetupAndDiscoverTest
FocusOutlineTest UiConsistencyTest ViewerExperienceTest TvRefinementUiTest`.
Forventa: grønt, unnateke dei kjende feila i minnet (`TvRefinementUiTest.libraryEditorGivesARemoteItsFirstFocus`,
`remoteFocusKeepsRailGeometryAndCaptionBaselinesStable`, `LibraryPresentationUiTest.libraryOverviewHasOneNextRowIndependentOfHomeSwitches`).
Nye feil: rett koden eller testen før du går vidare. Klassar som berre prøvde Aktivitet (`ActivityAdaptiveTest`,
`RequestHistoryUiTest`) er sletta i Task 2; skriv dei som trengst om til `MyRequestsScreen`.

- [ ] **Steg 2: Ekte data på 5564**

Installer eit signert bygg med `install -r` (sjå `RELEASE_WORKFLOW.md`), opne Oppdag og kontroller:
radene, toppfeltet med fokus, «Dine førespurnader» med faner, ein tittel med Folk og Liknande, personsida,
og Be om-arket (Kvalitet og Lagringsstad viser seg berre med avanserte rettar). Ta skjermbilete til
verifikasjonsrapporten (berre lokalt). Ikkje send nokon førespurnad på den ekte kontoen.

- [ ] **Steg 3: Release**

Følg `docs/RELEASE_WORKFLOW.md` steg for steg med versjon `1.0.0-beta14` og kode 151: fullt bygg, arkiv,
signatur og hash, commit, tag, atomisk push av `HEAD:main` og taggen, kladd med fem assets, kontroll av
digestar, publisering, offentleg nedlasting, og oppdatering gjennom appen frå offisiell beta13 på 5566.

- [ ] **Steg 4: Commit av rapporten**

```bash
git add docs/VERIFICATION_v1.0.0-beta14.md && git commit -m "Document beta14 public update and green CI [skip ci]" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" && git push origin HEAD:main
```
