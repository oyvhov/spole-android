package app.reelstack.data.repository

import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.requiredMenu
import app.reelstack.data.model.decodeHomeSections
import kotlinx.serialization.json.*

class AppPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)

    var preferredLibrarySource: app.reelstack.data.model.ServiceKind
        get() = app.reelstack.data.model.ServiceKind.entries.firstOrNull {
            it.name == preferences.getString("preferred_library_source", null) &&
                it in setOf(app.reelstack.data.model.ServiceKind.JELLYFIN, app.reelstack.data.model.ServiceKind.EMBY)
        } ?: app.reelstack.data.model.ServiceKind.JELLYFIN
        set(value) {
            require(value in setOf(app.reelstack.data.model.ServiceKind.JELLYFIN, app.reelstack.data.model.ServiceKind.EMBY))
            preferences.edit { putString("preferred_library_source", value.name) }
        }

    /** Read once: the device does not become a television while the app is running. */
    private val television: Boolean =
        (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

    /**
     * View, card size and image type per library, the way Jellyfin keeps them. Stored by library id
     * so a film library and a recordings library can differ, and read back with defaults when the
     * library is new.
     */
    fun libraryDisplay(libraryId: String): app.reelstack.data.model.LibraryDisplay =
        app.reelstack.data.model.LibraryDisplay.decode(
            preferences.getString(displayKey(libraryId), null),
        )

    fun setLibraryDisplay(libraryId: String, display: app.reelstack.data.model.LibraryDisplay) {
        if (libraryId.isBlank()) return
        preferences.edit { putString(displayKey(libraryId), display.encode()) }
    }

    private fun displayKey(libraryId: String) = "library_display." + libraryId

    var personalization: app.reelstack.data.model.Personalization
        get() = app.reelstack.data.model.Personalization(
            appLabel = preferences.getString("app_label", null)?.trim()?.take(24).orEmpty().ifBlank { "Spole" },
            accent = app.reelstack.data.model.AccentPalette.decode(preferences.getString("accent_palette", null)),
            artworkSize = app.reelstack.data.model.ArtworkSize.decode(preferences.getString("artwork_size", null)),
            autoResume = preferences.getBoolean("auto_resume", true),
            showNextEpisode = preferences.getBoolean("show_next_episode", true),
            nextEpisodeLeadSeconds = preferences.getInt("next_episode_lead", 15).coerceIn(0, 300),
            autoPlayNextEpisode = preferences.getBoolean("auto_play_next_episode", true),
            nextEpisodeDelaySeconds = preferences.getInt("next_episode_delay", 12).coerceIn(5, 60),
            showPlaybackModeInOsd = preferences.getBoolean("show_playback_mode_in_osd", true),
            lightweightTv = preferences.getBoolean("lightweight_tv", false),
            hideTvSidebar = preferences.getBoolean("hide_tv_sidebar", false),
            sidebarExpanded = if (preferences.contains("sidebar_expanded")) preferences.getBoolean("sidebar_expanded", true) else null,
            menuOrder = preferences.getString("menu_order", null)?.split(',') ?: app.reelstack.data.model.DEFAULT_MENU,
            hiddenMenuItems = preferences.getStringSet("menu_hidden", emptySet()).orEmpty().toSet(),
            showNextUp = preferences.getBoolean("show_next_up", true),
            combineContinueWatching = preferences.getBoolean("combine_continue", false),
            showHero = preferences.getBoolean("show_hero", true),
            heroRotate = preferences.getBoolean("hero_rotate", true),
            heroLogo = preferences.getBoolean("hero_logo", true),
            heroCompact = preferences.getBoolean("hero_compact", false),
            startInLibrary = preferences.getBoolean("start_in_library", false),
            libraryHub = preferences.getBoolean("library_hub", true),
            showLibraryTitle = preferences.getBoolean("library_title", false),
            libraryCardsWide = preferences.getBoolean("library_cards_wide", true),
            libraryHubOrder = preferences.getString("library_hub_order", null)?.split(',') ?: app.reelstack.data.model.DEFAULT_LIBRARY_HUB,
            libraryHubHidden = preferences.getStringSet("library_hub_hidden", emptySet()).orEmpty().toSet(),
            libraryOrder = preferences.getString("library_order", "")!!.split(',').filter(String::isNotBlank),
            libraryHidden = preferences.getStringSet("library_hidden", emptySet()).orEmpty().toSet(),
            showUpcomingEpisodes = preferences.getBoolean("show_upcoming_episodes", true),
            reduceMotion = preferences.getBoolean("reduce_motion", false),
            homeRowFormats = preferences.getString("home_row_formats", "").orEmpty().split(',').mapNotNull {
                val pair = it.split('='); if (pair.size == 2) pair[0] to pair[1] else null
            }.toMap(),
            showRatings = preferences.getBoolean("show_ratings", true),
            showQuality = preferences.getBoolean("show_quality", true),
            detailBackdrop = preferences.getBoolean("detail_backdrop", true),
            slowStartup = preferences.getBoolean("slow_startup", false),
            visualTheme = app.reelstack.data.model.VisualTheme.decode(preferences.getString("visual_theme", null)),
            artworkCorners = app.reelstack.data.model.ArtworkCorners.decode(preferences.getString("artwork_corners", null)),
            focusStyle = app.reelstack.data.model.FocusStyle.decode(preferences.getString("focus_style", null), television),
            highContrast = preferences.getBoolean("high_contrast", false),
            seasonalOrnament = preferences.getBoolean("seasonal_ornament", true),
            showLibraryCardNames = preferences.getBoolean("library_card_names", true),
            watchNextEnabled = preferences.getBoolean("watch_next_enabled", false),
            subtitleStyle = app.reelstack.data.model.SubtitleStyle.entries.firstOrNull { it.name == preferences.getString("subtitle_style", null) } ?: app.reelstack.data.model.SubtitleStyle.CLEAN,
            preferredSubtitleLanguage = app.reelstack.data.model.SubtitleLanguage.decode(preferences.getString("subtitle_language", null), app.reelstack.data.model.SubtitleLanguage.NORWEGIAN),
            fallbackSubtitleLanguage = app.reelstack.data.model.SubtitleLanguage.decode(preferences.getString("subtitle_fallback", null), app.reelstack.data.model.SubtitleLanguage.ENGLISH),
        )
        set(value) = preferences.edit {
            putString("app_label", value.appLabel.trim().take(24).ifBlank { "Spole" })
            putString("accent_palette", value.accent.name)
            putString("artwork_size", value.artworkSize.name)
            putBoolean("auto_resume", value.autoResume)
            putBoolean("show_next_episode", value.showNextEpisode)
            putInt("next_episode_lead", value.nextEpisodeLeadSeconds.coerceIn(0, 300))
            putBoolean("auto_play_next_episode", value.autoPlayNextEpisode)
            putInt("next_episode_delay", value.nextEpisodeDelaySeconds.coerceIn(5, 60))
            putBoolean("show_playback_mode_in_osd", value.showPlaybackModeInOsd)
            putBoolean("lightweight_tv", value.lightweightTv)
            putBoolean("hide_tv_sidebar", value.hideTvSidebar)
            value.sidebarExpanded?.let { putBoolean("sidebar_expanded", it) } ?: remove("sidebar_expanded")
            putString("menu_order", value.menuOrder.joinToString(","))
            putStringSet("menu_hidden", value.hiddenMenuItems.intersect(app.reelstack.data.model.DEFAULT_MENU.toSet()) - value.requiredMenu())
            putBoolean("show_next_up", value.showNextUp)
            putBoolean("combine_continue", value.combineContinueWatching)
            putBoolean("show_hero", value.showHero)
            putBoolean("hero_rotate", value.heroRotate)
            putBoolean("hero_logo", value.heroLogo)
            putBoolean("hero_compact", value.heroCompact)
            putBoolean("start_in_library", value.startInLibrary)
            putBoolean("library_hub", value.libraryHub)
            putBoolean("library_title", value.showLibraryTitle)
            putBoolean("library_cards_wide", value.libraryCardsWide)
            putString("library_hub_order", value.libraryHubOrder.joinToString(","))
            putStringSet("library_hub_hidden", value.libraryHubHidden)
            putString("library_order", value.libraryOrder.joinToString(","))
            putStringSet("library_hidden", value.libraryHidden)
            putBoolean("show_upcoming_episodes", value.showUpcomingEpisodes)
            putBoolean("reduce_motion", value.reduceMotion)
            putString("home_row_formats", value.homeRowFormats.entries.joinToString(",") { "${it.key}=${it.value}" })
            putBoolean("show_ratings", value.showRatings)
            putBoolean("show_quality", value.showQuality)
            putBoolean("detail_backdrop", value.detailBackdrop)
            putBoolean("slow_startup", value.slowStartup)
            putString("visual_theme", value.visualTheme.name)
            putString("artwork_corners", value.artworkCorners.name)
            putString("focus_style", value.focusStyle.name)
            putBoolean("high_contrast", value.highContrast)
            putBoolean("seasonal_ornament", value.seasonalOrnament)
            putBoolean("library_card_names", value.showLibraryCardNames)
            putBoolean("watch_next_enabled", value.watchNextEnabled)
            putString("subtitle_style", value.subtitleStyle.name)
            putString("subtitle_language", value.preferredSubtitleLanguage.name)
            putString("subtitle_fallback", value.fallbackSubtitleLanguage.name)
        }

    fun observePersonalization(onChange: (app.reelstack.data.model.Personalization) -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in setOf("app_label", "watch_next_enabled", "subtitle_style", "library_card_names", "accent_palette", "artwork_size", "auto_resume", "sidebar_expanded", "hide_tv_sidebar", "menu_order", "menu_hidden",
                    "show_next_up", "combine_continue", "show_hero", "show_ratings", "show_quality", "slow_startup",
                    "visual_theme", "artwork_corners", "focus_style", "high_contrast",
                    "seasonal_ornament", "show_next_episode", "next_episode_lead", "auto_play_next_episode",
                    "next_episode_delay", "lightweight_tv", "detail_backdrop", "hero_rotate", "hero_logo",
                    "hero_compact", "start_in_library", "library_hub", "show_upcoming_episodes", "reduce_motion", "home_row_formats", "library_title", "library_cards_wide", "library_hub_order", "library_hub_hidden", "library_order", "subtitle_language", "subtitle_fallback")) onChange(personalization)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onChange(personalization)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** Null preserves the existing default; an empty saved set explicitly selects nothing. */
    fun selectedLibraryIds(connection: app.reelstack.data.model.ServiceConnection): Set<String>? =
        preferences.getStringSet(libraryKey(connection), null)?.toSet()

    fun setSelectedLibraryIds(connection: app.reelstack.data.model.ServiceConnection, ids: Set<String>) {
        preferences.edit { putStringSet(libraryKey(connection), ids.toSet()) }
    }

    fun libraryShortcuts(connection: app.reelstack.data.model.ServiceConnection): List<Pair<String, String>> = runCatching {
        Json.parseToJsonElement(preferences.getString("pins_" + libraryKey(connection), "{}")!!).jsonObject
            .map { it.key to it.value.jsonPrimitive.content }
            .filter { selectedLibraryIds(connection)?.contains(it.first) != false }
    }.getOrDefault(emptyList())

    fun setLibraryShortcuts(connection: app.reelstack.data.model.ServiceConnection, shortcuts: List<Pair<String, String>>) {
        preferences.edit { putString("pins_" + libraryKey(connection), buildJsonObject {
            shortcuts.distinctBy { it.first }.forEach { (id, name) -> put(id, name) }
        }.toString()) }
    }

    fun libraryIcons(connection: app.reelstack.data.model.ServiceConnection): Map<String, app.reelstack.data.model.LibraryIcon> = runCatching {
        Json.parseToJsonElement(preferences.getString("icons_" + libraryKey(connection), "{}")!!).jsonObject.mapNotNull { (key, value) ->
            app.reelstack.data.model.LibraryIcon.entries.find { it.name == value.jsonPrimitive.content }?.let { key to it }
        }.toMap()
    }.getOrDefault(emptyMap())

    fun setLibraryIcons(connection: app.reelstack.data.model.ServiceConnection, icons: Map<String, app.reelstack.data.model.LibraryIcon>) {
        preferences.edit { putString("icons_" + libraryKey(connection), buildJsonObject { icons.forEach { (id, icon) -> put(id, icon.name) } }.toString()) }
    }

    fun includesLibrary(connection: app.reelstack.data.model.ServiceConnection,
        view: app.reelstack.data.network.RemoteLibraryView): Boolean =
        selectedLibraryIds(connection)?.contains(view.id) ?: true

    fun librarySelectionFingerprint(connections: List<app.reelstack.data.model.ServiceConnection>): String =
        connections.filter { it.kind == app.reelstack.data.model.ServiceKind.JELLYFIN }.sortedBy { it.identity }
            .joinToString("|") { connection -> libraryKey(connection) + ":" +
                (selectedLibraryIds(connection)?.sorted()?.joinToString(",") ?: "default") }

    private fun libraryKey(connection: app.reelstack.data.model.ServiceConnection): String {
        val identity = "${connection.kind}|${connection.identity.trimEnd('/')}|${connection.userId}"
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        return "library_selection_" + digest.joinToString("") { "%02x".format(it) }
    }

    var onboardingCompleted: Boolean
        get() = preferences.getBoolean("onboarding_completed", false)
        set(value) = preferences.edit { putBoolean("onboarding_completed", value) }

    var notificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = preferences.edit { putBoolean(KEY_NOTIFICATIONS, value) }

    var wifiOnly: Boolean
        get() = preferences.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = preferences.edit { putBoolean(KEY_WIFI_ONLY, value) }

    /** Local-only, profile-scoped search recall. Queries never leave the device as telemetry. */
    fun searchHistory(profileId: String): List<String> = runCatching {
        Json.parseToJsonElement(preferences.getString(searchHistoryKey(profileId), "[]")!!).jsonArray
            .mapNotNull { it.jsonPrimitive.contentOrNull?.trim() }
            .filter { it.length in 2..120 && it.none(Char::isISOControl) }
            .distinct()
            .take(12)
    }.getOrDefault(emptyList())

    fun rememberSearch(profileId: String, query: String) {
        val cleaned = query.trim().take(120)
        if (cleaned.length < 2 || cleaned.any(Char::isISOControl)) return
        preferences.edit {
            putString(searchHistoryKey(profileId), buildJsonArray {
                add(JsonPrimitive(cleaned))
                searchHistory(profileId).filterNot { it.equals(cleaned, ignoreCase = true) }.take(11)
                    .forEach { add(JsonPrimitive(it)) }
            }.toString())
        }
    }

    fun clearSearchHistory(profileId: String) { preferences.edit { remove(searchHistoryKey(profileId)) } }

    private fun searchHistoryKey(profileId: String): String {
        val stable = profileId.ifBlank { "adult" }
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(stable.toByteArray(Charsets.UTF_8))
        return "search_history_" + digest.joinToString("") { "%02x".format(it) }
    }

    var homeRowOrder: List<app.reelstack.data.model.HomeRow>
        get() = app.reelstack.data.model.decodeHomeRowOrder(preferences.getString("home_row_order", null))
        set(value) = preferences.edit {
            putString("home_row_order", app.reelstack.data.model.decodeHomeRowOrder(value.joinToString(",") { it.name }).joinToString(",") { it.name })
        }

    var visibleHomeSections: Set<HomeSection>
        get() {
            val saved = preferences.getStringSet(KEY_HOME_SECTIONS, null)
            val decoded = decodeHomeSections(saved)
            // Add the new shared rails once for existing installations. After the first read, the
            // normal setter owns the choice so a user can turn them off again.
            if (saved != null && saved.isNotEmpty() &&
                preferences.getInt(KEY_HOME_SECTIONS_VERSION, 0) < HOME_SECTIONS_VERSION
            ) {
                val migrated = decoded + HomeSection.RECOMMENDATIONS + HomeSection.RECENT_RELEASES +
                    HomeSection.CONTINUE_WATCHING + HomeSection.FAVOURITES
                preferences.edit {
                    putStringSet(KEY_HOME_SECTIONS, migrated.mapTo(mutableSetOf()) { it.name })
                    putInt(KEY_HOME_SECTIONS_VERSION, HOME_SECTIONS_VERSION)
                }
                return migrated
            }
            return decoded
        }
        set(value) = preferences.edit {
            putStringSet(KEY_HOME_SECTIONS, value.mapTo(mutableSetOf()) { it.name })
            putInt(KEY_HOME_SECTIONS_VERSION, HOME_SECTIONS_VERSION)
        }

    private companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_WIFI_ONLY = "wifi_only"
        const val KEY_HOME_SECTIONS = "home_sections"
        const val KEY_HOME_SECTIONS_VERSION = "home_sections_version"
        // 4: Favourites. A section added after someone saved their choices is absent from that
        // saved set, and absent reads as "off" — so a new row would never appear for anyone who
        // had ever opened the settings. The version says which additions this install has seen.
        const val HOME_SECTIONS_VERSION = 4
    }
}
