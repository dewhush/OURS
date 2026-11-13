package app.ours.data.api

import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiParsingTest {

    @Test
    fun `parses paused partner location with null coordinates`() {
        val raw = """
            {
              "user_id": 2,
              "paused": true,
              "last_seen_at": 1731500005000,
              "lat": null, "lon": null, "accuracy_m": null, "speed_mps": null,
              "bearing": null, "battery_pct": null, "charging": null, "recorded_at": null
            }
        """.trimIndent()
        val loc = OursJson.decodeFromString<PartnerLocationDto>(raw)
        assertEquals(2L, loc.userId)
        assertTrue(loc.paused)
        assertNull(loc.lat)
        assertNull(loc.batteryPct)
    }

    @Test
    fun `parses location upload response with piggyback events`() {
        val raw = """
            {
              "accepted": 2,
              "paused": false,
              "events": [
                {
                  "id": 42,
                  "type": "place",
                  "created_at": 1731500000000,
                  "actor_id": 2,
                  "data": {
                    "place_id": 3,
                    "place_name": "Home",
                    "transition": "enter",
                    "lat": -6.2,
                    "lon": 106.8
                  }
                }
              ]
            }
        """.trimIndent()
        val resp = OursJson.decodeFromString<LocationsUploadResponse>(raw)
        assertEquals(2, resp.accepted)
        assertEquals(1, resp.events.size)
        val placeEvent = OursJson.decodeFromJsonElement<EventPlaceData>(resp.events.first().data)
        assertEquals("Home", placeEvent.placeName)
        assertEquals("enter", placeEvent.transition)
    }

    @Test
    fun `parses memories response with null next_before`() {
        val raw = """
            {
              "items": [
                {
                  "id": 5,
                  "author_id": 1,
                  "photo_id": "3f9c0a",
                  "note": "coffee",
                  "lat": -6.2,
                  "lon": 106.8,
                  "place_name": "Kopi Kenangan",
                  "created_at": 1731500000000,
                  "expires_at": 1731586400000,
                  "highlighted": true,
                  "highlighted_by": 2,
                  "highlighted_at": 1731500500000,
                  "replies": []
                }
              ],
              "next_before": null
            }
        """.trimIndent()
        val resp = OursJson.decodeFromString<MemoriesResponse>(raw)
        assertEquals(1, resp.items.size)
        assertTrue(resp.items.first().highlighted)
        assertNull(resp.nextBefore)
    }

    @Test
    fun `parses messages response and chat_message event`() {
        val raw = """
            {
              "items": [
                {
                  "id": 12,
                  "author_id": 1,
                  "text": "on my way!",
                  "photo_id": null,
                  "reply_to_message_id": null,
                  "reply_to_checkin_id": 5,
                  "created_at": 1731500200000
                }
              ],
              "next_before": 1731500100000
            }
        """.trimIndent()
        val resp = OursJson.decodeFromString<MessagesResponse>(raw)
        assertEquals(1, resp.items.size)
        assertEquals("on my way!", resp.items.first().text)
        assertEquals(5L, resp.items.first().replyToCheckInId)
        assertEquals(1731500100000L, resp.nextBefore)

        val eventRaw = """
            {
              "id": 77,
              "type": "chat_message",
              "created_at": 1731500200000,
              "actor_id": 2,
              "data": {
                "message": {
                  "id": 13,
                  "author_id": 2,
                  "text": "see you!",
                  "photo_id": null,
                  "reply_to_message_id": 12,
                  "reply_to_checkin_id": null,
                  "created_at": 1731500205000
                }
              }
            }
        """.trimIndent()
        val ev = OursJson.decodeFromString<EventDto>(eventRaw)
        val payload = OursJson.decodeFromJsonElement<EventChatMessageData>(ev.data)
        assertEquals(13L, payload.message.id)
        assertEquals(12L, payload.message.replyToMessageId)
    }
}
