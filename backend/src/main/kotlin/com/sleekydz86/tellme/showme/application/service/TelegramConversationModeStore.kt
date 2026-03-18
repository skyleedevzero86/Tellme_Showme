package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.domain.ConversationMode
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TelegramConversationModeStore(
    private val redisTemplate: StringRedisTemplate
) {
    fun get(chatId: Long): ConversationMode? {
        val raw = redisTemplate.opsForValue().get(key(chatId))
        return ConversationMode.fromAiMode(raw)
    }

    fun activate(chatId: Long, mode: ConversationMode) {
        redisTemplate.opsForValue().set(key(chatId), mode.aiMode, MODE_TTL)
    }

    fun clear(chatId: Long): ConversationMode? {
        val mode = get(chatId)
        redisTemplate.delete(key(chatId))
        return mode
    }

    private fun key(chatId: Long): String = "telegram:conversation-mode:$chatId"

    companion object {
        private val MODE_TTL: Duration = Duration.ofHours(24)
    }
}
