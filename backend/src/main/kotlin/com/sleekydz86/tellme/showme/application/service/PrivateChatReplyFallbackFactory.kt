package com.sleekydz86.tellme.showme.application.service

import org.springframework.stereotype.Component

@Component
class PrivateChatReplyFallbackFactory {

    fun build(text: String): String {
        val normalized = text.trim()
        return when {
            normalized.isBlank() ->
                "한 문장만 더 편하게 보내 주세요. 바로 이어서 도와드릴게요."

            normalized.contains("배고프") ->
                "배고프시군요. 지금 바로 먹을 수 있는 게 있는지 먼저 챙겨 보세요. 원하면 간단한 메뉴도 같이 골라드릴게요."

            normalized.contains("코드") || normalized.contains("버그") || normalized.contains("에러") ->
                "코드 관련 질문이라면 언어, 에러 메시지, 원하는 동작 중 하나만 더 알려 주세요. 그럼 더 정확하게 도와드릴게요."

            normalized.contains("힘들") || normalized.contains("지쳤") || normalized.contains("외롭") || normalized.contains("슬프") ->
                "많이 버거우셨겠어요. 지금 가장 크게 걸리는 부분부터 편하게 말해 주세요."

            normalized.endsWith("?") || normalized.contains("왜") || normalized.contains("어떻게") || normalized.contains("뭐") ->
                "좋은 질문이에요. 핵심부터 차근차근 같이 풀어볼게요."

            else ->
                "계속 듣고 있어요. 더 이어서 말해 주시면 그 내용에 맞춰 바로 답할게요."
        }
    }
}
