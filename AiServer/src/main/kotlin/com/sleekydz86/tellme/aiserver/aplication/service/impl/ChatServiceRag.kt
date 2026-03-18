package com.sleekydz86.tellme.aiserver.aplication.service.impl

import com.sleekydz86.tellme.aiserver.aplication.event.DomainEventPublisher
import com.sleekydz86.tellme.aiserver.aplication.port.RedisLockPort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisQueuePort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisSearchPort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisSessionPort
import com.sleekydz86.tellme.aiserver.aplication.port.SSEPort
import com.sleekydz86.tellme.aiserver.aplication.service.ChatService
import com.sleekydz86.tellme.aiserver.aplication.service.RagAnswerService
import com.sleekydz86.tellme.aiserver.domain.event.ChatMessageSent
import com.sleekydz86.tellme.aiserver.domain.event.ChatResponseGenerated
import com.sleekydz86.tellme.aiserver.domain.model.ChatEntity
import com.sleekydz86.tellme.global.enums.SSEMsgType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ChatServiceRag(
    private val ssePort: SSEPort,
    private val domainEventPublisher: DomainEventPublisher,
    private val redisLockPort: RedisLockPort,
    private val redisQueuePort: RedisQueuePort,
    private val redisSearchPort: RedisSearchPort,
    private val redisSessionPort: RedisSessionPort,
    private val ragAnswerService: RagAnswerService
) : ChatService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun streamOllama(chatEntity: ChatEntity) {}

    override fun streamRag(chatEntity: ChatEntity) {
        val (currentUserName, question, useKnowledgeBase) = chatEntity
        redisQueuePort.pushPendingQuestion(currentUserName, question, "rag")
        redisSearchPort.incrementQueryScore(question)
        redisSessionPort.set(currentUserName, "lastActivity", System.currentTimeMillis().toString())
        domainEventPublisher.publish(ChatMessageSent(currentUserName, question, useKnowledgeBase, "rag"))

        val ran = redisLockPort.withLock("chat:$currentUserName", 30L) {
            try {
                val content = ragAnswerService.answer(
                    currentUserName = currentUserName,
                    question = question,
                    useKnowledgeBase = useKnowledgeBase,
                    strictKnowledgeBase = false
                )

                if (content.isNotEmpty()) {
                    content.chunked(100).forEach { chunk ->
                        ssePort.sendMsg(currentUserName, chunk, SSEMsgType.ADD.name)
                        Thread.sleep(50)
                    }
                }

                domainEventPublisher.publish(ChatResponseGenerated(currentUserName, content.length, "rag"))
                redisQueuePort.pushChatSaveJob(currentUserName, question, content, "rag")
                ssePort.sendMsg(currentUserName, "완료", SSEMsgType.FINISH.name)
            } catch (error: Exception) {
                logger.error("RAG chat failed: userId={}, error={}", currentUserName, error.message, error)
                ssePort.sendMsg(
                    currentUserName,
                    "요청 처리 중 오류가 발생했습니다.",
                    SSEMsgType.FINISH.name
                )
            } finally {
                ssePort.close(currentUserName)
            }
        }

        if (!ran) {
            ssePort.sendMsg(
                currentUserName,
                "다른 요청을 처리 중입니다. 잠시 후 다시 시도해 주세요.",
                SSEMsgType.FINISH.name
            )
            ssePort.close(currentUserName)
        }
    }
}
