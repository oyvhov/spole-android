package app.reelstack.data.network

import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Test

class RequestRulesTest {
    @Test fun missingQuotaIsUnknownRatherThanUnlimitedOrExhausted() {
        assertNull(parseRequestQuota("{}", "movie"))
        assertNull(parseRequestQuota("""{"tv":{}}""", "tv"))
        assertNull(parseRequestQuota("""{"movie":{"limit":-1,"remaining":-9}}""", "movie"))
    }
    @Test fun zeroLimitExplicitlyMeansUnlimitedEvenWithoutRemaining() {
        val quota = parseRequestQuota("""{"movie":{"limit":0,"days":7,"restricted":false}}""", "movie")!!
        assertTrue(quota.unlimited)
        assertFalse(quota.exceededBy(100))
    }
    @Test fun seriesCountSelectedSeasonsAndDoNotUseMovieQuota() {
        val quota = parseRequestQuota("""{"movie":{"limit":100,"remaining":100},"tv":{"limit":5,"remaining":2,"days":7}}""", "tv")!!
        assertFalse(quota.exceededBy(2))
        assertTrue(quota.exceededBy(3))
        assertFalse(quota.exceededBy(0))
        assertEquals(7, quota.days)
    }
    @Test fun restrictedServerResponseBlocksEvenWithoutRemainingCount() {
        val quota = parseRequestQuota("""{"tv":{"limit":5,"restricted":true}}""", "tv")!!
        assertTrue(quota.exceededBy(1))
    }
    @Test fun automaticApprovalRespectsTypeAndIsNotInferredFromRequestPermission() {
        val ordinary = ServiceAccount(ServiceKind.SEERR,"7","Me",permissions = 32)
        assertFalse(ordinary.automaticallyApproves("movie"))
        assertTrue(ordinary.copy(permissions=32L or 256L).automaticallyApproves("movie"))
        assertFalse(ordinary.copy(permissions=32L or 256L).automaticallyApproves("tv"))
        assertTrue(ordinary.copy(permissions=16).automaticallyApproves("tv"))
        assertTrue(ordinary.copy(isAdmin=true).automaticallyApproves("movie"))
        assertFalse(ordinary.copy(isPersonal=false,isAdmin=true).automaticallyApproves("movie"))
    }
    @Test fun onlyVerifiedOwnQuotaIsReadAndFailuresKeepItUnknown() {
        val connection = ServiceConnection(ServiceKind.SEERR,"Fixture","https://seerr.example","fixture", "7",sessionCookie=true)
        val reads = mutableListOf<String>()
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String,String>): HttpResponse {
                reads += url
                return if (url.endsWith("auth/me")) HttpResponse(200,"""{"id":7,"permissions":32}""")
                    else HttpResponse(503,"{}")
            }
            override fun post(url:String,headers:Map<String,String>,jsonBody:String):HttpResponse = error("No writes")
        }
        val rules = RequestRulesClient(transport).load(connection,"7","movie")
        assertTrue(rules.approvalRequired)
        assertTrue(rules.canRequest)
        assertNull(rules.quota)
        assertTrue(reads.last().endsWith("/user/7/quota"))
        reads.clear()
        assertThrows(Exception::class.java) { RequestRulesClient(transport).load(connection,"8","movie") }
        assertEquals(1,reads.size)
    }
}
