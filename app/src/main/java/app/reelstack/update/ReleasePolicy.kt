package app.reelstack.update

import kotlinx.serialization.json.*
import java.net.URI

internal const val RELEASE_REPO = "oyvhov/spole-android"
internal const val RELEASE_API = "https://api.github.com/repos/$RELEASE_REPO/releases?per_page=100"
internal const val MAX_APK_BYTES = 300L * 1024 * 1024

internal data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int, val stage: Int, val number: Int) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int = compareValuesBy(this, other,
        { it.major }, { it.minor }, { it.patch }, { it.stage }, { it.number })
    companion object {
        fun parse(raw: String): ReleaseVersion? {
            val match = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-(alpha|beta|rc)[.-]?(\\d+))?(?:-debug)?$").matchEntire(raw) ?: return null
            return runCatching { ReleaseVersion(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt(),
                when (match.groupValues[4]) { "alpha" -> 0; "beta" -> 1; "rc" -> 2; else -> 3 }, match.groupValues[5].toIntOrNull() ?: 0) }.getOrNull()
        }
    }
}

internal data class AppRelease(val tag: String, val notes: String, val assetId: Long, val size: Long, val digest: String) {
    fun encode() = buildJsonObject { put("tag", tag); put("notes", notes); put("id", assetId); put("size", size); put("digest", digest) }.toString()
    companion object {
        fun decode(raw: String): AppRelease? = runCatching {
            val o = Json.parseToJsonElement(raw).jsonObject
            AppRelease(o.getValue("tag").jsonPrimitive.content, o.getValue("notes").jsonPrimitive.content,
                o.getValue("id").jsonPrimitive.long, o.getValue("size").jsonPrimitive.long, o.getValue("digest").jsonPrimitive.content)
        }.getOrNull()
    }
}

/** Compare actual versions, never GitHub's publication order or a lexicographic alpha suffix. */
internal fun newerRelease(raw: String, current: String, previews: Boolean): AppRelease? {
    val installed = ReleaseVersion.parse(current) ?: return null
    return Json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
        val o = element.jsonObject
        if (o["draft"]?.jsonPrimitive?.booleanOrNull == true) return@mapNotNull null
        val tag = o["tag_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        if (tag.contains("debug", true)) return@mapNotNull null
        val version = ReleaseVersion.parse(tag) ?: return@mapNotNull null
        if (version <= installed || (!previews && (version.stage < 3 || o["prerelease"]?.jsonPrimitive?.booleanOrNull == true))) return@mapNotNull null
        val assets = o["assets"]?.jsonArray.orEmpty().map { it.jsonObject }.filter {
            val name = it["name"]?.jsonPrimitive?.content.orEmpty()
            name.endsWith(".apk", true) && !name.contains("debug", true) && it["state"]?.jsonPrimitive?.content == "uploaded"
        }
        // This app publishes one universal signed APK. Ambiguous split/ABI releases need an explicit future policy.
        val asset = assets.singleOrNull() ?: return@mapNotNull null
        val id = asset["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
        val size = asset["size"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
        val digest = asset["digest"]?.jsonPrimitive?.content.orEmpty().removePrefix("sha256:").lowercase()
        if (id <= 0 || size !in 1..MAX_APK_BYTES || !digest.matches(Regex("[0-9a-f]{64}"))) return@mapNotNull null
        version to AppRelease(tag, o["body"]?.jsonPrimitive?.content.orEmpty().take(12000), id, size, digest)
    }.maxByOrNull { it.first }?.second
}

internal fun trustedUpdateUrl(url: String): Boolean = runCatching {
    val uri = URI(url)
    uri.scheme == "https" && uri.rawUserInfo == null && uri.port in setOf(-1, 443) &&
        (uri.host == "api.github.com" && uri.path.startsWith("/repos/$RELEASE_REPO/releases") ||
            uri.host in setOf("release-assets.githubusercontent.com", "objects.githubusercontent.com", "github-releases.githubusercontent.com"))
}.getOrDefault(false)

internal data class UpdatePackageIdentity(val name: String, val versionCode: Long, val versionName: String,
    val minSdk: Int, val debug: Boolean, val signers: Set<String>)

internal fun compatibleUpdate(installed: UpdatePackageIdentity, candidate: UpdatePackageIdentity, tag: String, sdk: Int): Boolean {
    val expected = ReleaseVersion.parse(tag) ?: return false
    return candidate.name == installed.name && candidate.versionCode > installed.versionCode && candidate.minSdk <= sdk &&
        !candidate.debug && ReleaseVersion.parse(candidate.versionName) == expected && installed.signers.isNotEmpty() &&
        installed.signers == candidate.signers
}
