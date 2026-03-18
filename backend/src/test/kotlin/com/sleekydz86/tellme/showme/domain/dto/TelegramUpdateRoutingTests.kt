package com.sleekydz86.tellme.showme.domain.dto

import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TelegramUpdateRoutingTests {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `webhook channel post resolves to incoming message`() {
        val json = """
            {
              "update_id": 1001,
              "channel_post": {
                "message_id": 77,
                "sender_chat": {
                  "id": -1001234,
                  "title": "Notice Channel",
                  "username": "notice_channel"
                },
                "chat": {
                  "id": -1001234,
                  "type": "channel",
                  "title": "Notice Channel",
                  "username": "notice_channel"
                },
                "date": 1710000000,
                "text": "channel hello"
              }
            }
        """.trimIndent()

        val update = objectMapper.readValue(json, WebhookUpdate::class.java)

        assertEquals("channel_post", update.incomingMessageType())
        assertEquals("channel hello", update.incomingMessage()?.text)
        assertEquals("Notice Channel", update.incomingMessage()?.senderDisplayName())
    }

    @Test
    fun `getUpdates channel post resolves to incoming message`() {
        val json = """
            {
              "ok": true,
              "result": [
                {
                  "update_id": 1002,
                  "channel_post": {
                    "message_id": 88,
                    "sender_chat": {
                      "id": -1005678,
                      "title": "Poll Channel",
                      "username": "poll_channel"
                    },
                    "chat": {
                      "id": -1005678,
                      "type": "channel",
                      "title": "Poll Channel",
                      "username": "poll_channel"
                    },
                    "date": 1710000100,
                    "text": "poll hello"
                  }
                }
              ]
            }
        """.trimIndent()

        val update = objectMapper.readValue(json, TelegramUpdate::class.java)
        val first = update.result?.first()

        assertEquals("channel_post", first?.incomingMessageType())
        assertEquals("poll hello", first?.incomingMessage()?.text)
        assertEquals("Poll Channel", first?.incomingMessage()?.senderDisplayName())
    }
}
