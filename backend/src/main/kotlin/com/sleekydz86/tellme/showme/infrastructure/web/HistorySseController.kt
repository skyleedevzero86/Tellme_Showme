package com.sleekydz86.tellme.showme.infrastructure.web

import com.sleekydz86.tellme.showme.application.service.HistorySseService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class HistorySseController(
    private val historySseService: HistorySseService
) {
    @GetMapping(value = ["/message_history_events.do"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun connect(): SseEmitter = historySseService.connect()
}
