package com.sleekydz86.tellme.aiserver.presentation.controller

import com.sleekydz86.tellme.aiserver.aplication.port.RedisSearchPort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
class SearchController(
    private val redisSearchPort: RedisSearchPort
) {

    @GetMapping("/top-queries")
    fun topQueries(@RequestParam(defaultValue = "10") limit: Int): List<Map<String, Any>> {
        return redisSearchPort.getTopQueries(limit.coerceIn(1, 100)).map { (query, score) ->
            mapOf("query" to query, "score" to score)
        }
    }
}
