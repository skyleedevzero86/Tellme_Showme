package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.application.port.AiServerModeChatPort
import com.sleekydz86.tellme.showme.application.service.TelegramConversationModeStore
import com.sleekydz86.tellme.showme.application.service.TimeAlarmService
import com.sleekydz86.tellme.showme.domain.ConversationMode
import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class GodCommandHandler(
    private val conversationModeStore: TelegramConversationModeStore,
    private val timeAlarmService: TimeAlarmService,
    private val aiServerModeChat: AiServerModeChatPort
) : CommandHandler {

    override fun handle(ctx: MessageContext?): Mono<String> {
        val chatId = ctx?.chatId ?: return Mono.just(CHAT_NOT_FOUND_MESSAGE)
        timeAlarmService.cancelSetup(chatId)
        conversationModeStore.activate(chatId, ConversationMode.GOD)

        val prompt = ctx.commandArgument().takeUnless { it.isNullOrBlank() } ?: DEFAULT_PROMPT
        val introSuffix = if (ctx.isPrivateChat()) "" else GROUP_PRIVACY_HINT

        return aiServerModeChat.chat(chatId.toString(), prompt, ConversationMode.GOD)
            .map { if (it.isBlank()) EMPTY_REPLY_MESSAGE else it }
            .map { "$it\n\n$INTRO_MESSAGE$introSuffix" }
    }

    companion object {
        private const val CHAT_NOT_FOUND_MESSAGE =
            "\ucc44\ud305 \uc815\ubcf4\ub97c \ucc3e\uc9c0 \ubabb\ud588\uc2b5\ub2c8\ub2e4."
        private const val GROUP_PRIVACY_HINT =
            "\n\n\ucc38\uace0: \uadf8\ub8f9/\uc288\ud37c\uadf8\ub8f9\uc5d0\uc11c Telegram privacy mode\uac00 \ucf1c\uc838 \uc788\uc73c\uba74 \uc77c\ubc18 \uba54\uc2dc\uc9c0\ub97c \ubabb \ubc1b\uc744 \uc218 \uc788\uc2b5\ub2c8\ub2e4. 1:1 \ucc44\ud305\uc744 \uc0ac\uc6a9\ud558\uac70\ub098 BotFather /setprivacy\ub97c Disable\ub85c \ubc14\uafd4 \uc8fc\uc138\uc694."
        private const val INTRO_MESSAGE =
            "\uba85\uc5b8 \ub300\ud654 \ubaa8\ub4dc\ub97c \uc2dc\uc791\ud588\uc5b4\uc694. \uc774\uc81c \uc77c\ubc18 \uba54\uc2dc\uc9c0\ub97c \ubcf4\ub0b4\uba74 AiServer\uac00 \uc9e7\uc740 \uba85\uc5b8\uc774\ub098 \uaca9\uc5b8 \ub290\ub08c\uc73c\ub85c \ub2f5\ud569\ub2c8\ub2e4. \uc885\ub8cc\ub294 bye \ub610\ub294 /end \ub97c \uc785\ub825\ud574 \uc8fc\uc138\uc694."
        private const val EMPTY_REPLY_MESSAGE =
            "\uba85\uc5b8 \ubaa8\ub4dc \uc751\ub2f5\uc774 \ube44\uc5b4 \uc788\uc2b5\ub2c8\ub2e4."
        private const val DEFAULT_PROMPT =
            "\uc624\ub298 \ub098\uc5d0\uac8c \ud544\uc694\ud55c \uc9e7\uc740 \uba85\uc5b8\uc774\ub098 \uaca9\uc5b8 \ud55c\ub9c8\ub514\ub97c \uc54c\ub824\uc918"
    }
}
