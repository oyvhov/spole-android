package app.reelstack.ui.layout

/**
 * Pure window rules: never infer a tablet from its model name or its physical display size.
 *
 * Every width threshold in the app lives here. They used to be spread over seven files — 600 in two
 * sheet layouts, 680 in Discover and TV welcome, 900 in the page wrapper and Settings, 1000 in the
 * app shell — so the bands between them were leftovers rather than designed states. The measurement
 * is whatever container asks: the app shell passes the window, a sheet passes its own width.
 */
data class WindowLayoutPolicy(val widthDp: Float, val heightDp: Float) {
    /** A two-column body inside a sheet or dialog: poster beside text instead of stacked. */
    val useSideBySideMedia: Boolean get() = widthDp >= 600f

    val useNavigationRail: Boolean get() = widthDp >= 640f

    /** Heading, search and account share one row; the TV welcome splits into two panes. */
    val useInlineHeader: Boolean get() = widthDp >= 680f

    val showHomeSearch: Boolean get() = !useNavigationRail
    val useCenteredDialog: Boolean get() = widthDp >= 840f && heightDp >= 480f

    /** Full media canvas: larger artwork, Home feature, category/content Settings. */
    val useTabletCanvas: Boolean get() = widthDp >= 900f

    /** Only the initial state of the sidebar; a stored user choice always wins. */
    val expandSidebarByDefault: Boolean get() = widthDp >= 1000f
    // Media fills the remaining window; only reading pages keep a maximum column width.
    val mediaMaxWidthDp: Float get() = widthDp
    val dialogWidthDp: Float get() = if (useCenteredDialog) 720f else 640f

    fun dialogHeightDp(availableHeightDp: Float): Float {
        val height = availableHeightDp.coerceAtLeast(0f)
        return when {
            useCenteredDialog -> (height * .86f).coerceAtMost(860f)
            height < 480f -> height * .94f
            else -> height * .82f
        }
    }
}
