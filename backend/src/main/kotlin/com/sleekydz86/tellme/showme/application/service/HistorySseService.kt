package com.sleekydz86.tellme.showme.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class HistorySseService {
    private val log = LoggerFactory.getLogger(HistorySseService::class.java)
    private val emitters = ConcurrentHashMap<String, SseEmitter>()

    fun connect(): SseEmitter {
        val emitter = SseEmitter(0L)
        val emitterId = UUID.randomUUID().toString()
        emitters[emitterId] = emitter

        emitter.onCompletion { emitters.remove(emitterId) }
        emitter.onTimeout {
            emitters.remove(emitterId)
            emitter.complete()
        }
        emitter.onError {
            emitters.remove(emitterId)
            emitter.complete()
        }

        try {
            emitter.send(
                SseEmitter.event()
                    .name(HISTORY_REFRESH_EVENT)
                    .data("connected")
            )
        } catch (e: IOException) {
            emitters.remove(emitterId)
            emitter.completeWithError(e)
        }

        return emitter
    }

    fun publishMessageSaved() = publish("message")

    fun publishFileSaved() = publish("file")

    private fun publish(kind: String) {
        val failedIds = mutableListOf<String>()
        emitters.forEach { (id, emitter) ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(HISTORY_REFRESH_EVENT)
                        .data(kind)
                )
            } catch (e: Exception) {
                failedIds += id
                log.debug("History SSE send failed: id={}", id, e)
            }
        }
        failedIds.forEach { emitters.remove(it) }
    }

    companion object {
        const val HISTORY_REFRESH_EVENT = "history-refresh"
    }
}
