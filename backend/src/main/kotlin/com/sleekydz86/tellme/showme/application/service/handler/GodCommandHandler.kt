package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.application.port.AiServerModeChatPort
import com.sleekydz86.tellme.showme.application.service.TelegramConversationModeStore
import com.sleekydz86.tellme.showme.domain.ConversationMode
import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class GodCommandHandler(
    private val conversationModeStore: TelegramConversationModeStore,
    private val aiServerModeChat: AiServerModeChatPort
) : CommandHandler {

    override fun handle(ctx: MessageContext?): Mono<String> {
        val chatId = ctx?.chatId ?: return Mono.just(CHAT_NOT_FOUND_MESSAGE)
        conversationModeStore.activate(chatId, ConversationMode.GOD)

        val prompt = ctx.commandArgument().takeUnless { it.isNullOrBlank() } ?: DEFAULT_PROMPT

        return aiServerModeChat.chat(chatId.toString(), prompt, ConversationMode.GOD)
            .map { if (it.isBlank()) EMPTY_REPLY_MESSAGE else it }
            .map { "$it\n\n$INTRO_MESSAGE" }
    }

    companion object {
        private const val CHAT_NOT_FOUND_MESSAGE = "채팅 정보를 찾지 못했습니다."
        private const val INTRO_MESSAGE =
            "명언 대화 모드를 시작했어요. 이제 일반 메시지를 보내면 AiServer가 짧은 명언이나 격언 느낌으로 답합니다. 종료는 bye 또는 /end 를 입력해 주세요."
        private const val EMPTY_REPLY_MESSAGE = "명언 모드 응답이 비어 있습니다."
        private const val DEFAULT_PROMPT = "오늘 나에게 필요한 짧은 명언이나 격언 한마디를 들려줘."
    }
}
