package com.sleekydz86.tellme.aiserver.domain.model

data class ChatEntity(
    val currentUserName: String,
    val message: String,
    val useKnowledgeBase: Boolean = false
) {
    companion object {
        fun forOllama(userName: String, message: String) =
            ChatEntity(userName, message, useKnowledgeBase = false)

        fun forRag(userName: String, message: String, useKnowledge: Boolean = true) =
            ChatEntity(userName, message, useKnowledgeBase = useKnowledge)
    }
}
