package com.sleekydz86.tellme.aiserver.infrastructure.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.tellme.aiserver.aplication.port.AnalyticsPort
import com.sleekydz86.tellme.aiserver.domain.event.ChatMessageSent
import com.sleekydz86.tellme.aiserver.domain.event.ChatResponseGenerated
import com.sleekydz86.tellme.aiserver.domain.event.DocumentUploaded
import com.sleekydz86.tellme.aiserver.domain.event.DomainEvent
import com.sleekydz86.tellme.aiserver.domain.event.SseConnected
import com.sleekydz86.tellme.aiserver.domain.event.SseDisconnected
import com.sleekydz86.tellme.aiserver.domain.model.AnalyticsEventEntity
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.AnalyticsEventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AnalyticsAdapter(
    private val analyticsEventRepository: AnalyticsEventRepository,
    private val objectMapper: ObjectMapper
) : AnalyticsPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun record(event: DomainEvent) {
        val (eventType, userId, payload) = when (event) {
            is DocumentUploaded -> Triple("document_uploaded", event.userId, runCatching { objectMapper.writeValueAsString(mapOf("fileName" to event.fileName, "objectKey" to event.objectKey, "contentType" to event.contentType)) }.getOrNull())
            is ChatMessageSent -> Triple("chat_message_sent", event.userId, runCatching { objectMapper.writeValueAsString(mapOf("useKnowledge" to event.useKnowledge, "mode" to event.mode)) }.getOrNull())
            is ChatResponseGenerated -> Triple("chat_response_generated", event.userId, runCatching { objectMapper.writeValueAsString(mapOf("responseLength" to event.responseLength, "mode" to event.mode)) }.getOrNull())
            is SseConnected -> Triple("sse_connected", event.userId, null)
            is SseDisconnected -> Triple("sse_disconnected", event.userId, null)
        }
        try {
            analyticsEventRepository.save(
                AnalyticsEventEntity(
                    eventType = eventType,
                    userId = userId,
                    payload = payload
                )
            )
        } catch (e: Exception) {
            logger.warn("분석 기록 실패: eventType={}", eventType, e)
        }
    }
}
