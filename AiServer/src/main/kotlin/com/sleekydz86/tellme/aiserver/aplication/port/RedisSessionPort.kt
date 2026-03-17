package com.sleekydz86.tellme.aiserver.aplication.port

interface RedisSessionPort {
    fun set(sessionId: String, field: String, value: String)
    fun get(sessionId: String, field: String): String?
    fun getAll(sessionId: String): Map<String, String>
    fun delete(sessionId: String)
}
