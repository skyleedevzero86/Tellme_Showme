package com.sleekydz86.tellme.showme.application.service

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

@Component
class FrontendChatSessionIdCodec {

    fun toChatId(sessionId: String): Long {
        val normalized = sessionId.trim().ifBlank { "frontend-anonymous" }
        val crc32 = CRC32()
        crc32.update(normalized.toByteArray(StandardCharsets.UTF_8))
        return FRONTEND_CHAT_ID_BASE + crc32.value
    }

    companion object {
        private const val FRONTEND_CHAT_ID_BASE = 8_000_000_000_000_000_000L
    }
}
