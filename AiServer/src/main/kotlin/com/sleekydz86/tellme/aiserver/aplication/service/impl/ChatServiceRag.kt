package com.sleekydz86.tellme.aiserver.aplication.service.impl

import com.sleekydz86.tellme.aiserver.aplication.event.DomainEventPublisher
import com.sleekydz86.tellme.aiserver.aplication.port.RedisLockPort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisQueuePort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisSearchPort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisSessionPort
import com.sleekydz86.tellme.aiserver.aplication.port.SSEPort
import com.sleekydz86.tellme.aiserver.aplication.service.ChatService
import com.sleekydz86.tellme.aiserver.aplication.service.DocumentService
import com.sleekydz86.tellme.aiserver.aplication.service.DocumentUsageService
import com.sleekydz86.tellme.aiserver.domain.event.ChatMessageSent
import com.sleekydz86.tellme.aiserver.domain.event.ChatResponseGenerated
import com.sleekydz86.tellme.aiserver.domain.model.ChatEntity
import com.sleekydz86.tellme.global.enums.SSEMsgType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import org.springframework.stereotype.Service

@Service
class ChatServiceRag : ChatService {

    private val chatClient: ChatClient
    private val documentService: DocumentService
    private val ssePort: SSEPort
    private val domainEventPublisher: DomainEventPublisher
    private val redisLockPort: RedisLockPort
    private val redisQueuePort: RedisQueuePort
    private val redisSearchPort: RedisSearchPort
    private val redisSessionPort: RedisSessionPort
    private val documentUsageService: DocumentUsageService

    constructor(
        chatClient: ChatClient,
        documentService: DocumentService,
        ssePort: SSEPort,
        domainEventPublisher: DomainEventPublisher,
        redisLockPort: RedisLockPort,
        redisQueuePort: RedisQueuePort,
        redisSearchPort: RedisSearchPort,
        redisSessionPort: RedisSessionPort,
        documentUsageService: DocumentUsageService
    ) {
        this.chatClient = chatClient
        this.documentService = documentService
        this.ssePort = ssePort
        this.domainEventPublisher = domainEventPublisher
        this.redisLockPort = redisLockPort
        this.redisQueuePort = redisQueuePort
        this.redisSearchPort = redisSearchPort
        this.redisSessionPort = redisSessionPort
        this.documentUsageService = documentUsageService
        this.logger = LoggerFactory.getLogger(javaClass)
    }

    private val logger: Logger?

    companion object {
        private const val RAG_PROMPT_TEMPLATE = """
        다음 제공된 지식 정보를 바탕으로 사용자의 질문에 답변해 주세요.
        규칙: 지식 정보에 답변이 있으면 활용해 정확히 답변하고, 없으면 일반 지식으로 답변하세요.
        "지식에 따르면" 등의 표현은 사용하지 마세요. 친근하고 도움이 되는 톤으로 작성하세요.
        【지식 정보】
        {context}
        【사용자 질문】
        {question}
        위 지식 정보를 바탕으로 답변해 주세요.
    """
    }

    override fun streamOllama(chatEntity: ChatEntity) {}

    override fun streamRag(chatEntity: ChatEntity) {
        val (userId, question, useKnowledgeBase) = chatEntity
        redisQueuePort.pushPendingQuestion(userId, question, "rag")
        redisSearchPort.incrementQueryScore(question)
        redisSessionPort.set(userId, "lastActivity", System.currentTimeMillis().toString())
        domainEventPublisher.publish(ChatMessageSent(userId, question, useKnowledgeBase, "rag"))

        val ran = redisLockPort.withLock("chat:$userId", 30L) {
            try {
                val searchResults = if (useKnowledgeBase) documentService.doSearch(question) else emptyList()
                if (useKnowledgeBase && searchResults.isNotEmpty()) {
                    val objectKeys = searchResults.mapNotNull { it.metadata["objectKey"] as? String }.distinct()
                    documentUsageService.recordUsage(userId, question, objectKeys)
                }
                val prompt = if (searchResults.isNotEmpty()) createRAGPrompt(question, searchResults) else Prompt(question)
                val response = chatClient.call(prompt)
                val content = response.result?.output?.content ?: ""
                if (content.isNotEmpty()) {
                    content.chunked(100).forEach { chunk ->
                        ssePort.sendMsg(userId, chunk, SSEMsgType.ADD.name)
                        Thread.sleep(50)
                    }
                }
                domainEventPublisher.publish(ChatResponseGenerated(userId, content.length, "rag"))
                redisQueuePort.pushChatSaveJob(userId, question, content, "rag")
                ssePort.sendMsg(userId, "done", SSEMsgType.FINISH.name)
            } catch (error: Exception) {
                logger.error("RAG 채팅 오류: userId={}, error={}", userId, error.message, error)
                ssePort.sendMsg(userId, "서비스 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", SSEMsgType.FINISH.name)
            } finally {
                ssePort.close(userId)
            }
        }
        if (!ran) {
            ssePort.sendMsg(userId, "다른 요청 처리 중입니다. 잠시 후 다시 시도해 주세요.", SSEMsgType.FINISH.name)
            ssePort.close(userId)
        }
    }

    private fun createRAGPrompt(question: String, searchResults: List<Document>): Prompt {
        val context = if (searchResults.isNotEmpty()) {
            searchResults.joinToString("\n---\n") { doc ->
                val fileName = doc.metadata["fileName"] ?: "알 수 없는 문서"
                "[$fileName]\n${doc.content}"
            }
        } else "관련된 지식 정보를 찾을 수 없습니다."
        val promptContent = RAG_PROMPT_TEMPLATE.replace("{context}", context).replace("{question}", question)
        return Prompt(promptContent)
    }
}
