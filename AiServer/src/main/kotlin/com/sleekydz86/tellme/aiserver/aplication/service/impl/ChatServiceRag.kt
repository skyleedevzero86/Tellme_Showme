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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import org.springframework.stereotype.Service

@Service
class ChatServiceRag(
    private val chatClientProvider: ObjectProvider<ChatClient>,
    private val documentService: DocumentService,
    private val ssePort: SSEPort,
    private val domainEventPublisher: DomainEventPublisher,
    private val redisLockPort: RedisLockPort,
    private val redisQueuePort: RedisQueuePort,
    private val redisSearchPort: RedisSearchPort,
    private val redisSessionPort: RedisSessionPort,
    private val documentUsageService: DocumentUsageService
) : ChatService {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RAG_PROMPT_TEMPLATE = """
        아래 참고 자료를 활용하여 사용자 질문에 답하세요.
        참고 자료가 관련 있으면 이를 우선하여 사용하고,
        부족하면 일반 지식으로 답하되, 답변이 불완전할 수 있음을 밝히세요.

        참고 자료:
        {context}

        사용자 질문:
        {question}
        """
    }

    override fun streamOllama(chatEntity: ChatEntity) {}

    override fun streamRag(chatEntity: ChatEntity) {
        val (currentUserName, question, useKnowledgeBase) = chatEntity
        redisQueuePort.pushPendingQuestion(currentUserName, question, "rag")
        redisSearchPort.incrementQueryScore(question)
        redisSessionPort.set(currentUserName, "lastActivity", System.currentTimeMillis().toString())
        domainEventPublisher.publish(ChatMessageSent(currentUserName, question, useKnowledgeBase, "rag"))

        val ran = redisLockPort.withLock("chat:$currentUserName", 30L) {
            try {
                val searchResults = if (useKnowledgeBase) documentService.doSearch(question) else emptyList()
                if (useKnowledgeBase && searchResults.isNotEmpty()) {
                    val objectKeys = searchResults
                        .mapNotNull { it.metadata["objectKey"] as? String }
                        .distinct()
                    documentUsageService.recordUsage(currentUserName, question, objectKeys)
                }

                val prompt = if (searchResults.isNotEmpty()) {
                    createRagPrompt(question, searchResults)
                } else {
                    Prompt(question)
                }

                val content = generateResponse(prompt, searchResults)
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
                logger.error("RAG 채팅 실패: userId={}, error={}", currentUserName, error.message, error)
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
                "다른 요청이 이미 처리 중입니다.",
                SSEMsgType.FINISH.name
            )
            ssePort.close(currentUserName)
        }
    }

    private fun createRagPrompt(question: String, searchResults: List<Document>): Prompt {
        val context = if (searchResults.isNotEmpty()) {
            searchResults.joinToString("\n---\n") { doc ->
                val fileName = doc.metadata["fileName"] ?: "알 수 없는 문서"
                "[$fileName]\n${doc.text}"
            }
        } else {
            "관련 참고 정보를 찾을 수 없습니다."
        }

        val promptContent = RAG_PROMPT_TEMPLATE
            .replace("{context}", context)
            .replace("{question}", question)
        return Prompt(promptContent)
    }

    private fun generateResponse(prompt: Prompt, searchResults: List<Document>): String {
        val chatClient = chatClientProvider.ifAvailable
        if (chatClient != null) {
            return chatClient.prompt(prompt).call().content() ?: ""
        }

        logger.warn("No ChatClient bean is configured. Falling back to a reference-only RAG response.")
        if (searchResults.isEmpty()) {
            return "No chat model is configured, and no relevant reference information was found."
        }

        val references = searchResults.take(3).joinToString("\n\n") { doc ->
            val fileName = doc.metadata["fileName"] ?: "Unknown document"
            "[$fileName]\n${doc.text.orEmpty()}"
        }
        return buildString {
            appendLine("No chat model is currently configured, so this response is based on reference text only.")
            appendLine()
            append(references)
        }
    }
}
