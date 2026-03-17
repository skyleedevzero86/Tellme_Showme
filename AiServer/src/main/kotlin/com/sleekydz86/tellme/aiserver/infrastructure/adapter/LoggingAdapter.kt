package com.sleekydz86.tellme.aiserver.infrastructure.adapter

import com.sleekydz86.tellme.aiserver.aplication.port.LoggingPort
import com.sleekydz86.tellme.aiserver.domain.event.ChatMessageSent
import com.sleekydz86.tellme.aiserver.domain.event.ChatResponseGenerated
import com.sleekydz86.tellme.aiserver.domain.event.DocumentUploaded
import com.sleekydz86.tellme.aiserver.domain.event.DomainEvent
import com.sleekydz86.tellme.aiserver.domain.event.SseConnected
import com.sleekydz86.tellme.aiserver.domain.event.SseDisconnected
import com.sleekydz86.tellme.aiserver.domain.model.AuditLogEntity
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LoggingAdapter(
    private val auditLogRepository: AuditLogRepository
) : LoggingPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun log(event: DomainEvent) {
        val (action, userId, details) = when (event) {
            is DocumentUploaded -> Triple("DOCUMENT_UPLOADED", event.userId, "fileName=${event.fileName}, objectKey=${event.objectKey}, contentType=${event.contentType}")
            is ChatMessageSent -> Triple("CHAT_MESSAGE_SENT", event.userId, "useKnowledge=${event.useKnowledge}, mode=${event.mode}")
            is ChatResponseGenerated -> Triple("CHAT_RESPONSE_GENERATED", event.userId, "responseLength=${event.responseLength}, mode=${event.mode}")
            is SseConnected -> Triple("SSE_CONNECTED", event.userId, null)
            is SseDisconnected -> Triple("SSE_DISCONNECTED", event.userId, null)
        }
        logger.info("[감사 로그] action={} userId={} details={}", action, userId, details)
        try {
            auditLogRepository.save(AuditLogEntity(action = action, userId = userId, details = details))
        } catch (e: Exception) {
            logger.warn("감사 로그 저장 실패: action={}", action, e)
        }
    }
}
