package app.reelstack.data.repository

/** An in-process lease. Even switching away and back revokes previously captured work. */
data class SessionScope(val profileId: String, val accountFingerprint: String, val generation: Long,
    /** A child's profile. An own, grown-up profile is not one, though it is not the main profile either. */
    val kids: Boolean = profileId.isNotBlank()) {
    val kidsMode: Boolean get() = kids
}
