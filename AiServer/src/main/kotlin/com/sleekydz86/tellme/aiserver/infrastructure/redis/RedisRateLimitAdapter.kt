package com.sleekydz86.tellme.aiserver.infrastructure.redis

import com.sleekydz86.tellme.aiserver.aplication.port.RateLimitPort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisRateLimitAdapter(
    private val redisTemplate: RedisTemplate<String, Any>
) : RateLimitPort {

    companion object {
        private const val RATE_PREFIX = "rate:"
    }

    override fun isAllowed(key: String, limit: Int, windowSeconds: Long): Boolean {
        val k = RATE_PREFIX + key
        val count = redisTemplate.opsForValue().increment(k) ?: 1L
        if (count == 1L) redisTemplate.expire(k, windowSeconds, TimeUnit.SECONDS)
        return count <= limit
    }
}
