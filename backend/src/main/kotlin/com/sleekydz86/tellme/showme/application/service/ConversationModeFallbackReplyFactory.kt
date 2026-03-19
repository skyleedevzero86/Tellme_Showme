package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.domain.ConversationMode
import org.springframework.stereotype.Component

@Component
class ConversationModeFallbackReplyFactory {

    fun build(mode: ConversationMode, text: String): String {
        val normalized = text.trim()
        return when (mode) {
            ConversationMode.ENG -> buildEnglishReply(normalized)
            ConversationMode.GOD -> buildGodReply(normalized)
        }
    }

    private fun buildEnglishReply(text: String): String =
        when {
            text.isBlank() -> "Tell me a little more. I will keep replying in English."
            text.endsWith("?") -> "That is a thoughtful question. Let us take it one step at a time."
            else -> "I hear you. Keep going, one sentence at a time, and I will stay with you in English."
        }

    private fun buildGodReply(text: String): String =
        when {
            text.isBlank() ->
                "\uc791\uc740 \uc228\ube44\ub3c4 \ub2e4\uc2dc \ubc14\ub78c\uc774 \ub429\ub2c8\ub2e4.\n\ucc9c\ucc9c\ud788 \ub9c8\uc74c\uc744 \uac00\ub2e4\ub4ec\uace0 \ub2e4\uc2dc \uc774\uc57c\uae30\ud574 \ubcf4\uc138\uc694."

            text.contains("\ud798\ub4e4") || text.contains("\uc678\ub86d") ->
                "\uc5b4\ub460\uc744 \uacac\ub514\ub294 \ub9c8\uc74c\uc5d0\ub3c4 \uc544\uce68\uc758 \ube5b\uc740 \ucc3e\uc544\uc635\ub2c8\ub2e4.\n\uc624\ub298\uc740 \uc2a4\uc2a4\ub85c\ub97c \ud0d3\ud558\uc9c0 \ub9d0\uace0, \ud55c \uac78\uc74c\ub9cc \ubc84\ud2f0\uba74 \ucda9\ubd84\ud569\ub2c8\ub2e4."

            text.contains("\ubd88\uc548") || text.contains("\uac71\uc815") ->
                "\ub9c8\uc74c\uc774 \ud754\ub4e4\ub9b4\uc218\ub85d \ud638\ud761\uc744 \uace0\ub974\uac8c \ud558\uc2ed\uc2dc\uc624.\n\uacb0\ub860\uc744 \uc11c\ub450\ub974\uc9c0 \ub9d0\uace0, \uc624\ub298 \ud560 \uc218 \uc788\ub294 \ud55c \uac00\uc9c0\uc5d0 \uc9d1\uc911\ud574 \ubcf4\uc138\uc694."

            else ->
                "\ucc9c\ucc9c\ud788 \uac00\ub3c4 \uba48\ucd94\uc9c0 \uc54a\uc73c\uba74 \uc55e\uc73c\ub85c \uac11\ub2c8\ub2e4.\n\uc9c0\uae08\uc758 \uace0\ubbfc\ub3c4 \ud55c \uac78\uc74c\uc529 \ud480\uc5b4\uac00\uba74 \uae38\uc774 \ubcf4\uc77c \uac83\uc785\ub2c8\ub2e4."
        }
}
