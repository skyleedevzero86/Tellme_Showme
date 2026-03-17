package com.sleekydz86.tellme.aiserver.aplication.port

interface RedisLockPort {
    fun tryLock(key: String, value: String, ttlSeconds: Long): Boolean
    fun get(key: String): String?
    fun delete(key: String): Boolean
    fun withLock(key: String, ttlSeconds: Long, block: () -> Unit): Boolean
}
