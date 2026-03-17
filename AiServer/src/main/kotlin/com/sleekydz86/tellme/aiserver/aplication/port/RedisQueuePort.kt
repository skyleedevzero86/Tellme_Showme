package com.sleekydz86.tellme.aiserver.aplication.port

interface RedisQueuePort {
    fun pushChatSaveJob(userId: String, userMessage: String, assistantMessage: String, mode: String)
    fun popChatSaveJob(): ChatSaveJob?
    fun pushPendingQuestion(userId: String, question: String, mode: String)
    fun rangePendingQuestions(start: Long, end: Long): List<PendingQuestion>
    fun popPendingQuestion(): PendingQuestion?
}
