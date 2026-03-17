package com.sleekydz86.tellme.aiserver.infrastructure.redis

import com.sleekydz86.tellme.aiserver.aplication.port.RedisLockPort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisLockAdapter(
    private val redisTemplate: RedisTemplate<String, Any>
) : RedisLockPort {

    override fun tryLock(key: String, value: String, ttlSeconds: Long): Boolean {
        val k = "lock:$key"
        val ok = redisTemplate.opsForValue().setIfAbsent(k, value, ttlSeconds, TimeUnit.SECONDS)
        return ok == true
    }

    override fun get(key: String): String? {
        val v = redisTemplate.opsForValue().get("lock:$key") ?: return null
        return v as? String
    }

    override fun delete(key: String): Boolean {
        return redisTemplate.delete("lock:$key") == true
    }

    override fun withLock(key: String, ttlSeconds: Long, block: () -> Unit): Boolean {
        val token = "${System.currentTimeMillis()}-${Thread.currentThread().id}"
        if (!tryLock(key, token, ttlSeconds)) return false
        try {
            block()
            return true
        } finally {
            if (get(key) == token) delete(key)
        }
    }
}
