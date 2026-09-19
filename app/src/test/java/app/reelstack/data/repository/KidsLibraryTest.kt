package app.reelstack.data.repository

import app.reelstack.data.model.*
import app.reelstack.data.network.*
import org.junit.Assert.*
import org.junit.Test

class KidsLibraryTest {
    private val connection = ServiceConnection(ServiceKind.EMBY, "Child", "https://media.example", "test-token", userId = "child")

    @Test fun assignsLibraryFromRequestedViewWhenServerOmitsLibraryId() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse = HttpResponse(200,
                if (url.contains("/Views")) """{"Items":[{"Id":"films","Name":"Films","CollectionType":"movies"}]}"""
                else """{"Items":[{"Id":"movie","Name":"A story","Type":"Movie"}]}""")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Unexpected write")
        }
        val media = MediaSyncRepository(mediaServerClient = MediaServerClient(transport)).accountLibrary(connection)
        assertEquals(1, media.size)
        assertEquals("films", media.single().libraryId)
        assertEquals(ServiceKind.EMBY, media.single().source)
    }

    @Test fun unavailableServerIsAnErrorRatherThanAnEmptyChildLibrary() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse = throw java.io.IOException("Offline")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Unexpected write")
        }
        assertThrows(java.io.IOException::class.java) {
            MediaSyncRepository(mediaServerClient = MediaServerClient(transport)).accountLibrary(connection)
        }
    }
}
