package com.sleekydz86.tellme.aiserver.aplication.service

import com.sleekydz86.tellme.aiserver.infrastructure.persistence.DocumentUploadRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class RagAnswerService(
    private val chatClientProvider: ObjectProvider<ChatClient>,
    private val localOllamaCompletionService: LocalOllamaCompletionService,
    private val documentService: DocumentService,
    private val documentUsageService: DocumentUsageService,
    private val documentUploadRepository: DocumentUploadRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun answer(
        currentUserName: String,
        question: String,
        useKnowledgeBase: Boolean = true,
        strictKnowledgeBase: Boolean = false,
        replyContext: String? = null
    ): String {
        val normalizedQuestion = question.trim()
        if (normalizedQuestion.isBlank()) {
            return "질문을 입력해 주세요."
        }

        val searchResults = if (useKnowledgeBase) documentService.doSearch(normalizedQuestion) else emptyList()
        if (useKnowledgeBase && searchResults.isNotEmpty()) {
            val objectKeys = searchResults
                .mapNotNull { it.metadata["objectKey"] as? String }
                .distinct()

            if (objectKeys.isNotEmpty()) {
                documentUsageService.recordUsage(currentUserName, normalizedQuestion, objectKeys)
            }
        }

        if (strictKnowledgeBase && useKnowledgeBase && searchResults.isEmpty()) {
            return if (documentUploadRepository.count() == 0L) {
                NO_DOCUMENTS_MESSAGE
            } else {
                NO_MATCH_MESSAGE
            }
        }

        val prompt = when {
            searchResults.isNotEmpty() -> createRagPrompt(normalizedQuestion, searchResults, replyContext)
            !replyContext.isNullOrBlank() && isClarificationRequest(normalizedQuestion) ->
                createClarificationPrompt(normalizedQuestion, replyContext)
            else -> createGeneralPrompt(normalizedQuestion, replyContext)
        }

        val content = generateResponse(prompt, searchResults)
        if (content.isNotBlank()) {
            return ensureKoreanReply(normalizedQuestion, content, replyContext)
        }

        return if (strictKnowledgeBase && useKnowledgeBase) {
            NO_MATCH_MESSAGE
        } else {
            buildKoreanFallback(normalizedQuestion, replyContext)
        }
    }

    private fun createRagPrompt(question: String, searchResults: List<Document>, replyContext: String?): Prompt {
        val context = searchResults.joinToString("\n---\n") { doc ->
            val fileName = doc.metadata["fileName"] ?: "이름 없는 문서"
            "[$fileName]\n${doc.text.orEmpty()}"
        }

        val promptContent = RAG_PROMPT_TEMPLATE
            .replace("{context}", context)
            .replace("{replyContext}", replyContextSection(replyContext))
            .replace("{question}", question)

        return Prompt(promptContent)
    }

    private fun createGeneralPrompt(question: String, replyContext: String?): Prompt {
        val promptContent = GENERAL_PROMPT_TEMPLATE
            .replace("{replyContext}", replyContextSection(replyContext))
            .replace("{question}", question)
        return Prompt(promptContent)
    }

    private fun createClarificationPrompt(question: String, replyContext: String): Prompt =
        Prompt(
            """
            You are helping a Korean user understand a previous assistant message.
            The user replied to that previous message and is asking what it means.
            Explain the previous message directly in plain Korean.
            If the previous message contains Chinese, mixed language, or broken wording, rewrite its intended meaning in natural Korean.
            Do not answer generically. Focus on the previous message first.
            Keep the explanation concise but specific.
            Reply only in Korean.

            Previous message:
            $replyContext

            User follow-up:
            $question
            """.trimIndent()
        )

    private fun replyContextSection(replyContext: String?): String =
        replyContext?.trim()?.takeIf { it.isNotBlank() }
            ?.let { "Previous replied message:\n$it\n\n" }
            .orEmpty()

    private fun generateResponse(prompt: Prompt, searchResults: List<Document>): String {
        val chatClient = chatClientProvider.ifAvailable
        if (chatClient != null) {
            return chatClient.prompt(prompt).call().content()?.trim().orEmpty()
        }

        logger.warn("No ChatClient bean is configured. Trying local Ollama generate API.")
        val ollamaResponse = localOllamaCompletionService.generate(prompt.contents)
        if (ollamaResponse.isNotBlank()) {
            return ollamaResponse
        }

        logger.warn("Local Ollama generate API was unavailable. Falling back to reference-only response.")
        if (searchResults.isEmpty()) {
            return ""
        }

        return buildString {
            appendLine("업로드한 문서를 기준으로 찾은 내용입니다.")
            appendLine()
            append(
                searchResults.take(3).joinToString("\n\n") { doc ->
                    val fileName = doc.metadata["fileName"] ?: "Unknown document"
                    "[$fileName]\n${doc.text.orEmpty()}"
                }
            )
        }.trim()
    }

    private fun ensureKoreanReply(question: String, rawReply: String, replyContext: String?): String {
        val trimmed = rawReply.trim()
        if (trimmed.isBlank()) {
            return buildKoreanFallback(question, replyContext)
        }
        if (containsHangul(trimmed)) {
            return trimmed
        }

        logger.warn("Non-Korean general reply detected. Rewriting to Korean.")
        val rewritten = localOllamaCompletionService.generate(
            """
            Rewrite the following assistant reply in natural Korean.
            Reply only in Korean.
            Do not use Chinese.
            Preserve the original meaning and keep it concise.

            Previous message:
            ${replyContext ?: "(none)"}

            User question:
            $question

            Assistant draft:
            $trimmed
            """.trimIndent()
        ).trim()

        return when {
            rewritten.isBlank() -> buildKoreanFallback(question, replyContext)
            containsHangul(rewritten) -> rewritten
            else -> buildKoreanFallback(question, replyContext)
        }
    }

    private fun buildKoreanFallback(question: String, replyContext: String? = null): String =
        when {
            !replyContext.isNullOrBlank() && isClarificationRequest(question) ->
                "방금 답변의 뜻을 다시 쉽게 풀어드릴게요. 직전에 답글로 단 문장을 기준으로 설명해야 하는 상황으로 이해했어요. 특히 헷갈린 표현을 한 문장만 더 짚어 주시면 그 부분부터 정확히 설명할게요."

            question.contains("코드") || question.contains("버그") || question.contains("에러") ->
                "코드 관련해서 도와드릴 수 있어요. 에러 메시지나 기대한 동작을 한두 줄만 더 알려 주세요."

            question.contains("배고파") ->
                "배고프시군요. 지금 바로 먹을 수 있는 것부터 먼저 챙겨 보세요. 원하시면 간단한 메뉴도 같이 골라드릴게요."

            question.contains("힘들") || question.contains("우울") || question.contains("슬프") ->
                "많이 버거우셨겠어요. 지금 가장 크게 걸리는 부분부터 편하게 말해 주세요."

            question.endsWith("?") || question.contains("왜") || question.contains("어떻게") || question.contains("뭐") ->
                "좋은 질문이에요. 핵심부터 차근차근 같이 풀어볼게요."

            else ->
                "계속 말씀해 주세요. 질문하신 내용에 맞춰 이어서 도와드릴게요."
        }

    private fun isClarificationRequest(question: String): Boolean {
        val normalized = question.lowercase()
        return listOf(
            "무슨말",
            "무슨 말",
            "뭔말",
            "무슨뜻",
            "무슨 뜻",
            "설명해",
            "설명 좀",
            "쉽게 말",
            "쉽게 설명",
            "다시 말",
            "다시 설명",
            "번역해",
            "이게 뭐",
            "이건 뭐",
            "무슨 이야기"
        ).any { normalized.contains(it) }
    }

    private fun containsHangul(text: String): Boolean =
        text.any { ch -> ch in '\uAC00'..'\uD7A3' || ch in '\u3131'..'\u318E' }

    companion object {
        private const val GENERAL_PROMPT_TEMPLATE = """
        You are a helpful assistant for a Korean user.
        Always answer in natural Korean.
        Never answer in Chinese.
        Do not switch to English unless the user explicitly asks for English mode.
        Keep the reply concise, direct, and conversational.
        If the user is replying to a previous message, use that previous message as the primary context.
        If the user asks what the previous message means, explain that previous message directly in Korean instead of giving a generic answer.

        User message:
        {replyContext}{question}
        """

        private const val RAG_PROMPT_TEMPLATE = """
        아래 참고 자료를 우선 사용해서 사용자의 질문에 답하세요.
        참고 자료에 없는 내용은 추측하지 말고, 문서에서 찾지 못했다고 분명하게 말하세요.
        답변은 반드시 자연스럽고 이해하기 쉬운 한국어로만 작성하세요.
        중국어로 답하지 마세요.
        사용자가 이전 메시지에 대한 뜻을 묻는 경우에는 그 메시지를 먼저 풀어서 설명하세요.

        참고 자료:
        {context}

        사용자 질문:
        {replyContext}{question}
        """

        private const val NO_DOCUMENTS_MESSAGE =
            "아직 업로드한 문서가 없습니다. http://localhost:3000/channel 에서 문서를 업로드한 뒤 다시 /search 해 주세요."

        private const val NO_MATCH_MESSAGE =
            "업로드한 문서에서 관련 내용을 찾지 못했습니다. 다른 키워드로 다시 /search 해 주세요."
    }
}
