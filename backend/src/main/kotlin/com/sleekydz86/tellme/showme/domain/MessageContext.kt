package com.sleekydz86.tellme.showme.domain

import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate

data class MessageContext(
    val chatId: Long? = null,
    val text: String? = null,
    val firstName: String? = null,
    val chatType: String? = null,
    val inputSource: InputSource = InputSource.TELEGRAM
) {
    fun isPrivateChat(): Boolean = chatType.equals("private", ignoreCase = true)

    fun isFrontend(): Boolean = inputSource == InputSource.FRONTEND

    fun supportsAlarmSetup(): Boolean = inputSource.supportsAlarmSetup()

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
            return MessageContext(
                chatId = message.chat?.id,
                text = message.text?.trim().orEmpty(),
                firstName = message.from?.firstName ?: "User",
                chatType = message.chat?.type,
                inputSource = InputSource.TELEGRAM
            )
        }
    }
}
