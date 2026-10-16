package app.reelstack.ui

/** What the system Back does on the main pages when no sheet or search screen owns it. */
internal enum class ShellBack { CLOSE_GRID, CLOSE_MY_REQUESTS, START_TAB }

/**
 * One decision instead of several handlers whose order decided the answer: the last registered
 * handler wins, so «go to the start tab» used to win over closing a page inside Oppdag.
 */
internal fun shellBack(state: ReelstackUiState, startTab: AppTab): ShellBack? {
    if (state.globalSearchOpen || state.activeSheet != null || state.showOnboarding) return null
    if (state.selectedTab == AppTab.DISCOVER && state.discoverHub.grid != null) return ShellBack.CLOSE_GRID
    if (state.selectedTab == AppTab.DISCOVER && state.myRequestsOpen) return ShellBack.CLOSE_MY_REQUESTS
    return if (state.selectedTab != startTab) ShellBack.START_TAB else null
}
