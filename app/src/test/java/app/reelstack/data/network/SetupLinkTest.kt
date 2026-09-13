package app.reelstack.data.network

import org.junit.Assert.*
import org.junit.Test

class SetupLinkTest {
    @Test fun roundTripPreservesServerPaths() {
        val setup = SetupLink("https://media.example/jellyfin", "https://requests.example/seerr")
        assertEquals(setup, SetupLink.parse(setup.encode()))
    }
    @Test fun seerrIsOptional() {
        val setup = SetupLink("https://media.example")
        assertEquals(setup, SetupLink.parse(setup.encode()))
    }
    @Test fun refusesCredentialsAndUnexpectedFields() {
        listOf("spole://setup?jellyfin=https%3A%2F%2Fuser%3Asecret%40media.example",
            "spole://setup?jellyfin=https%3A%2F%2Fmedia.example%3Fapi_key%3Dsecret",
            "spole://setup?jellyfin=https%3A%2F%2Fmedia.example&token=secret",
            "spole://setup?jellyfin=https%3A%2F%2Fmedia.example&jellyfin=https%3A%2F%2Fother.example",
            "https://setup?jellyfin=https%3A%2F%2Fmedia.example",
            "spole://setup?jellyfin=http%3A%2F%2Fmedia.example",
            "spole://setup?jellyfin=%xx", "spole://setup?seerr=https%3A%2F%2Frequests.example"
        ).forEach { assertTrue(runCatching { SetupLink.parse(it) }.isFailure) }
    }
    @Test fun sharingRejectsSecretsInsteadOfSilentlyStrippingThem() {
        assertTrue(runCatching { SetupLink("https://media.example?token=secret").encode() }.isFailure)
        assertTrue(runCatching { SetupLink("https://media.example", "https://user:secret@requests.example").encode() }.isFailure)
    }
}
