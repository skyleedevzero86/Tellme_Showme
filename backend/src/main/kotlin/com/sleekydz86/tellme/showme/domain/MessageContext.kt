package com.sleekydz86.tellme.showme.domain

import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import lombok.Builder
import lombok.Value

@Value
@Builder
class MessageContext {
    var chatId: Long? = null
    var text: String? = null
    var firstName: String? = null

    companion object {
        fun from(message: TelegramUpdate.Message?): MessageContext? {
            if (message == null) {
                return null
            }
            val chatId: Long = (if (message.getChat() != null) message.getChat().getId() else null)!!
            val text: String? = message.getText()
            val firstName: String? = if (message.getFrom() != null) message.getFrom().getFirstName() else null
            return MessageContext.builder()
                .chatId(chatId)
                .text(if (text != null) text.trim { it <= ' ' } else "")
                .firstName(if (firstName != null) firstName else "사용자")
                .build()
        }
    }
}
