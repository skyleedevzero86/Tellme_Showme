package com.sleekydz86.tellme.aiserver.infrastructure.redis

import com.sleekydz86.tellme.aiserver.aplication.port.RedisSearchPort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.stereotype.Component

@Component
class RedisSearchAdapter(
    private val redisTemplate: RedisTemplate<String, Any>
) : RedisSearchPort {

    companion object {
        private const val SEARCH_QUERIES_KEY = "search:queries"
    }

    override fun incrementQueryScore(query: String, delta: Double) {
        val normalized = query.trim().take(500)
        if (normalized.isBlank()) return
        redisTemplate.opsForZSet().incrementScore(SEARCH_QUERIES_KEY, normalized, delta)
    }

    override fun getTopQueries(limit: Int): List<Pair<String, Double>> {
        val set = redisTemplate.opsForZSet().reverseRangeWithScores(SEARCH_QUERIES_KEY, 0, limit.toLong() - 1)
            ?: return emptyList()
        return set.mapNotNull { t ->
            val score = t?.score?.toDouble() ?: return@mapNotNull null
            val value = t.value
            when (value) {
                is String -> value to score
                else -> value.toString() to score
            }
        }
    }
}
