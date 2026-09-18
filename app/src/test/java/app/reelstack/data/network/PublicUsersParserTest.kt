package app.reelstack.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicUsersParserTest {

    private val baseUrl = "https://jellyfin.example.com"

    @Test
    fun `parses normal array response with passwords and avatar tags`() {
        val json = """
            [
                {
                    "Name": "Pappa",
                    "Id": "user-1",
                    "HasPassword": true,
                    "PrimaryImageTag": "tag-1"
                },
                {
                    "Name": "Barna",
                    "Id": "user-2",
                    "HasPassword": false,
                    "PrimaryImageTag": "tag-2"
                }
            ]
        """.trimIndent()

        val users = ServicePayloadParser.publicUsers(baseUrl, json)
        assertEquals(2, users.size)

        val pappa = users[0]
        assertEquals("user-1", pappa.id)
        assertEquals("Pappa", pappa.name)
        assertTrue(pappa.hasPassword)
        assertEquals("https://jellyfin.example.com/Users/user-1/Images/Primary?tag=tag-1", pappa.avatarUrl)

        val barna = users[1]
        assertEquals("user-2", barna.id)
        assertEquals("Barna", barna.name)
        assertFalse(barna.hasPassword)
        assertEquals("https://jellyfin.example.com/Users/user-2/Images/Primary?tag=tag-2", barna.avatarUrl)
    }

    @Test
    fun `user without primary image tag has null avatar url`() {
        val json = """
            [
                {
                    "Name": "Ola",
                    "Id": "ola-id",
                    "HasPassword": false
                }
            ]
        """.trimIndent()

        val users = ServicePayloadParser.publicUsers(baseUrl, json)
        assertEquals(1, users.size)
        assertEquals("Ola", users[0].name)
        assertNull(users[0].avatarUrl)
    }

    @Test
    fun `parses items-wrapped response object`() {
        val json = """
            {
                "Items": [
                    {
                        "Name": "Kari",
                        "Id": "kari-id",
                        "HasPassword": true,
                        "PrimaryImageTag": "tag-kari"
                    }
                ],
                "TotalRecordCount": 1
            }
        """.trimIndent()

        val users = ServicePayloadParser.publicUsers(baseUrl, json)
        assertEquals(1, users.size)
        assertEquals("Kari", users[0].name)
        assertEquals("kari-id", users[0].id)
        assertTrue(users[0].hasPassword)
        assertEquals("https://jellyfin.example.com/Users/kari-id/Images/Primary?tag=tag-kari", users[0].avatarUrl)
    }

    @Test
    fun `ignores users with Policy IsDisabled true`() {
        val json = """
            [
                {
                    "Name": "Aktiv",
                    "Id": "active-id",
                    "HasPassword": false,
                    "Policy": { "IsDisabled": false }
                },
                {
                    "Name": "Deaktivert",
                    "Id": "disabled-id",
                    "HasPassword": false,
                    "Policy": { "IsDisabled": true }
                }
            ]
        """.trimIndent()

        val users = ServicePayloadParser.publicUsers(baseUrl, json)
        assertEquals(1, users.size)
        assertEquals("Aktiv", users[0].name)
    }

    @Test
    fun `ignores users with root IsDisabled true`() {
        val json = """
            [
                {
                    "Name": "Gammal",
                    "Id": "old-id",
                    "HasPassword": false,
                    "IsDisabled": true
                }
            ]
        """.trimIndent()

        val users = ServicePayloadParser.publicUsers(baseUrl, json)
        assertTrue(users.isEmpty())
    }

    @Test
    fun `invalid json returns empty list safely without throwing`() {
        val users = ServicePayloadParser.publicUsers(baseUrl, "{ not valid json: true }")
        assertTrue(users.isEmpty())
    }

    @Test
    fun `blank or empty payload returns empty list`() {
        assertTrue(ServicePayloadParser.publicUsers(baseUrl, "").isEmpty())
        assertTrue(ServicePayloadParser.publicUsers(baseUrl, "   ").isEmpty())
    }
}
