package app.reelstack.data.model

/** Profile model for switching between adult/main account and kid accounts. */
data class UserProfile(
    val id: String, // "" for main account, or child account ID
    val name: String,
    val isKid: Boolean,
    val avatarUrl: String? = null,
) {
    val isMain: Boolean get() = id.isBlank()
}
