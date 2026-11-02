package app.reelstack.ui

/** What the television's top menu can hold. */
internal sealed interface TopMenuEntry {
    data class Page(val tab: AppTab) : TopMenuEntry
    data class Library(val id: String, val label: String) : TopMenuEntry
    data object Search : TopMenuEntry
}

/**
 * The top menu's entries, in the owner's order from «Sider og meny».
 *
 * The libraries stand where the library page stands, since they are the library page opened up: a
 * film shelf and a series shelf are one press from the menu instead of behind a page that lists them.
 * With none known the page itself stands there. Settings live behind the profile circle, and search
 * is always the last entry.
 */
internal fun topMenuEntries(visibleMenu: List<String>, libraries: List<Pair<String, String>>): List<TopMenuEntry> = buildList {
    visibleMenu.forEach { id ->
        when (id) {
            AppTab.HOME.name -> add(TopMenuEntry.Page(AppTab.HOME))
            AppTab.LIBRARY.name ->
                if (libraries.isEmpty()) add(TopMenuEntry.Page(AppTab.LIBRARY))
                else libraries.distinctBy { it.first }.forEach { add(TopMenuEntry.Library(it.first, it.second)) }
            AppTab.DISCOVER.name -> add(TopMenuEntry.Page(AppTab.DISCOVER))
        }
    }
    add(TopMenuEntry.Search)
}
