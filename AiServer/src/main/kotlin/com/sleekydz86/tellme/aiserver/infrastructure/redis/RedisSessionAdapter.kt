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
        redisTemplate.opsForHash<String, String>().put(sessionKey(sessionId), field, value)
    }

    override fun get(sessionId: String, field: String): String? {
        return redisTemplate.opsForHash<String, String>().get(sessionKey(sessionId), field)
    }

    override fun getAll(sessionId: String): Map<String, String> {
        return redisTemplate.opsForHash<String, String>().entries(sessionKey(sessionId))
    }

    override fun delete(sessionId: String) {
        redisTemplate.delete(sessionKey(sessionId))
    }

    private fun sessionKey(sessionId: String): String = SESSION_PREFIX + sessionId
}
