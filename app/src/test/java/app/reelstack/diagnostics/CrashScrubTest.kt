package app.reelstack.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A crash report is only shareable if it cannot carry the two things the user never agreed to
 * hand over: where their server lives, and anything that opens it.
 */
class CrashScrubTest {
    @Test fun serverAddressesAreRemoved() {
        val scrubbed = CrashReporter.scrub(
            "java.io.IOException: failed to connect to https://media.heime.example:8096/System/Info",
        )

        assertFalse(scrubbed, scrubbed.contains("media.heime.example"))
        assertFalse(scrubbed, scrubbed.contains("8096"))
        assertTrue(scrubbed, scrubbed.contains("<adresse fjerna>"))
    }

    @Test fun cleartextAddressesAreRemovedToo() {
        assertFalse(CrashReporter.scrub("at http://192.168.1.20:8096/Users").contains("192.168"))
    }

    @Test fun credentialsAreRemovedWhateverTheyAreCalled() {
        val scrubbed = CrashReporter.scrub(
            "Authorization=MediaBrowser Token=abc123 api_key: deadbeef password=hunter2",
        )

        assertFalse(scrubbed, scrubbed.contains("abc123"))
        assertFalse(scrubbed, scrubbed.contains("deadbeef"))
        assertFalse(scrubbed, scrubbed.contains("hunter2"))
    }

    @Test fun theUsefulPartOfATraceSurvives() {
        // Class and method names are the whole point of keeping the report. Scrubbing them would
        // leave a file that is safe and useless.
        val scrubbed = CrashReporter.scrub(
            "at app.reelstack.data.network.MediaServerClient.sessions(ServiceClients.kt:250)",
        )

        assertTrue(scrubbed, scrubbed.contains("MediaServerClient.sessions"))
        assertTrue(scrubbed, scrubbed.contains("ServiceClients.kt:250"))
    }
}
