package com.sleekydz86.tellme.aiserver.aplication.port

interface RedisSearchPort {
    fun incrementQueryScore(query: String, delta: Double = 1.0)
    fun getTopQueries(limit: Int): List<Pair<String, Double>>
}
