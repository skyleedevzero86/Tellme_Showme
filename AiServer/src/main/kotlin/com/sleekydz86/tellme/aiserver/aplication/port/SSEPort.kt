package com.sleekydz86.tellme.aiserver.aplication.port

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface SSEPort {
    fun addConnection(userId: String, emitter: SseEmitter)
    fun sendMsg(userId: String, message: String, type: String)
    fun close(userId: String)
}
