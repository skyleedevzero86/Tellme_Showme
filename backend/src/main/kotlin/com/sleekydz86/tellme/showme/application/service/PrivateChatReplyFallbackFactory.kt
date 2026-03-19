package com.sleekydz86.tellme.showme.application.service

import org.springframework.stereotype.Component

@Component
class PrivateChatReplyFallbackFactory {

    fun build(text: String): String {
        val normalized = text.trim()
        return when {
            normalized.isBlank() ->
                "Tell me one more sentence about what is on your mind. I will keep the conversation going."

            normalized.endsWith("?") ->
                "That is a good question. Let us unpack it one step at a time."

            normalized.contains("tired", ignoreCase = true) ||
                normalized.contains("lonely", ignoreCase = true) ||
                normalized.contains("hard", ignoreCase = true) ->
                "That sounds heavy. You do not have to swallow it all at once. Tell me the next part too."

            else ->
                "I am listening. Keep going and I will reply right away."
        }
    }
}
