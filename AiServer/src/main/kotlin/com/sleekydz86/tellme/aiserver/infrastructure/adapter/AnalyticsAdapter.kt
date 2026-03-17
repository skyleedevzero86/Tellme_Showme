package com.sleekydz86.tellme.aiserver.infrastructure.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.tellme.aiserver.aplication.port.NotificationPort
import com.sleekydz86.tellme.aiserver.domain.event.ChatMessageSent
import com.sleekydz86.tellme.aiserver.domain.event.ChatResponseGenerated
import com.sleekydz86.tellme.aiserver.domain.event.DocumentUploaded
import com.sleekydz86.tellme.aiserver.domain.event.DomainEvent
import com.sleekydz86.tellme.aiserver.domain.event.SseConnected
import com.sleekydz86.tellme.aiserver.domain.event.SseDisconnected
import com.sleekydz86.tellme.aiserver.domain.model.NotificationEntity
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NotificationAdapter(
    private val notificationRepository: NotificationRepository,
    private val objectMapper: ObjectMapper
) : NotificationPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun notify(event: DomainEvent) {
        val (type, userId, payload) = when (event) {
            is DocumentUploaded -> Triple("DOCUMENT_UPLOADED", event.userId, runCatching { objectMapper.writeValueAsString(mapOf("fileName" to event.fileName, "objectKey" to event.objectKey, "contentType" to event.contentType)) }.getOrNull())
            is ChatMessageSent -> Triple("CHAT_MESSAGE_SENT", event.userId, runCatching { objectMapper.writeValueAsString(mapOf("messageLength" to event.message.length, "useKnowledge" to event.useKnowledge, "mode" to event.mode)) }.getOrNull())
            is ChatResponseGenerated -> Triple("CHAT_RESPONSE_GENERATED", event.userId, runCatching { objectMapper.writeValueAsString(mapOf("responseLength" to event.responseLength, "mode" to event.mode)) }.getOrNull())
            is SseConnected -> Triple("SSE_CONNECTED", event.userId, null)
            is SseDisconnected -> Triple("SSE_DISCONNECTED", event.userId, null)
        }
        try {
            notificationRepository.save(NotificationEntity(type = type, userId = userId, payload = payload))
        } catch (e: Exception) {
            logger.warn("알림 저장 실패: type={}, userId={}", type, userId, e)
        }
    }
}
