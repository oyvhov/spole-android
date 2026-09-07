package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Seerr answers 20 results at a time, so a long search needs more than the first page. */
class SearchPaginationTest {
    private class Pages(private val body: (Int) -> String) : JsonHttpTransport {
        val urls = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            val page = Regex("page=([0-9]+)").find(url)?.groupValues?.get(1)?.toInt() ?: 1
            return HttpResponse(200, body(page))
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("unused")
    }

    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "key")

    private fun page(number: Int, total: Int) = """
        {"page":$number,"totalPages":$total,
         "results":[{"id":${number}01,"mediaType":"movie","title":"Treff $number"}]}
    """.trimIndent()

    @Test fun theFirstPageReportsThatMoreExist() {
        val result = SeerrServiceClient(Pages { page(it, 5) }).search(seerr, "dune")
        assertEquals(1, result.page)
        assertEquals(5, result.totalPages)
        assertTrue(result.hasMore)
        assertEquals(listOf("Treff 1"), result.items.map { it.title })
    }

    @Test fun askingForTheNextPageRequestsItAndReportsWhenItIsTheLast() {
        val transport = Pages { page(it, 3) }
        val client = SeerrServiceClient(transport)
        client.search(seerr, "dune", page = 2)
        assertTrue(transport.urls.single().contains("page=2"))

        val last = SeerrServiceClient(Pages { page(it, 3) }).search(seerr, "dune", page = 3)
        assertFalse(last.hasMore)
    }

    @Test fun aPageNumberBelowOneIsRejectedRatherThanSentToTheServer() {
        val transport = Pages { page(it, 1) }
        assertTrue(runCatching { SeerrServiceClient(transport).search(seerr, "dune", page = 0) }.isFailure)
        assertTrue(transport.urls.isEmpty())
    }

    @Test fun aResponseWithoutPagingIsTreatedAsOnePage() {
        val result = SeerrServiceClient(Pages { """{"results":[]}""" }).search(seerr, "dune")
        assertEquals(1, result.totalPages)
        assertFalse(result.hasMore)
    }
}
