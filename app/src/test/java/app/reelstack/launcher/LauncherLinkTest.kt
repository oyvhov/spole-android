package app.reelstack.launcher

import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherLinkTest {
    @Test fun parsesJellyfinPlayLink() {
        assertEquals(
            LauncherLink(ServiceKind.JELLYFIN, "user_123", "item-456", LauncherAction.PLAY),
            LauncherLinks.parse("spole://open?kind=jellyfin&user=user_123&item=item-456&action=play"),
        )
    }

    @Test fun parsesEmbyDetailsAndIgnoresUnknownParameters() {
        assertEquals(
            LauncherLink(ServiceKind.EMBY, "User", "Item", LauncherAction.DETAILS),
            LauncherLinks.parse("spole://open?extra=%C3%B8&item=Item&kind=emby&action=details&user=User&extra=again"),
        )
    }

    @Test fun decodesQueryNamesAndValuesOnce() {
        assertEquals(
            LauncherLink(ServiceKind.JELLYFIN, "user_1", "item-2", LauncherAction.PLAY),
            LauncherLinks.parse("spole://open?%6bind=jellyfin&user=%75ser%5F1&item=item%2D2&action=%70lay"),
        )
    }

    @Test fun acceptsIdentifierLengthBoundaries() {
        listOf("A", "aZ09_-".repeat(21) + "_-", "_", "-").forEach { id ->
            assertEquals(
                LauncherLink(ServiceKind.JELLYFIN, id, id, LauncherAction.DETAILS),
                LauncherLinks.parse("spole://open?kind=jellyfin&user=$id&item=$id&action=details"),
            )
        }
    }

    @Test fun rejectsAbsentOrEmptyInput() {
        listOf(null, "", " ", "\n").forEach { assertNull(it, LauncherLinks.parse(it)) }
    }

    @Test fun rejectsOtherSchemesAuthoritiesAndRoutes() {
        listOf(
            "https://open", "spole://setup", "spole://other", "spole:open", "spole:///open",
            "spole://open/", "spole://open/details", "spole://open:80", "spole://user@open",
            "spole://open.evil", "spole://%6fpen", "spole://open/../", "spole://open/%2e%2e",
        ).forEach { prefix -> assertNull(prefix, LauncherLinks.parse("$prefix?$validQuery")) }
    }

    @Test fun rejectsFragmentsIncludingEmptyFragment() {
        listOf("#", "#details", "#?action=play").forEach { fragment ->
            assertNull(fragment, LauncherLinks.parse("spole://open?$validQuery$fragment"))
        }
    }

    @Test fun rejectsMalformedUrisAndPercentEncoding() {
        listOf(
            "spole://[open?$validQuery", "spole://open?kind=jellyfin&user=a b&item=b&action=play",
            "spole://open?$validQuery&extra=%", "spole://open?$validQuery&extra=%GG",
            "spole://open?$validQuery&extra=%C3%28", "spole://open?$validQuery&extra=%FF",
            "spole://open?$validQuery&%FF=value",
        ).forEach { raw -> assertNull(raw, LauncherLinks.parse(raw)) }
    }

    @Test fun rejectsUnsupportedKindsAndActions() {
        listOf("seerr", "JELLYFIN", "", "jellyfin%20").forEach { kind ->
            assertNull(kind, LauncherLinks.parse("spole://open?kind=$kind&user=a&item=b&action=play"))
        }
        listOf("setup", "PLAY", "", "play%20").forEach { action ->
            assertNull(action, LauncherLinks.parse("spole://open?kind=jellyfin&user=a&item=b&action=$action"))
        }
    }

    @Test fun requiresEveryParameter() {
        val parameters = validQuery.split('&')
        parameters.indices.forEach { omitted ->
            val query = parameters.filterIndexed { index, _ -> index != omitted }.joinToString("&")
            assertNull(query, LauncherLinks.parse("spole://open?$query"))
        }
        assertNull(LauncherLinks.parse("spole://open"))
        assertNull(LauncherLinks.parse("spole://open?"))
    }

    @Test fun rejectsUnsafeIdentifiersForBothFields() {
        listOf(
            "", "..", "../item", "%2E%2E%2Fitem", "a%2Fb", "a%5Cb", "%252F", "a.b",
            "a+b", "a%20b", "%00", "%0A", "%C3%B8", "a%26b", "a%3Db",
            "a".repeat(129), "a".repeat(500),
        ).forEach { id ->
            assertNull("user=$id", LauncherLinks.parse("spole://open?kind=jellyfin&user=$id&item=b&action=play"))
            assertNull("item=$id", LauncherLinks.parse("spole://open?kind=jellyfin&user=a&item=$id&action=play"))
        }
    }

    @Test fun rejectsDuplicateRequiredParametersEvenWhenValuesMatch() {
        listOf("kind=jellyfin", "user=a", "item=b", "action=play", "%75ser=a", "action=details").forEach { duplicate ->
            assertNull(duplicate, LauncherLinks.parse("spole://open?$validQuery&$duplicate"))
        }
    }

    @Test fun parsesLibraryLinksForBothServicesAndDecodesOnce() {
        assertEquals(
            LauncherLink(ServiceKind.JELLYFIN, "user_1", "library-2", LauncherAction.LIBRARY),
            LauncherLinks.parse("spole://library?%6bind=jellyfin&user=%75ser%5F1&library=library%2D2"),
        )
        assertEquals(
            LauncherLink(ServiceKind.EMBY, "U", "L", LauncherAction.LIBRARY),
            LauncherLinks.parse("spole://library?extra=%C3%B8&library=L&kind=emby&user=U&extra=again"),
        )
        val id = "a".repeat(128)
        assertEquals(
            LauncherLink(ServiceKind.EMBY, id, id, LauncherAction.LIBRARY),
            LauncherLinks.parse("spole://library?kind=emby&user=$id&library=$id"),
        )
    }

    @Test fun rejectsMissingDuplicateAndInvalidLibraryParameters() {
        val valid = "kind=jellyfin&user=a&library=b"
        valid.split('&').indices.forEach { omitted ->
            val query = valid.split('&').filterIndexed { index, _ -> index != omitted }.joinToString("&")
            assertNull(query, LauncherLinks.parse("spole://library?$query"))
        }
        listOf("kind=jellyfin", "user=a", "%75ser=a", "library=b", "%6cibrary=b").forEach { duplicate ->
            assertNull(duplicate, LauncherLinks.parse("spole://library?$valid&$duplicate"))
        }
        listOf("", "..", "a%2Fb", "a%5Cb", "%252F", "a+b", "%00", "%C3%B8", "a%26b", "a".repeat(129)).forEach { id ->
            assertNull("user=$id", LauncherLinks.parse("spole://library?kind=jellyfin&user=$id&library=b"))
            assertNull("library=$id", LauncherLinks.parse("spole://library?kind=jellyfin&user=a&library=$id"))
        }
        listOf("seerr", "JELLYFIN", "", "emby%20").forEach { kind ->
            assertNull(kind, LauncherLinks.parse("spole://library?kind=$kind&user=a&library=b"))
        }
    }

    @Test fun rejectsMalformedLibraryUrisAndDecodingEvenInUnknownParameters() {
        val valid = "kind=jellyfin&user=a&library=b"
        listOf(
            "spole://library/?$valid", "spole://library/path?$valid", "spole://library:80?$valid",
            "spole://user@library?$valid", "spole://%6cibrary?$valid", "spole://library?$valid#",
            "spole://library?$valid&extra=%", "spole://library?$valid&extra=%FF",
            "spole://library?$valid&extra=%C3%28", "spole://library?$valid&%FF=value",
            "spole://library", "spole://library?",
        ).forEach { raw -> assertNull(raw, LauncherLinks.parse(raw)) }
    }

    @Test fun openHostNeverTreatsLibraryAsAnItem() {
        assertNull(LauncherLinks.parse("spole://open?kind=jellyfin&user=a&library=b"))
        assertEquals(
            LauncherLink(ServiceKind.JELLYFIN, "a", "b", LauncherAction.PLAY),
            LauncherLinks.parse("spole://open?$validQuery&library=ignored"),
        )
    }

    private val validQuery = "kind=jellyfin&user=a&item=b&action=play"
}
