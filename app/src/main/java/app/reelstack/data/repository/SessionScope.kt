package app.reelstack.data.repository

/** An in-process lease. Even switching away and back revokes previously captured work. */
data class SessionScope(val profileId: String, val accountFingerprint: String, val generation: Long) {
    val kidsMode: Boolean get() = profileId.isNotBlank()
}
