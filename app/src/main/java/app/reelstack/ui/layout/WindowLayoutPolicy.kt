package app.reelstack.ui.layout

/** Pure window rules: never infer a tablet from its model name or its physical display size. */
data class WindowLayoutPolicy(val widthDp: Float, val heightDp: Float) {
    val useNavigationRail: Boolean get() = widthDp >= 640f
    val showHomeSearch: Boolean get() = !useNavigationRail
    val useCenteredDialog: Boolean get() = widthDp >= 840f && heightDp >= 480f
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
