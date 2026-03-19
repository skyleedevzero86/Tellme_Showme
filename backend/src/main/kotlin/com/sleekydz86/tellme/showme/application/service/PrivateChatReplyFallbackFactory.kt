package com.sleekydz86.tellme.showme.application.service

import org.springframework.stereotype.Component

@Component
class PrivateChatReplyFallbackFactory {

    fun build(text: String, replyContext: String? = null): String {
        val normalized = text.trim()
        val hasReplyContext = !replyContext.isNullOrBlank()

        return when {
            normalized.isBlank() ->
                "한 문장만 편하게 보내 주세요. 바로 이어서 도와드릴게요."

            hasReplyContext && isClarificationRequest(normalized) ->
                "방금 답장한 내용을 다시 쉽게 풀어드릴게요. 특히 헷갈린 문장을 한 번만 더 짚어 주시면 그 부분부터 정확히 설명할게요."

            normalized.contains("배고파") ->
                "배고프시군요. 지금 바로 먹을 수 있는 게 있는지부터 먼저 챙겨 보세요. 원하시면 간단한 메뉴도 같이 골라드릴게요."

            normalized.contains("코드") || normalized.contains("버그") || normalized.contains("에러") ->
                "코드 관련 질문이라면 언어, 에러 메시지, 기대한 동작 중 하나만 더 알려 주세요. 그러면 더 정확하게 도와드릴게요."

            normalized.contains("힘들") || normalized.contains("지쳐") || normalized.contains("슬프") ->
                "많이 버거우셨겠어요. 지금 가장 크게 걸리는 부분부터 편하게 말해 주세요."

            normalized.endsWith("?") || normalized.contains("왜") || normalized.contains("어떻게") || normalized.contains("뭐") ->
                "좋은 질문이에요. 핵심부터 차근차근 같이 풀어볼게요."

            else ->
                "계속 듣고 있어요. 더 이어서 말해 주시면 그 내용에 맞춰 바로 답할게요."
        }
    }

    private fun isClarificationRequest(text: String): Boolean {
        val normalized = text.lowercase()
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
}
