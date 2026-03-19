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
        strictKnowledgeBase: Boolean = false
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

        val prompt = if (searchResults.isNotEmpty()) {
            createRagPrompt(normalizedQuestion, searchResults)
        } else if (!useKnowledgeBase) {
            createGeneralPrompt(normalizedQuestion)
        } else {
            Prompt(normalizedQuestion)
        }

        val content = generateResponse(prompt, searchResults)
        if (content.isNotBlank()) {
            if (shouldPreferKorean(normalizedQuestion) && looksEnglishOnly(content)) {
                logger.warn("General answer language mismatch detected. Falling back to Korean reply.")
                return buildKoreanFallback(normalizedQuestion)
            }
            return content
        }

        return if (strictKnowledgeBase && useKnowledgeBase) {
            NO_MATCH_MESSAGE
        } else {
            "답변을 생성하지 못했습니다."
        }
    }

    private fun createRagPrompt(question: String, searchResults: List<Document>): Prompt {
        val context = searchResults.joinToString("\n---\n") { doc ->
            val fileName = doc.metadata["fileName"] ?: "이름 없는 문서"
            "[$fileName]\n${doc.text.orEmpty()}"
        }

        val promptContent = RAG_PROMPT_TEMPLATE
            .replace("{context}", context)
            .replace("{question}", question)

        return Prompt(promptContent)
    }

    private fun createGeneralPrompt(question: String): Prompt {
        val promptContent = GENERAL_PROMPT_TEMPLATE
            .replace("{question}", question)
        return Prompt(promptContent)
    }

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

    private fun shouldPreferKorean(question: String): Boolean =
        question.any { ch -> ch in '\uAC00'..'\uD7A3' || ch in '\u3131'..'\u318E' }

    private fun looksEnglishOnly(content: String): Boolean {
        val hasHangul = content.any { ch -> ch in '\uAC00'..'\uD7A3' || ch in '\u3131'..'\u318E' }
        val hasLatin = content.any { it in 'A'..'Z' || it in 'a'..'z' }
        return hasLatin && !hasHangul
    }

    private fun buildKoreanFallback(question: String): String =
        when {
            question.endsWith("?") || question.contains("왜") || question.contains("어떻게") ->
                "좋은 질문이에요. 핵심부터 차근차근 같이 풀어볼게요."

            question.contains("힘들") || question.contains("외롭") || question.contains("슬프") ->
                "많이 버거우셨겠어요. 지금 가장 크게 걸리는 부분부터 편하게 말해 주세요."

            else ->
                "계속 말씀해 주세요. 질문하신 내용에 맞춰 이어서 도와드릴게요."
        }

    companion object {
        private const val GENERAL_PROMPT_TEMPLATE = """
        You are a helpful chat assistant.
        Default to Korean unless the user clearly writes mostly in English or explicitly asks for English.
        If the user writes in Korean, answer only in Korean.
        Do not switch to English just because a few English words appear in the message.
        Keep the reply concise, direct, and conversational.

        User message:
        {question}
        """

        private const val RAG_PROMPT_TEMPLATE = """
        아래 참고 자료를 우선 사용해서 사용자 질문에 답하세요.
        참고 자료에 없는 내용은 추측하지 말고, 문서에서 찾지 못했다고 명확히 말하세요.
        답변 언어는 사용자의 질문 언어를 따르세요. 한국어 질문이면 한국어로만 답하세요.

        참고 자료:
        {context}

        사용자 질문:
        {question}
        """

        private const val NO_DOCUMENTS_MESSAGE =
            "아직 업로드한 문서가 없습니다. http://localhost:3000/channel 에서 문서를 업로드한 뒤 다시 /search 해 주세요."

        private const val NO_MATCH_MESSAGE =
            "업로드한 문서에서 관련 내용을 찾지 못했습니다. 다른 키워드로 다시 /search 해 주세요."
    }
}
