package com.sleekydz86.tellme.aiserver.aplication.service

import com.sleekydz86.tellme.aiserver.aplication.event.DomainEventPublisher
import com.sleekydz86.tellme.aiserver.aplication.port.RedisLockPort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisQueuePort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisSessionPort
import com.sleekydz86.tellme.aiserver.domain.event.ChatMessageSent
import com.sleekydz86.tellme.aiserver.domain.event.ChatResponseGenerated
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class ModeAnswerService(
    private val chatClientProvider: ObjectProvider<ChatClient>,
    private val localOllamaCompletionService: LocalOllamaCompletionService,
    private val domainEventPublisher: DomainEventPublisher,
    private val redisLockPort: RedisLockPort,
    private val redisQueuePort: RedisQueuePort,
    private val redisSessionPort: RedisSessionPort
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun answer(currentUserName: String, message: String, mode: String): String {
        val conversationMode = AssistantConversationMode.from(mode)
            ?: return UNSUPPORTED_MODE_MESSAGE

        val normalizedMessage = message.trim()
        if (normalizedMessage.isBlank()) {
            return conversationMode.emptyMessage
        }

        redisQueuePort.pushPendingQuestion(currentUserName, normalizedMessage, conversationMode.modeName)
        redisSessionPort.set(currentUserName, "lastActivity", System.currentTimeMillis().toString())
        redisSessionPort.set(currentUserName, "conversationMode", conversationMode.modeName)
        domainEventPublisher.publish(ChatMessageSent(currentUserName, normalizedMessage, false, conversationMode.modeName))

        var answer = ""
        val ran = redisLockPort.withLock("chat:$currentUserName", 30L) {
            answer = try {
                generateResponse(conversationMode, normalizedMessage)
                    .ifBlank { conversationMode.fallbackReply(normalizedMessage) }
            } catch (error: Exception) {
                logger.error(
                    "Mode chat failed: userId={}, mode={}, error={}",
                    currentUserName,
                    conversationMode.modeName,
                    error.message,
                    error
                )
                ERROR_MESSAGE
            }
        }

        if (!ran) {
            return BUSY_MESSAGE
        }

        if (answer.isBlank()) {
            answer = conversationMode.fallbackReply(normalizedMessage)
        }

        domainEventPublisher.publish(ChatResponseGenerated(currentUserName, answer.length, conversationMode.modeName))
        redisQueuePort.pushChatSaveJob(currentUserName, normalizedMessage, answer, conversationMode.modeName)
        return answer
    }

    private fun generateResponse(mode: AssistantConversationMode, message: String): String {
        val chatClient = chatClientProvider.ifAvailable
        if (chatClient != null) {
            return chatClient.prompt(mode.toPrompt(message)).call().content()?.trim().orEmpty()
        }

        logger.warn("No ChatClient bean is configured for mode chat. Trying local Ollama generate API. mode={}", mode.modeName)
        val ollamaResponse = localOllamaCompletionService.generate(mode.toPrompt(message).contents)
        if (ollamaResponse.isNotBlank()) {
            return ollamaResponse
        }

        logger.warn("Local Ollama generate API was unavailable. Falling back to template response. mode={}", mode.modeName)
        return mode.fallbackReply(message)
    }

    private enum class AssistantConversationMode(
        val modeName: String,
        val emptyMessage: String,
        private val promptTemplate: String
    ) {
        ENG(
            modeName = "eng",
            emptyMessage = "영어로 대화할 내용을 입력해 주세요.",
            promptTemplate = """
                You are a friendly English conversation partner.
                Reply only in natural English.
                Keep the answer concise, warm, and easy to understand.
                If the user writes in Korean, still answer in English.

                User message:
                {message}
            """.trimIndent()
        ),
        GOD(
            modeName = "god",
            emptyMessage = "명언 느낌으로 답할 주제를 입력해 주세요.",
            promptTemplate = """
                You are a warm inspirational assistant.
                Reply in Korean.
                Start with one short quote, proverb, or wisdom sentence.
                After that, add one or two short lines that connect the quote to the user's message.
                Keep it gentle, concise, and uplifting.

                User message:
                {message}
            """.trimIndent()
        );

        fun toPrompt(message: String): Prompt =
            Prompt(promptTemplate.replace("{message}", message))

        fun fallbackReply(message: String): String =
            when (this) {
                ENG -> buildFallbackEnglishReply(message)
                GOD -> buildFallbackGodReply(message)
            }

        companion object {
            fun from(mode: String): AssistantConversationMode? =
                entries.find { it.modeName.equals(mode.trim(), ignoreCase = true) }

            private fun buildFallbackEnglishReply(message: String): String {
                val normalized = message.trim()
                return when {
                    normalized.endsWith("?") ->
                        "That's a thoughtful question. My short answer is to take it step by step and keep moving forward."

                    normalized.length <= 12 ->
                        "I hear you. Tell me a little more, and I will keep replying in English."

                    else ->
                        "Thanks for sharing. Let's keep talking in English, one step at a time."
                }
            }

            private fun buildFallbackGodReply(message: String): String {
                val quotes = listOf(
                    "작은 걸음도 멈추지 않으면 결국 길이 됩니다.",
                    "비 온 뒤에 땅이 굳듯, 어려움 뒤에는 단단함이 남습니다.",
                    "오늘의 인내가 내일의 힘이 됩니다.",
                    "천천히 가도 멈추지 않으면 앞으로 갑니다."
                )
                val quote = quotes[(message.hashCode() and Int.MAX_VALUE) % quotes.size]
                return "$quote\n지금의 고민도 너무 급히 결론내리지 말고 한 걸음씩 풀어가 보세요."
            }
        }
    }

    companion object {
        private const val UNSUPPORTED_MODE_MESSAGE = "지원하지 않는 대화 모드입니다."
        private const val BUSY_MESSAGE = "다른 요청을 처리 중입니다. 잠시 후 다시 시도해 주세요."
        private const val ERROR_MESSAGE = "요청을 처리하는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
    }
}
