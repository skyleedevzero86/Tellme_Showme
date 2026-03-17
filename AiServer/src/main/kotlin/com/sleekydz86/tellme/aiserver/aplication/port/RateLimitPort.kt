package com.sleekydz86.tellme.aiserver.aplication.port

interface RateLimitPort {
    fun isAllowed(key: String, limit: Int, windowSeconds: Long): Boolean
}
