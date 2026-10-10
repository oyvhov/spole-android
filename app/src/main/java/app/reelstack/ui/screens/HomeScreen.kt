package app.reelstack.ui.screens

import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.isTelevision
import app.reelstack.ui.components.playableNow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import app.reelstack.ui.components.tvCardPress
import app.reelstack.ui.components.SpoleDropdownMenu as DropdownMenu
import app.reelstack.ui.components.SpoleDropdownMenuItem as DropdownMenuItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.res.stringResource
import app.reelstack.ui.theme.LocalTabletCanvas
import app.reelstack.ui.components.TabletLibraryFeature
import app.reelstack.ui.components.cinematicBleed
import app.reelstack.ui.components.tabletFeaturedTitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.HomeRowKey
import app.reelstack.data.model.HomeRowKind
import app.reelstack.data.model.homeRowFormat
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.isSeries
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.state.HomeUiState
import app.reelstack.ui.state.preferredHomeAccount
import app.reelstack.ui.state.toHomeUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.HomeSearchEntry
import app.reelstack.ui.components.IncomingSkeleton
import app.reelstack.ui.components.LibraryRailSkeleton
import app.reelstack.ui.components.AccountAvatarButton
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.components.UpcomingSkeleton
import app.reelstack.ui.components.RecommendationSkeleton
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.ReelPage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor
import app.reelstack.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
/**
 * `searchTransitionModifier` is not this screen's modifier and must not be called `modifier`.
 *
 * A screen fills the window and takes no modifier of its own; this one is threaded down to the
 * search field alone, so that the field can keep its identity while Home and Oppdag swap places.
 * Renaming it would make the signature claim something untrue, so the lint rule is answered here
 * rather than obeyed.
 */
@Suppress("ModifierParameter")
@Composable
fun HomeScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onSessionClick: (String) -> Unit,
    onPlaybackToggle: (String) -> Unit,
    onMediaClick: (String) -> Unit,
    onLibraryClick: (String) -> Unit,
    onUpcomingClick: (String) -> Unit,
    onCalendarClick: () -> Unit = {},
    onRefresh: () -> Unit,
    onAccountClick: () -> Unit = {},
    onDiscoverClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    searchTransitionModifier: Modifier = Modifier,
    showSearch: Boolean = true,
    showSearchIcon: Boolean = false,
    showBrand: Boolean = true,
    cardActions: MediaCardActions? = null,
) = HomeScreen(
    state = state.toHomeUiState(),
    contentPadding = contentPadding,
    onSessionClick = onSessionClick,
    onPlaybackToggle = onPlaybackToggle,
    onMediaClick = onMediaClick,
    onLibraryClick = onLibraryClick,
    onUpcomingClick = onUpcomingClick,
    onCalendarClick = onCalendarClick,
    onRefresh = onRefresh,
    onAccountClick = onAccountClick,
    onDiscoverClick = onDiscoverClick,
    onSearchClick = onSearchClick,
    searchTransitionModifier = searchTransitionModifier,
    showSearch = showSearch,
    showSearchIcon = showSearchIcon,
    showBrand = showBrand,
    cardActions = cardActions,
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onSessionClick: (String) -> Unit,
    onPlaybackToggle: (String) -> Unit,
    onMediaClick: (String) -> Unit,
    onLibraryClick: (String) -> Unit,
    onUpcomingClick: (String) -> Unit,
    onCalendarClick: () -> Unit = {},
    onRefresh: () -> Unit,
    onAccountClick: () -> Unit = {},
    onDiscoverClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    searchTransitionModifier: Modifier = Modifier,
    showSearch: Boolean = true,
    /** The search icon beside the profile: every touch layout, never television or a child. */
    showSearchIcon: Boolean = false,
    showBrand: Boolean = true,
    cardActions: MediaCardActions? = null,
) {
    var showTopRefreshIndicatorByUser by rememberSaveable { mutableStateOf(false) }
    val triggerRefresh = {
        if (!state.isRefreshing) {
            showTopRefreshIndicatorByUser = true
        }
        onRefresh()
    }
    LaunchedEffect(state.isRefreshing) {
        if (!state.isRefreshing) showTopRefreshIndicatorByUser = false
    }
    val showTopRefreshIndicator = state.isRefreshing && showTopRefreshIndicatorByUser
    val layout = state.homeLayout
    val configuredMediaSources = state.connections
        .filter { connection ->
            connection.baseUrl.isNotBlank() &&
                (connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        }
        // Each source becomes a keyed LazyColumn item, and duplicate keys crash the list.
        .map { it.kind }
        .distinct()
    val mediaSources = configuredMediaSources.ifEmpty {
        (state.recentMovies + state.recentSeries).map(LibraryMedia::source).distinct()
    }
    val hasCalendarConnection = state.connections.any {
        it.baseUrl.isNotBlank() && it.kind == ServiceKind.SEERR
    }
    // Recommendations, releases and the calendar arrive with the whole refresh. Until the first
    // one (or the cache) they wait in a skeleton; after that a refresh updates them in place.
    val feedFirstLoad = state.isRefreshing && state.lastUpdatedEpochMillis == null
    HomeRefreshFrame(
        isRefreshing = showTopRefreshIndicator,
        onRefresh = triggerRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
      ReelPage(media = true) {
        val personalization = app.reelstack.ui.theme.LocalPersonalization.current
        // Next up joins a server's "continue watching" only where both of that server's rows are on.
        fun combine(source: ServiceKind) = personalization.combineContinueWatching &&
            layout.isVisible(HomeRowKey(HomeRowKind.CONTINUE_WATCHING, source)) &&
            layout.isVisible(HomeRowKey(HomeRowKind.NEXT_UP, source))
        val combinedNextUp = state.nextUp.filter { combine(it.source) }
        val continueRows = app.reelstack.data.model.watchingBySource(state.resume, combinedNextUp)
        val nextRows = app.reelstack.data.model.watchingBySource(state.nextUp)
        val continueItems = if (combinedNextUp.isNotEmpty()) app.reelstack.data.model.combinedWatching(state.resume, combinedNextUp) else state.resume
        val watchingSources = (configuredMediaSources + continueRows.keys + nextRows.keys).distinct()
        val tablet = LocalTabletCanvas.current
        val edge = app.reelstack.ui.theme.LocalMediaEdgeToEdge.current
        val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
            android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        val featurePool = buildList {
            val lists = listOf(continueItems, state.nextUp, state.recentMovies, state.recentSeries)
            val iterators = lists.map { it.iterator() }
            while (iterators.any { it.hasNext() }) {
                for (iterator in iterators) {
                    if (iterator.hasNext()) add(iterator.next())
                }
            }
        }
        val features = if (personalization.showHero) tabletFeaturedTitles(featurePool, layout,
            allowLocalArtwork = state.configuredCount == 0) else emptyList()
        val featured = features.firstOrNull()
        val feedState = androidx.compose.foundation.lazy.rememberLazyListState()
        val feedScope = rememberCoroutineScope()
        val parallaxOffset by remember {
            androidx.compose.runtime.derivedStateOf {
                if (feedState.firstVisibleItemIndex == 0) {
                    feedState.firstVisibleItemScrollOffset * 0.42f
                } else 0f
            }
        }
        val featureVisible by remember { androidx.compose.runtime.derivedStateOf {
            feedState.layoutInfo.visibleItemsInfo.any { it.key == "tablet-feature" || it.key == "mobile-feature" }
        } }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides
                if (television) app.reelstack.ui.components.DetailBringIntoView
                else androidx.compose.foundation.gestures.LocalBringIntoViewSpec.current) {
        LazyColumn(
            state = feedState,
            contentPadding = PaddingValues(
                start = ReelLayout.Gutter,
                top = if (television && featured != null) 0.dp else if (!tablet && featured != null) 0.dp else ReelLayout.PageTop,
                end = if (edge) 0.dp else ReelLayout.Gutter,
                bottom = contentPadding.calculateBottomPadding() + 22.dp,
            ),
            modifier = Modifier.fillMaxSize().testTag("home-feed"),
        ) {
            if (featured == null) item(key = "home-header") {
                Box(Modifier.padding(end = if (edge) ReelLayout.Gutter else 0.dp)) {
                    HomeHeader(state, onAccountClick, showBrand, onSearchClick.takeIf { showSearchIcon })
                }
            }
            if (featured != null) {
                if (tablet) {
                    item(key = "tablet-feature") {
                        TabletLibraryFeature(featured, onLibraryClick,
                            Modifier.padding(bottom = 4.dp, end = if (edge) ReelLayout.Gutter else 0.dp)
                                .then(if (television) Modifier.cinematicBleed(ReelLayout.Gutter) else Modifier),
                            account = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (showSearchIcon) app.reelstack.ui.components.HomeSearchButton(onSearchClick, onArtwork = true)
                                    HomeAccountButton(state, onAccountClick, onArtwork = true)
                                }
                            },
                            candidates = features,
                            rotationEnabled = featureVisible && state.activeSheet == null,
                            onFocusWithin = { focused -> if (television && focused) feedScope.launch {
                                // Let the focus target finish its own bring-into-view request first.
                                androidx.compose.runtime.withFrameNanos { }
                                feedState.scrollToItem(0)
                            } },
                        )
                    }
                } else {
                    item(key = "mobile-feature") {
                        app.reelstack.ui.components.MobileLibraryFeature(
                            media = featured,
                            onOpen = onLibraryClick,
                            modifier = Modifier
                                .cinematicBleed(ReelLayout.Gutter),
                            header = {
                                HomeHeader(
                                    state = state,
                                    onAccountClick = onAccountClick,
                                    showBrand = showBrand,
                                    onSearchClick = onSearchClick.takeIf { showSearchIcon },
                                    onArtwork = true,
                                )
                            },
                            candidates = features,
                            rotationEnabled = featureVisible && state.activeSheet == null,
                            parallaxOffset = parallaxOffset,
                        )
                    }
                }
            }
            if (showSearch) item(key = "search-entry") {
                Box(Modifier.padding(top = 8.dp, end = mediaEndInset())) {
                    HomeSearchEntry(onSearchClick, searchTransitionModifier)
                }
            }
            // Claim the smaller gap only when an actual section is registered after the mobile hero.
            // Empty/hidden rows must not consume it, and optional search keeps its own spacing.
            var belowMobileHero = featured != null && !tablet && !showSearch
            fun nextSectionTop(regular: androidx.compose.ui.unit.Dp = ReelLayout.SectionTop): androidx.compose.ui.unit.Dp {
                val top = if (belowMobileHero) ReelLayout.MobileHeroSectionTop else regular
                belowMobileHero = false
                return top
            }
            layout.order.filter(layout::isVisible).forEach { row ->
                val source = row.source
                if (row.kind == HomeRowKind.NOW_PLAYING && state.sessions.isNotEmpty()) {
                    val sectionTop = nextSectionTop()
                    item(key = "now-playing") {
                            Row(Modifier.fillMaxWidth().padding(top = sectionTop, bottom = ReelLayout.SectionBottom, end = mediaEndInset()), verticalAlignment = Alignment.Bottom) {
                                SectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_now_playing), Modifier.weight(1f))
                                // A count is status, not an action, so it stays out of the accent colour.
                                if (state.sessions.size > 1) Text(androidx.compose.ui.res.pluralStringResource(app.reelstack.R.plurals.home_playback_count, state.sessions.size, state.sessions.size),
                                    color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
                            }
                            NowPlayingRail(
                                sessions = state.sessions,
                                pendingSessionKey = state.pendingSessionKey,
                                onOpen = onSessionClick,
                                onPlaybackToggle = onPlaybackToggle,
                            )
                    }
                }
                if (row.kind == HomeRowKind.CONTINUE_WATCHING && source != null && source in watchingSources) {
                    val items = continueRows[source].orEmpty()
                    val incomplete = source in state.failedServices || source in state.serviceWarnings
                    // Only a row that has never loaded waits in a skeleton. An empty row used to get
                    // a skeleton at every refresh and lose it again, and the page jumped twice.
                    val firstLoad = state.isRefreshing && state.configuredCount > 0 && source !in state.loadedSources
                    if (items.isNotEmpty() || incomplete || firstLoad) {
                        val sectionTop = nextSectionTop(if (television && featured != null) 14.dp else ReelLayout.SectionTop)
                        item(key = "continue-watching-${source.name}") {
                            Column(Modifier.testTag("continue-watching-${source.name}")) {
                                MediaSectionTitle(stringResource(if (combine(source)) R.string.tv_continue_combined else R.string.home_continue), source, Modifier.padding(
                                    top = sectionTop,
                                    bottom = ReelLayout.SectionBottom,
                                ))
                                if (items.isEmpty() && firstLoad) LibraryRailSkeleton(stringResource(R.string.home_loading_resume), wide = true, tabletArtwork = false)
                                else if (items.isEmpty()) EmptySectionLine(stringResource(R.string.home_resume_retry))
                                else ResumeRail(items, onLibraryClick, cardActions, rowKey = row.id)
                            }
                        }
                    }
                }
                // The payoff for the heart on every card. A row that is empty until somebody stars
                // something, and then keeps what they starred in one place, per server.
                if (row.kind == HomeRowKind.FAVOURITES && source != null) {
                    val items = state.favourites.filter { it.source == source }
                    if (items.isNotEmpty()) {
                      val sectionTop = nextSectionTop()
                      item(key = "favourites-${source.name}") {
                        MediaSectionTitle(stringResource(R.string.home_favourites), source,
                            Modifier.padding(top = sectionTop, bottom = ReelLayout.SectionBottom))
                        LibraryRail(items, onLibraryClick, wide = false, rowKey = row.id, actions = cardActions)
                      }
                    }
                }
                if (row.kind == HomeRowKind.NEXT_UP && source != null && !combine(source)) {
                    val items = nextRows[source].orEmpty()
                    if (items.isNotEmpty()) {
                      val sectionTop = nextSectionTop()
                      item(key = "next-up-${source.name}") {
                        Column(Modifier.testTag("next-up-${source.name}")) {
                            MediaSectionTitle(stringResource(R.string.tv_next_up), source, Modifier.padding(top = sectionTop, bottom = ReelLayout.SectionBottom))
                            // Next up has no resume point of its own; the other two writes still apply.
                            ResumeRail(items, onLibraryClick, cardActions.withoutResumeRemoval(), rowKey = row.id)
                        }
                      }
                    }
                }
                if (row.kind == HomeRowKind.NEW_MOVIES && source != null && source in mediaSources) {
                        val sectionTop = nextSectionTop()
                        item(key = "recent-movies-${source.name}") {
                            MediaSectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_new_movies), source, Modifier.padding(top = sectionTop, bottom = ReelLayout.SectionBottom))
                            val items = state.recentMovies.filter { it.source == source }
                            if (items.isEmpty()) {
                                if (state.isRefreshing && source !in state.loadedSources) {
                                    LibraryRailSkeleton(stringResource(R.string.home_loading_movies, source.displayName))
                                } else {
                                    EmptySectionLine(mediaEmptyMessage(state, source, stringResource(R.string.home_no_movies)))
                                }
                            } else {
                                LibraryRail(items, onLibraryClick, wide = false, rowKey = row.id, actions = cardActions)
                            }
                        }
                }
                if (row.kind == HomeRowKind.NEW_SERIES && source != null && source in mediaSources) {
                        val sectionTop = nextSectionTop()
                        item(key = "recent-series-${source.name}") {
                            MediaSectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_new_episodes), source, Modifier.padding(top = sectionTop, bottom = ReelLayout.SectionBottom))
                            val items = state.recentSeries.filter { it.source == source }
                            if (items.isEmpty()) {
                                if (state.isRefreshing && source !in state.loadedSources) {
                                    LibraryRailSkeleton(
                                        stringResource(R.string.home_loading_series, source.displayName),
                                        wide = true,
                                    )
                                } else {
                                    EmptySectionLine(mediaEmptyMessage(state, source, stringResource(R.string.home_no_episodes)))
                                }
                            } else {
                                LibraryRail(items, onLibraryClick, wide = true, rowKey = row.id, actions = cardActions)
                            }
                        }
                }
                if (row.kind == HomeRowKind.RECOMMENDATIONS) {
                    val sectionTop = nextSectionTop()
                    item(key = "recommendations") {
                        SectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_recommendations), Modifier.padding(top = sectionTop, bottom = 4.dp))
                        Text(stringResource(R.string.home_recommendations_note), color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                            modifier = Modifier.padding(bottom = 12.dp))
                        if (state.recommendations.isEmpty() && feedFirstLoad) {
                            RecommendationSkeleton()
                        } else if (state.recommendations.isEmpty()) {
                            EmptySectionLine(stringResource(R.string.home_recommendations_empty))
                        } else {
                            RecommendationRail(state.recommendations.take(8), onDiscoverClick)
                        }
                    }
                }
                if (row.kind == HomeRowKind.RECENT_RELEASES) {
                    val sectionTop = nextSectionTop()
                    item(key = "recent-releases") {
                        Column(Modifier.padding(top = sectionTop, bottom = ReelLayout.SectionBottom)) {
                            SectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_recent_releases))
                            Text(
                                stringResource(R.string.home_releases_note),
                                color = Muted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (state.recentReleases.isEmpty()) {
                            if (feedFirstLoad && state.configuredCount > 0) {
                                UpcomingSkeleton()
                            } else {
                                EmptySectionLine(state.recentReleasesError ?: stringResource(R.string.home_releases_empty))
                            }
                        } else {
                            RecentReleaseRail(state.recentReleases, onUpcomingClick)
                            state.recentReleasesError?.let { EmptySectionLine(it) }
                        }
                    }
                }
                if (row.kind == HomeRowKind.UPCOMING) {
                    val sectionTop = nextSectionTop()
                    item(key = "upcoming") {
                        UpcomingSectionTitle(
                            onCalendarClick = onCalendarClick,
                            modifier = Modifier.padding(top = sectionTop, bottom = ReelLayout.SectionBottom),
                        )
                        if (state.upcoming.isEmpty()) {
                            // Seerr supplies the personal calendar. Without it there is nothing to
                            // wait for, and a 226 dp skeleton used to collapse into one line.
                            if (feedFirstLoad && hasCalendarConnection) {
                                UpcomingSkeleton()
                            } else {
                                EmptySectionLine(state.upcomingError ?: if (!hasCalendarConnection)
                                    stringResource(R.string.home_calendar_disconnected)
                                else stringResource(R.string.home_upcoming_empty))
                            }
                        } else {
                            UpcomingRail(app.reelstack.data.model.calendarHomeItems(state.upcoming), onUpcomingClick)
                            state.upcomingError?.let { EmptySectionLine(it) }
                        }
                    }
                }
            }
            item(key = "freshness") { HomeFreshness(state, triggerRefresh) }
        }
        }
      }
    }
}

/**
 * Pull-to-refresh belongs to touch screens; a TV refresh never overlays its hero artwork.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeRefreshFrame(isRefreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    if (isTelevision()) Box(modifier, content = content)
    else PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier, content = content)
}

@Composable
private fun HomeFreshness(state: HomeUiState, onRefresh: () -> Unit) {
    if (state.configuredCount == 0) return
    val text = when {
        state.isRefreshing -> stringResource(R.string.home_refreshing)
        state.lastUpdatedEpochMillis != null -> stringResource(R.string.home_updated_at, Instant
            .ofEpochMilli(state.lastUpdatedEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm")))
        else -> stringResource(R.string.home_waiting_refresh)
    }
    app.reelstack.ui.components.FeedRefreshAction(text, state.isRefreshing, onRefresh,
        Modifier.fillMaxWidth().padding(top = 28.dp, end = mediaEndInset()))
}

@Composable
private fun HomeHeader(state: HomeUiState, onAccountClick: () -> Unit, showBrand: Boolean, onSearchClick: (() -> Unit)? = null, onArtwork: Boolean = false) {
    var appeared by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared || !app.reelstack.ui.theme.LocalMotionEnabled.current || isTelevision()) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "home-header-reveal",
    )
    Column {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().testTag("home-header").graphicsLayer {
            alpha = reveal
            translationY = (1f - reveal) * 24f
        },
    ) {
            if (showBrand) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                app.reelstack.ui.components.SpoleBrandMark(Modifier.size(34.dp), "Spole-logo")
                Text(app.reelstack.ui.theme.LocalPersonalization.current.appLabel, color = TextColor, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.7).sp, modifier = Modifier.padding(start = 8.dp))
            } else Spacer(Modifier.weight(1f))
        if (onSearchClick != null) app.reelstack.ui.components.HomeSearchButton(onSearchClick, onArtwork = onArtwork)
        HomeAccountButton(state, onAccountClick, onArtwork = onArtwork)
    }
    if (showBrand && !onArtwork) app.reelstack.ui.components.SeasonalThemeBanner(Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun HomeAccountButton(state: HomeUiState, onAccountClick: () -> Unit, onArtwork: Boolean = false) {
    val account = state.preferredHomeAccount()
    val connection = state.connections.firstOrNull { it.kind == account?.source }
    AccountAvatarButton(
            account = account,
            connection = connection,
            onClick = onAccountClick,
            description = account?.let { stringResource(R.string.home_open_account, it.displayName, it.source.displayName) }
                ?: stringResource(R.string.home_account_settings),
            testTag = "home-account",
            onArtwork = onArtwork,
        )
}

@Composable
private fun mediaEmptyMessage(state: HomeUiState, source: ServiceKind, emptyMessage: String): String =
    if (source in state.failedServices) {
        stringResource(R.string.home_source_failed, source.displayName)
    } else if (source in state.serviceWarnings) {
        stringResource(R.string.home_source_partial, source.displayName)
    } else {
        emptyMessage
    }

@Composable
private fun mediaEndInset() = if (app.reelstack.ui.theme.LocalMediaEdgeToEdge.current) ReelLayout.Gutter else 0.dp

/** One size for every section heading on Home. */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TextColor,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier,
    )
}

@Composable
internal fun MediaSectionTitle(title: String, source: ServiceKind, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier.fillMaxWidth().padding(end = mediaEndInset()).clearAndSetSemantics {
        heading()
        contentDescription = "$title · ${source.displayName}"
    }) {
        // The trailing source mark and label identify the shelf without repeating the source
        // in its heading. Accessibility keeps both in the one spoken description above.
        Text(
            text = title,
            color = TextColor,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        // Mark and label stay centred on each other; the pair sits on the heading's baseline.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
            ServiceLogo(
                kind = source,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = source.displayName,
                color = Muted,
                fontSize = 12.sp, lineHeight = 17.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

@Composable
private fun RecommendationRail(items: List<DiscoverMedia>, onClick: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = DiscoverMedia::id) { media ->
            RecommendationCard(media) { onClick(media.id) }
        }
    }
}

@Composable
private fun RecommendationCard(media: DiscoverMedia, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val status = if (media.seerrStatus == null && !media.inLibrary && !media.requested) stringResource(R.string.media_check_availability)
        else app.reelstack.localization.localizedSeerrStatus(media.seerrStatus, media.inLibrary, media.requested)
    Box(
        Modifier
            .width(164.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale)
            .heightIn(min = 258.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale)
            .clip(RoundedCornerShape(16.dp))
            .focusOutline(interaction, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = app.reelstack.ui.components.mediaCardIndication(),
                onClickLabel = stringResource(R.string.flow_detail_named, media.title), onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .testTag("recommendation-${media.id}"),
    ) {
        MediaArtwork(media.artworkUrl, media.title, Modifier.matchParentSize(), fallbackRes = media.artworkRes, ContentScale.Crop)
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .14f),
                    .42f to Color.Transparent,
                    .72f to Color.Black.copy(alpha = .72f),
                    1f to Color.Black.copy(alpha = .96f),
                ),
            ),
        )
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (media.isSeries) stringResource(R.string.media_series) else stringResource(R.string.media_movie),
                color = Color.White,
                fontSize = 10.sp, lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .6.sp,
                modifier = Modifier.background(Color.Black.copy(alpha = .66f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            if (media.inLibrary || media.requested || media.seerrStatus in 2..6) {
                Icon(
                    if (media.inLibrary || media.seerrStatus == 5) app.reelstack.ui.components.SpoleIcons.DoneCircle else app.reelstack.ui.components.SpoleIcons.CloudReady,
                    contentDescription = null,
                    tint = app.reelstack.ui.theme.Success,
                    modifier = Modifier.background(Color.Black.copy(alpha = .72f), CircleShape).padding(6.dp).size(18.dp),
                )
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 54.dp)) {
            Text(media.metadata, color = Color.White.copy(alpha = .78f), fontSize = 11.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(media.title, color = Color.White, fontSize = 17.sp, lineHeight = 20.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp))
            Text(status, color = PrimarySoft, fontSize = 11.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun NowPlayingRail(
    sessions: List<PlaybackSession>,
    pendingSessionKey: String?,
    onOpen: (String) -> Unit,
    onPlaybackToggle: (String) -> Unit,
) {
    if (LocalTabletCanvas.current) {
        // A cover and two readable title lines share one frame on every source.
        val width = 360.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
        LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sessions, key = PlaybackSession::key) { session ->
                app.reelstack.ui.components.CompactSessionCard(session, pendingSessionKey == session.key,
                    controlsLocked = pendingSessionKey != null, onOpen = { onOpen(session.key) },
                    onToggle = { onPlaybackToggle(session.key) }, modifier = Modifier.width(width))
            }
        }
        return
    }
    if (sessions.size == 1) {
        val session = sessions.single()
        NowPlayingCard(
            session = session,
            pending = pendingSessionKey == session.key,
            controlsLocked = pendingSessionKey != null,
            onOpen = { onOpen(session.key) },
            onPlaybackToggle = { onPlaybackToggle(session.key) },
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(sessions, key = PlaybackSession::key) { session ->
            NowPlayingCard(
                session = session,
                pending = pendingSessionKey == session.key,
                controlsLocked = pendingSessionKey != null,
                onOpen = { onOpen(session.key) },
                onPlaybackToggle = { onPlaybackToggle(session.key) },
                modifier = Modifier.width(316.dp),
            )
        }
    }
}

@Composable
private fun NowPlayingCard(
    session: PlaybackSession,
    pending: Boolean,
    controlsLocked: Boolean,
    onOpen: () -> Unit,
    onPlaybackToggle: () -> Unit,
    modifier: Modifier,
) = app.reelstack.ui.components.CompactSessionCard(session, pending, controlsLocked, onOpen, onPlaybackToggle, modifier)

@Composable
internal fun LibraryRail(items: List<LibraryMedia>, onClick: (String) -> Unit, wide: Boolean, rowKey: String? = null, actions: MediaCardActions? = null,
    libraryDisplay: app.reelstack.data.model.LibraryDisplay? = null) {
    val chosenWide = if (libraryDisplay != null) when (libraryDisplay.artType) {
        app.reelstack.data.model.LibraryArtType.AUTO -> wide
        app.reelstack.data.model.LibraryArtType.POSTER -> false
        else -> true
    } else when (homeRowFormat(app.reelstack.ui.theme.LocalPersonalization.current.homeRowFormats, rowKey)) {
        "POSTER" -> false; "THUMB" -> true; else -> wide
    }
    // TV cards keep two title lines on every shelf, so long titles stay readable and captions
    // below neighbouring cards share a baseline. Touch shelves still use their compact layout.
    val titleLines = if (isTelevision()) 2 else if (items.any { it.title.length > if (chosenWide) 26 else 15 }) 2 else 1
    val railState = androidx.compose.foundation.lazy.rememberLazyListState()
    app.reelstack.ui.components.PrefetchRailArtwork(items, railState, wide = chosenWide, libraryDisplay = libraryDisplay)
    LazyRow(state = railState, modifier = Modifier.fillMaxWidth().testTag("library-rail"), contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> media.id }) { index, media ->
            LibraryCard(
                media = media,
                wide = chosenWide,
                titleLines = titleLines,
                revealDelay = (index.coerceAtMost(2) * 30),
                actions = actions?.copy(canRemoveFromResume = false),
                libraryDisplay = libraryDisplay,
                onClick = { onClick(media.id) },
            )
        }
    }
}

/**
 * Partly watched titles.
 *
 * Every card in the rail gets the same frame. Letting each card keep the shape of its own artwork
 * read well on paper — a film stays a poster, an episode stays a wide still — but a shelf holding
 * both came out with cards nearly three times wider than their neighbours, and a row with no shared
 * edge reads as a bug rather than as a choice. The shelf picks one shape; artwork that faces the
 * other way is fitted into it with a blurred copy of itself behind.
 */
@Composable
internal fun ResumeRail(items: List<LibraryMedia>, onClick: (String) -> Unit, actions: MediaCardActions? = null, rowKey: String? = null,
    resumeIds: Set<String>? = null, compact: Boolean = false) {
    val format = homeRowFormat(app.reelstack.ui.theme.LocalPersonalization.current.homeRowFormats, rowKey)
    val chosenWide = resumeRailIsWide(format)
    val titleLines = 2
    val density = androidx.compose.ui.platform.LocalDensity.current
    val window = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val wideWindow = with(density) {
        app.reelstack.ui.layout.WindowLayoutPolicy(window.width.toDp().value, window.height.toDp().value).useTabletCanvas
    }
    val inline = compact && (isTelevision() || LocalTabletCanvas.current || wideWindow) && density.fontScale < 1.6f
    val railState = androidx.compose.foundation.lazy.rememberLazyListState()
    app.reelstack.ui.components.PrefetchRailArtwork(items, railState, wide = chosenWide)
    val nextActions = actions.withoutResumeRemoval()
    LazyRow(state = railState, contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> "resume-${media.id}" }) { index, media ->
            ResumeCard(media, titleLines, revealDelay = index.coerceAtMost(2) * 30,
                actions = if (resumeIds == null || media.id in resumeIds) actions else nextActions,
                wide = chosenWide, showSeriesYear = resumeIds == null, compact = inline) { onClick(media.id) }
        }
    }
}

/**
 * The one shape a resume shelf uses, decided before the first card is laid out.
 *
 * An explicit per-row choice wins; `AUTO` is always the wide still. A first attempt let the majority
 * media type decide, and on a real Emby shelf — five part-watched films, four part-watched episodes
 * — the films won and every episode still ended up letterboxed into a tall poster frame with grey
 * bands above and below it. The reverse costs far less: a poster inside a wide frame is a small
 * picture with a blurred edge, which is what the Jellyfin shelf already looked like and read fine.
 * It is also the shape the shelf is *for* — a frame from where you stopped, with a progress bar
 * along the bottom — and the shape every television interface uses for it.
 */
/**
 * The picture to ask for, given the shape of the frame it has to fill.
 *
 * A wide frame wants the backdrop or the still; a tall frame wants the poster. `artworkUrl` is
 * whatever the feed picked as the item's main image, and for a film that is the poster — which is
 * why the resume shelf, the one shelf that is always wide, was showing every film as a 2:3 poster
 * with a blurred copy filling the sides. It asked for the wide frame and then fetched the poster.
 *
 * The fallback still matters: a title with no backdrop on the server has nothing else to show, and
 * a fitted poster is better than an empty frame.
 */
internal fun railArtworkUrl(
    wide: Boolean,
    heroUrl: String?,
    posterUrl: String?,
    artworkUrl: String?,
    isEpisode: Boolean = false,
): String? {
    if (!wide) return posterUrl ?: artworkUrl
    // A wide rail must use the server's wide artwork first. Episodes used to prefer their
    // Primary still here, even when heroUrl was a real series Thumb/Backdrop; that left a
    // portrait fallback in RailArtwork, where it was fitted as a tiny picture in the middle.
    heroUrl?.takeIf { it.isNotBlank() }?.let { return railSizedHero(it) }
    if (isEpisode) {
        val isFallbackToPoster = artworkUrl != null && posterUrl != null && artworkUrl == posterUrl
        if (!isFallbackToPoster && artworkUrl != null) return artworkUrl
    }
    return artworkUrl ?: posterUrl
}

/**
 * Hero art is requested at 1920 px for the full-width feature. A rail card is at most about 300 dp
 * wide, so a row of them downloaded three times the pixels it could show.
 */
internal fun railSizedHero(url: String): String = url.replace(HERO_IMAGE_SIZE, RAIL_IMAGE_SIZE)

private const val HERO_IMAGE_SIZE = "maxWidth=1920&quality=90"
private const val RAIL_IMAGE_SIZE = "maxWidth=1080&quality=85"

internal fun resumeRailIsWide(format: String?): Boolean = format != "POSTER"

/**
 * The day word on a "Kjem snart" card, decided against the clock rather than stored with the row.
 *
 * The sync writes [UpcomingMedia.dateLabel] once and the snapshot store keeps it, so a label that
 * said "I dag" when it was written still said "I dag" the next morning — which is why the day word
 * left the repository. It belongs here, where the clock is actually read. Anything the label
 * already carries after the separator is an airtime and is kept, so an episode row still reads
 * "I morgon · 21:00" rather than losing the time along with the date.
 *
 * A row with no usable timestamp keeps the date it was given; inventing "today" for an unknown
 * date is the one outcome worse than a stale one.
 */
internal fun upcomingDayLabel(
    dateLabel: String,
    airDateEpochMillis: Long,
    today: LocalDate,
    zone: ZoneId,
    todayWord: String,
    tomorrowWord: String,
): String {
    if (airDateEpochMillis <= 0L) return dateLabel
    val date = Instant.ofEpochMilli(airDateEpochMillis).atZone(zone).toLocalDate()
    val word = when (date) {
        today -> todayWord
        today.plusDays(1) -> tomorrowWord
        else -> return dateLabel
    }
    val time = dateLabel.substringAfter(" · ", "")
    return if (time.isBlank()) word else "$word · $time"
}

/**
 * The three things a card can do besides open.
 *
 * Passed as one object because a rail either offers all of them or none: a card with "favourite"
 * but no way to clear a stalled resume point is the half-measure that sent people back to the
 * Jellyfin app in the first place.
 */
/**
 * The same three choices, minus the one that does not apply to this shelf.
 *
 * Remembered on purpose. `copy` allocates, and a fresh actions object on every recomposition makes
 * every card in the rail below recompose with it — which defeats the point of the data class being
 * stable at all. Three shelves were doing exactly that.
 */
@Composable
internal fun MediaCardActions?.withoutResumeRemoval(): MediaCardActions? =
    remember(this) { this?.copy(canRemoveFromResume = false) }

data class MediaCardActions(
    val onRemoveFromResume: (LibraryMedia) -> Unit,
    val onFavourite: (LibraryMedia, Boolean) -> Unit,
    val onPlayed: (LibraryMedia, Boolean) -> Unit,
    /** Off for shelves where a resume point is not what the card represents. */
    val canRemoveFromResume: Boolean = true,
)

@Composable
private fun ResumeCard(media: LibraryMedia, titleLines: Int, revealDelay: Int,
    actions: MediaCardActions? = null, wide: Boolean, showSeriesYear: Boolean = true,
    compact: Boolean = false, onClick: () -> Unit) {
    val artworkHeight = (if (compact) 72.dp else ReelLayout.EpisodeHeight) *
        app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
    val frameRatio = if (wide) 16f / 9f else 2f / 3f
    val cardWidth = if (compact) 340.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
        else artworkHeight * frameRatio
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val scale = app.reelstack.ui.components.focusScale(focused, pressed)
    var appeared by rememberSaveable(media.id) { mutableStateOf(false) }
    LaunchedEffect(media.id) { appeared = true }
    val reveal by animateFloatAsState(
        if (appeared || !app.reelstack.ui.theme.LocalMotionEnabled.current || isTelevision()) 1f else 0f,
        tween(durationMillis = 240, delayMillis = revealDelay), label = "resume-card-reveal")
    var menuOpen by remember(media.id) { mutableStateOf(false) }
    val percent = ((media.progress ?: 0f).coerceIn(0f, 1f) * 100).toInt()
    val resumeLabel = if (percent > 0) stringResource(R.string.home_resume_description, media.title, percent)
        else "${media.title}, ${media.subtitle}"
    Column(Modifier.width(cardWidth).zIndex(if (focused) 10f else 0f)
        .graphicsLayer { alpha = reveal; translationY = (1f - reveal) * 30f; scaleX = scale; scaleY = scale }
        .tvCardPress(onClick, actions?.let { { menuOpen = true } })
        .combinedClickable(interactionSource = interactionSource,
            indication = app.reelstack.ui.components.mediaCardIndication(), onClick = onClick,
            onClickLabel = resumeLabel, onLongClick = actions?.let { { menuOpen = true } },
            onLongClickLabel = stringResource(R.string.library_card_options, media.title))
        .semantics { contentDescription = resumeLabel; role = Role.Button }
        .testTag("resume-card-${media.id}")) {
        if (actions != null) MediaCardMenu(media, actions, menuOpen, onDetails = onClick) { menuOpen = false }
        val artwork: @Composable () -> Unit = {
            val isEpisode = media.mediaType.equals("Episode", true) || (media.season != null && media.episode != null)
            val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
            Box(Modifier.fillMaxWidth().height(artworkHeight).clip(shape).focusOutline(interactionSource, shape)) {
                app.reelstack.ui.components.RailArtwork(
                    url = railArtworkUrl(wide, media.heroUrl, media.posterUrl, media.artworkUrl, isEpisode),
                    contentDescription = null, frameRatio = frameRatio, fallbackRes = media.artworkRes,
                    source = media.source, modifier = Modifier.fillMaxSize(), fitMismatched = false)
                if (percent > 0) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp)
                    .background(Color.Black.copy(alpha = .55f))) {
                    Box(Modifier.fillMaxWidth((media.progress ?: 0f).coerceIn(0f, 1f)).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface))
                }
            }
        }
        val caption: @Composable () -> Unit = {
            Text(media.title, color = TextColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 19.sp, minLines = titleLines, maxLines = titleLines,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("resume-title-${media.id}"))
            if (showSeriesYear || !media.mediaType.equals("Series", true)) Text(
                app.reelstack.ui.components.episodeLine(media.season, media.episode, media.subtitle),
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        if (compact) Row(Modifier.testTag("compact-resume-${media.id}"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(artworkHeight * frameRatio)) { artwork() }
            Column(Modifier.weight(1f)) { caption() }
        } else {
            artwork()
            Column(Modifier.padding(top = 9.dp)) { caption() }
        }
    }
}

/**
 * What holding a card offers.
 *
 * Written as the opposite of what is already true, so the menu states the outcome rather than the
 * setting: an unwatched title offers "Mark as watched", a favourite offers to stop being one.
 */
@Composable
private fun MediaCardMenu(media: LibraryMedia, actions: MediaCardActions, open: Boolean, onDetails: () -> Unit, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (open) app.reelstack.ui.components.SpoleChoiceDialog(media.title, onDismiss, icon = app.reelstack.ui.components.SpoleIcons.Movie) {
      Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Emby gained a player in 0.17.0-beta04; this menu kept asking for Jellyfin. The condition
        // now lives in one place so the next service to gain one is a single edit.
        if (media.playableNow()) DropdownMenuItem(
            text = { Text(stringResource(if ((media.progress ?: 0f) > 0f) R.string.tv_resume else R.string.phase_quick_play)) },
            leadingIcon = { Icon(app.reelstack.ui.components.SpoleIcons.PlaySimple, null) },
            onClick = { onDismiss(); app.reelstack.player.JellyfinPlayerActivity.open(context, media.remoteId.orEmpty(), source = media.source) },
            modifier = Modifier.testTag("card-play"))
        DropdownMenuItem(text = { Text(stringResource(R.string.phase_quick_details)) },
            leadingIcon = { Icon(app.reelstack.ui.components.SpoleIcons.Library, null) },
            onClick = { onDismiss(); onDetails() }, modifier = Modifier.testTag("card-details"))
        if (actions.canRemoveFromResume) DropdownMenuItem(
            text = { Text(stringResource(R.string.library_remove_resume)) },
            leadingIcon = { Icon(app.reelstack.ui.components.SpoleIcons.Close, null) },
            onClick = { onDismiss(); actions.onRemoveFromResume(media) },
            modifier = Modifier.testTag("card-remove-resume"),
        )
        DropdownMenuItem(
            text = { Text(stringResource(if (media.favourite) R.string.library_favourite_remove else R.string.library_favourite_add)) },
            leadingIcon = { Icon(app.reelstack.ui.components.SpoleIcons.Heart, null) },
            onClick = { onDismiss(); actions.onFavourite(media, !media.favourite) },
            modifier = Modifier.testTag("card-favourite"),
        )
        DropdownMenuItem(
            text = { Text(stringResource(if (media.played) R.string.library_played_unmark else R.string.library_played_mark)) },
            leadingIcon = { Icon(app.reelstack.ui.components.SpoleIcons.DoneCircle, null) },
            onClick = { onDismiss(); actions.onPlayed(media, !media.played) },
            modifier = Modifier.testTag("card-played"),
        )
      }
    }
}

@Composable
private fun LibraryCard(media: LibraryMedia, wide: Boolean, titleLines: Int, revealDelay: Int, actions: MediaCardActions? = null,
    libraryDisplay: app.reelstack.data.model.LibraryDisplay? = null, onClick: () -> Unit) {
    var menuOpen by remember(media.id) { mutableStateOf(false) }
    val tablet = LocalTabletCanvas.current
    val cardWidth = (if (wide) { if (tablet) 292.dp else ReelLayout.EpisodeWidth } else { if (tablet) 158.dp else ReelLayout.PosterWidth }) *
        app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale * (libraryDisplay?.size?.scale ?: 1f)
    val artType = libraryDisplay?.artType?.takeIf { it != app.reelstack.data.model.LibraryArtType.AUTO }
        ?: if (wide) app.reelstack.data.model.LibraryArtType.THUMB else app.reelstack.data.model.LibraryArtType.POSTER
    val frameRatio = artType.ratio
    val artworkHeight = cardWidth / frameRatio
    val density = androidx.compose.ui.platform.LocalDensity.current
    val widthPx = with(density) { cardWidth.roundToPx() }
    val requestSize = androidx.compose.ui.unit.IntSize(widthPx, (widthPx / frameRatio).toInt().coerceAtLeast(1))
    val artworkShape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    val cardLabel = listOf(media.title, media.subtitle).filter(String::isNotBlank).joinToString(", ")
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    var appeared by rememberSaveable(media.id) { mutableStateOf(false) }
    LaunchedEffect(media.id) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared || !app.reelstack.ui.theme.LocalMotionEnabled.current || isTelevision()) 1f else 0f,
        animationSpec = tween(durationMillis = 240, delayMillis = revealDelay),
        label = "library-card-reveal",
    )
    val scale = app.reelstack.ui.components.focusScale(focused, pressed)
    Column(
        modifier = Modifier
            .width(cardWidth)
            .zIndex(if (focused) 10f else 0f)
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 30f
                scaleX = scale
                scaleY = scale
            }
            .tvCardPress(onClick, actions?.let { { menuOpen = true } })
            .combinedClickable(
                interactionSource = interactionSource,
                indication = app.reelstack.ui.components.mediaCardIndication(),
                onClick = onClick,
                onClickLabel = media.title,
                onLongClick = actions?.let { { menuOpen = true } },
                onLongClickLabel = stringResource(R.string.library_card_options, media.title),
            )
            // See ResumeCard on why this is a plain `semantics` block.
            .semantics { contentDescription = cardLabel; role = Role.Button },
    ) {
        val isEpisode = media.mediaType.equals("Episode", ignoreCase = true) || (media.season != null && media.episode != null)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(artworkHeight)
                .testTag("library-artwork-${media.id}")
                .clip(artworkShape).focusOutline(interactionSource, artworkShape),
        ) {
            val image = if (libraryDisplay == null) railArtworkUrl(wide, media.heroUrl, media.posterUrl, media.artworkUrl, isEpisode = isEpisode)
                else app.reelstack.data.model.librarySizedArtwork(app.reelstack.data.model.libraryArtworkUrl(media, artType), widthPx)
            if (libraryDisplay != null && artType == app.reelstack.data.model.LibraryArtType.LOGO && media.logoUrl != null)
                app.reelstack.ui.components.MediaArtwork(image, null, Modifier.fillMaxSize().padding(12.dp), source = media.source,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit, requestSize = requestSize)
            else app.reelstack.ui.components.RailArtwork(
                url = image,
                contentDescription = null,
                frameRatio = frameRatio,
                fallbackRes = media.artworkRes,
                source = media.source,
                modifier = Modifier.fillMaxSize(),
                requestSize = requestSize.takeIf { libraryDisplay != null },
            )
            if (actions != null) MediaCardMenu(media, actions, menuOpen, onDetails = onClick) { menuOpen = false }
            libraryDisplay?.let { display ->
                app.reelstack.ui.components.LibraryCardStatus(media.id, media.mediaType, media.played, media.unplayedItemCount,
                    display, Modifier.align(Alignment.TopEnd).padding(6.dp))
                if (display.showRatings) app.reelstack.data.model.communityRatingLabel(media.facts)?.let { rating ->
                    app.reelstack.ui.components.LibraryRating(rating, Modifier.align(Alignment.BottomEnd).padding(6.dp))
                }
            }
        }
        if (libraryDisplay?.showTitles != false) {
        Text(
            media.title,
            color = TextColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            maxLines = titleLines,
            overflow = TextOverflow.Ellipsis,
            // Library captions sit directly below the image, without a reserved empty line.
            minLines = if (libraryDisplay == null) titleLines else 1,
            modifier = Modifier.padding(top = 9.dp),
        )
        if (libraryDisplay == null || !media.mediaType.equals("Series", true)) Text(
            app.reelstack.ui.components.episodeLine(media.season, media.episode, media.subtitle),
            color = Muted,
            fontSize = 12.sp,
            maxLines = if (wide) 2 else 1,
            lineHeight = 17.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        }
    }
}

@Composable
private fun UpcomingRail(items: List<UpcomingMedia>, onClick: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> media.id }) { index, media ->
            UpcomingCard(media, revealDelay = index.coerceAtMost(2) * 30) { onClick(media.id) }
        }
    }
}

@Composable
private fun RecentReleaseRail(items: List<UpcomingMedia>, onClick: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> media.id }) { index, media ->
            UpcomingCard(media, revealDelay = index.coerceAtMost(2) * 30, recent = true) { onClick(media.id) }
        }
    }
}

@Composable
private fun UpcomingCard(media: UpcomingMedia, revealDelay: Int, recent: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var appeared by rememberSaveable(media.id) { mutableStateOf(false) }
    LaunchedEffect(media.id) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared || !app.reelstack.ui.theme.LocalMotionEnabled.current || isTelevision()) 1f else 0f,
        animationSpec = tween(durationMillis = 240, delayMillis = revealDelay),
        label = "upcoming-card-reveal",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(stiffness = 460f, dampingRatio = 0.7f),
        label = "upcoming-card-press",
    )
    Box(Modifier.width(280.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale).heightIn(min = 226.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale).graphicsLayer {
        alpha = reveal; translationY = (1f - reveal) * 18f; scaleX = scale; scaleY = scale
    }.clip(shape).background(SurfaceRaised).focusOutline(interactionSource, shape)
        .clickable(interactionSource = interactionSource, indication = app.reelstack.ui.components.mediaCardIndication(), onClick = onClick)
        .semantics(mergeDescendants = true) { role = Role.Button }.testTag("upcoming-cover-${media.id}")) {
        MediaArtwork(media.artworkUrl, null, Modifier.matchParentSize(), fallbackRes = media.artworkRes, ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .12f), .35f to Color.Transparent,
            .7f to Color.Black.copy(alpha = .62f), 1f to Color.Black.copy(alpha = .94f))))
        Row(Modifier.align(Alignment.TopStart).padding(14.dp).background(Color.Black.copy(alpha = .76f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(app.reelstack.ui.components.SpoleIcons.Clock, null, tint = Primary, modifier = Modifier.size(14.dp))
            Text(upcomingDayLabel(media.dateLabel, media.airDateEpochMillis, LocalDate.now(), ZoneId.systemDefault(),
                stringResource(R.string.calendar_today), stringResource(R.string.calendar_tomorrow)),
                color = Color.White, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 70.dp)) {
            Text(if (recent) {
                if (media.mediaType.equals("Movie", true)) stringResource(R.string.media_new_movie) else stringResource(R.string.media_new_episode)
            } else if (media.mediaType.equals("Movie", true)) stringResource(R.string.media_home_release) else stringResource(R.string.media_new_episode),
                color = Color.White.copy(alpha = .8f), fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(media.title, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            Text(media.subtitle.replace(" · TBA", ""), color = Color.White.copy(alpha = .85f),
                fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

/**
 * Title and calendar shortcut sit on one line; the explanatory line runs full width underneath so
 * it is not squeezed into a half-width column that wraps at the default font size.
 */
@Composable
private fun UpcomingSectionTitle(onCalendarClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(end = mediaEndInset())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_upcoming), Modifier.weight(1f).padding(end = 12.dp))
            Surface(
                onClick = onCalendarClick,
                color = Primary.copy(alpha = 0.18f),
                contentColor = PrimarySoft,
                shape = CircleShape,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(app.reelstack.ui.components.SpoleIcons.Calendar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_calendar), fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        Text(
            stringResource(R.string.home_upcoming_note),
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EmptySectionLine(text: String) {
    Text(text, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, end = mediaEndInset()))
}

@Composable
private fun IncomingRow(media: IncomingMedia, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        MediaArtwork(
            url = media.artworkUrl,
            fallbackRes = media.artworkRes,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 66.dp, height = 82.dp).clip(RoundedCornerShape(12.dp)),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(media.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
            // Source is provenance, not an action, so it stays neutral.
            Text(media.source.displayName, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                Icon(
                    if (media.state == IncomingState.DOWNLOADING) app.reelstack.ui.components.SpoleIcons.Download else app.reelstack.ui.components.SpoleIcons.Clock,
                    contentDescription = null,
                    // Waiting in a queue is a normal state, not a problem the user must act on.
                    tint = if (media.state == IncomingState.DOWNLOADING) Primary else Muted,
                    modifier = Modifier.size(18.dp),
                )
                Text(media.status, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(start = 6.dp))
            }
            media.progress?.let { progress ->
                Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp).clip(CircleShape).background(SurfaceRaised)) {
                    Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0f, 1f)).height(4.dp).background(Primary))
                }
            }
        }
        Icon(app.reelstack.ui.components.SpoleIcons.ChevronRight, contentDescription = null, tint = app.reelstack.ui.theme.Muted, modifier = Modifier.size(17.dp))
    }
}
