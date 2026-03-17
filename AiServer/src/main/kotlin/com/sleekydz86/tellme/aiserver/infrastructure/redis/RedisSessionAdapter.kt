package com.sleekydz86.tellme.aiserver.infrastructure.redis

import com.sleekydz86.tellme.aiserver.aplication.port.RedisSessionPort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisSessionAdapter(
    private val redisTemplate: RedisTemplate<String, Any>
) : RedisSessionPort {

    companion object {
        private const val SESSION_PREFIX = "session:"
    }

    override fun set(sessionId: String, field: String, value: String) {
        redisTemplate.opsForHash().put(SESSION_PREFIX + sessionId, field, value)
    }

    override fun get(sessionId: String, field: String): String? {
        val v = redisTemplate.opsForHash().get(SESSION_PREFIX + sessionId, field) ?: return null
        return v as? String
    }

    @Suppress("UNCHECKED_CAST")
    override fun getAll(sessionId: String): Map<String, String> {
        val entries = redisTemplate.opsForHash().entries(SESSION_PREFIX + sessionId) ?: return emptyMap()
        return entries.entries.associate { it.key.toString() to (it.value as? String ?: it.value.toString()) }
    }

    override fun delete(sessionId: String) {
        redisTemplate.delete(SESSION_PREFIX + sessionId)
    }
}
