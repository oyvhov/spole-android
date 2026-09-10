package app.reelstack.update

import org.junit.Assert.*
import org.junit.Test

class ReleasePolicyTest {
    private fun release(tag: String, preview: Boolean = true, draft: Boolean = false, asset: String = "Spole.apk", digest: String = "a".repeat(64), size: Long = 5000) =
        """{"tag_name":"$tag","prerelease":$preview,"draft":$draft,"body":"Notes","assets":[{"id":5,"name":"$asset","state":"uploaded","size":$size,"digest":"sha256:$digest"}]}"""
    @Test fun numericPreviewVersionsAndStableOrder() {
        assertTrue(ReleaseVersion.parse("v0.16.0-alpha12")!! > ReleaseVersion.parse("0.16.0-alpha9")!!)
        assertTrue(ReleaseVersion.parse("0.16.0")!! > ReleaseVersion.parse("0.16.0-rc9")!!)
        assertNull(ReleaseVersion.parse("v0.16.0-malicious"))
    }
    @Test fun picksHighestVersionNotFirstPublishedItem() {
        val raw = "[${release("v0.16.0-alpha13")},${release("v0.16.0-alpha20")},${release("v0.16.0-alpha9")}]"
        assertEquals("v0.16.0-alpha20", newerRelease(raw, "0.16.0-alpha12", true)?.tag)
    }
    @Test fun stableChannelExcludesPreviewsAndDowngrades() {
        assertNull(newerRelease("[${release("v0.16.0-alpha13")},${release("v0.15.1", false)}]", "0.16.0-alpha12", false))
        assertNotNull(newerRelease("[${release("v0.16.0", false)}]", "0.16.0-alpha12", false))
    }
    @Test fun draftsDebugAndIncompleteAssetsCannotBeUpdates() {
        for (item in listOf(release("v1.0.0", draft = true), release("v1.0.0", asset = "app-debug.apk"),
                release("v1.0.0", digest = ""), release("v1.0.0", size = MAX_APK_BYTES + 1), release("v1.0.0", size = 0)))
            assertNull(newerRelease("[$item]", "0.16.0-alpha12", true))
    }
    @Test fun ambiguousApkAssetsAreNotGuessed() {
        val item = release("v1.0.0").replace("}]}", "},{\"id\":6,\"name\":\"other.apk\",\"state\":\"uploaded\"}]}")
        assertNull(newerRelease("[$item]", "0.16.0-alpha12", true))
    }
    @Test fun redirectsCannotEscapeGithubReleaseHostsOrLeakToOtherRepos() {
        assertTrue(trustedUpdateUrl("https://api.github.com/repos/oyvhov/spole-android/releases/assets/1"))
        assertTrue(trustedUpdateUrl("https://release-assets.githubusercontent.com/asset?token=temporary"))
        for (url in listOf("http://release-assets.githubusercontent.com/file", "https://github.com.evil.test/file",
                "https://api.github.com/repos/another/repo/releases", "https://user@release-assets.githubusercontent.com/file",
                "https://release-assets.githubusercontent.com:8443/file", "https://127.0.0.1/file")) assertFalse(url, trustedUpdateUrl(url))
    }
    @Test fun cachedReleaseRoundTripsWithoutLosingDigestOrNotes() {
        val release = AppRelease("v1.0.0", "a\nb\"c", 8, 256, "a".repeat(64))
        assertEquals(release, AppRelease.decode(release.encode()))
        assertNull(AppRelease.decode("invalid"))
    }
    @Test fun installerAcceptsOnlyANewerMatchingSignedPackage() {
        val installed = UpdatePackageIdentity("app.reelstack", 54, "0.16.0-alpha11", 26, false, setOf("certificate"))
        val candidate = installed.copy(versionCode = 55, versionName = "0.16.0-alpha12")
        assertTrue(compatibleUpdate(installed, candidate, "v0.16.0-alpha12", 36))
        for (bad in listOf(candidate.copy(name = "another.app"), candidate.copy(versionCode = 54),
                candidate.copy(versionCode = 53), candidate.copy(versionName = "1.0.0"), candidate.copy(debug = true),
                candidate.copy(minSdk = 37), candidate.copy(signers = setOf("impostor")), candidate.copy(signers = emptySet())))
            assertFalse(compatibleUpdate(installed, bad, "v0.16.0-alpha12", 36))
        assertFalse(compatibleUpdate(installed.copy(signers = emptySet()), candidate, "v0.16.0-alpha12", 36))
    }
}
