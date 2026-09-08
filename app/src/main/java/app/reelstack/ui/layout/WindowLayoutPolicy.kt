package app.reelstack.ui.layout

/** Pure window rules: never infer a tablet from its model name or its physical display size. */
data class WindowLayoutPolicy(val widthDp: Float, val heightDp: Float) {
    val useNavigationRail: Boolean get() = widthDp >= 640f
    val useCenteredDialog: Boolean get() = widthDp >= 840f && heightDp >= 480f
    // This width is measured after the navigation rail has taken its space.
    val mediaMaxWidthDp: Float get() = if (widthDp >= 1000f) 1120f else 840f
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
