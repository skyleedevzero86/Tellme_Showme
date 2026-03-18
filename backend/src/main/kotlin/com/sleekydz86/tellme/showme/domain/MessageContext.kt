package com.sleekydz86.tellme.showme.domain

import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate

data class MessageContext(
    val chatId: Long? = null,
    val text: String? = null,
    val firstName: String? = null
) {
    fun commandArgument(): String? {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        val firstSpaceIndex = trimmed.indexOf(' ')
        if (firstSpaceIndex < 0) return null

        return trimmed.substring(firstSpaceIndex + 1)
            .trim()
            .takeIf { it.isNotBlank() }
    }

    companion object {
        fun from(message: TelegramUpdate.Message?): MessageContext? {
            if (message == null) return null
            val chatId = message.chat?.id
            val text = message.text?.trim() ?: ""
            val firstName = message.from?.firstName ?: "사용자"
            return MessageContext(chatId = chatId, text = text, firstName = firstName)
        }
    }
}
