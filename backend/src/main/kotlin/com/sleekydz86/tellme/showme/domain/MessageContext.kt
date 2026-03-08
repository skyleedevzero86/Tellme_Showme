package com.sleekydz86.tellme.showme.domain

import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate

data class MessageContext(
    val chatId: Long? = null,
    val text: String? = null,
    val firstName: String? = null
) {
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
