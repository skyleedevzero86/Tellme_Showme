package com.sleekydz86.tellme.showme.application.service

import org.springframework.stereotype.Component

@Component
class PrivateChatReplyFallbackFactory {

    fun build(text: String): String {
        val normalized = text.trim()
        return if (prefersEnglish(normalized)) {
            buildEnglishReply(normalized)
        } else {
            buildKoreanReply(normalized)
        }
    }

    private fun buildKoreanReply(text: String): String =
        when {
            text.isBlank() ->
                "마음에 걸리는 내용 한 문장만 더 말해 주세요. 바로 이어서 도와드릴게요."

            text.contains("영어", ignoreCase = true) ->
                "알겠어요. 이제 영어로 자연스럽게 이어서 대화할게요. 하고 싶은 말을 계속 보내 주세요."

            text.endsWith("?") ||
                text.contains("왜") ||
                text.contains("어떻게") ||
                text.contains("뭐") ->
                "좋은 질문이에요. 핵심부터 차근차근 같이 풀어볼게요."

            text.contains("힘들", ignoreCase = true) ||
                text.contains("외롭", ignoreCase = true) ||
                text.contains("슬프", ignoreCase = true) ||
                text.contains("지쳤", ignoreCase = true) ||
                text.contains("불안", ignoreCase = true) ->
                "많이 버거우셨겠어요. 지금 제일 크게 걸리는 부분부터 편하게 말해 주세요."

            else ->
                "계속 듣고 있어요. 더 이어서 말해 주시면 그 내용에 맞춰 바로 답할게요."
        }

    private fun buildEnglishReply(text: String): String =
        when {
            text.isBlank() ->
                "Tell me one more sentence about what is on your mind, and I will keep helping."

            text.endsWith("?") ->
                "That is a good question. Let us unpack it step by step."

            text.contains("tired", ignoreCase = true) ||
                text.contains("lonely", ignoreCase = true) ||
                text.contains("hard", ignoreCase = true) ||
                text.contains("sad", ignoreCase = true) ->
                "That sounds heavy. Tell me the hardest part first, and I will stay with you."

            else ->
                "I am listening. Keep going and I will respond to what you say."
        }

    private fun prefersEnglish(text: String): Boolean {
        if (text.contains("영어")) return true
        if (containsHangul(text)) return false
        return text.any { it in 'A'..'Z' || it in 'a'..'z' }
    }

    private fun containsHangul(text: String): Boolean =
        text.any { ch -> ch in '\uAC00'..'\uD7A3' || ch in '\u3131'..'\u318E' }
}
