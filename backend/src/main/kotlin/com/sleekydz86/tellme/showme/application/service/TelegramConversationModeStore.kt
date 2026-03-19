package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.domain.ConversationMode
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class TelegramConversationModeStore(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(TelegramConversationModeStore::class.java)
    private val fallbackModes = ConcurrentHashMap<Long, StoredMode>()

    fun get(chatId: Long): ConversationMode? {
        val redisMode = runCatching {
            redisTemplate.opsForValue().get(key(chatId))
        }.onFailure { e ->
            log.warn("Failed to read conversation mode from Redis: chatId={}", chatId, e)
        }.getOrNull()?.let { ConversationMode.fromAiMode(it) }

        if (redisMode != null) {
            fallbackModes[chatId] = StoredMode(redisMode, Instant.now().plus(MODE_TTL))
            return redisMode
        }

        val fallback = fallbackModes[chatId]
        if (fallback == null) {
            return null
        }
        if (fallback.expiresAt.isBefore(Instant.now())) {
            fallbackModes.remove(chatId)
            return null
        }
        return fallback.mode
    }

    fun activate(chatId: Long, mode: ConversationMode) {
        fallbackModes[chatId] = StoredMode(mode, Instant.now().plus(MODE_TTL))
        runCatching {
            redisTemplate.opsForValue().set(key(chatId), mode.aiMode, MODE_TTL)
        }.onFailure { e ->
            log.warn("Failed to store conversation mode in Redis: chatId={}, mode={}", chatId, mode.aiMode, e)
        }
    }

    fun clear(chatId: Long): ConversationMode? {
        val mode = get(chatId)
        fallbackModes.remove(chatId)
        runCatching {
            redisTemplate.delete(key(chatId))
        }.onFailure { e ->
            log.warn("Failed to clear conversation mode in Redis: chatId={}", chatId, e)
        }
        return mode
    }

    private fun key(chatId: Long): String = "telegram:conversation-mode:$chatId"

    private data class StoredMode(
        val mode: ConversationMode,
        val expiresAt: Instant
    )

    companion object {
        private val MODE_TTL: Duration = Duration.ofHours(24)
    }
}
