package com.sleekydz86.tellme.aiserver.infrastructure.redis

import com.sleekydz86.tellme.aiserver.aplication.port.ChatSaveJob
import com.sleekydz86.tellme.aiserver.aplication.port.PendingQuestion
import com.sleekydz86.tellme.aiserver.aplication.port.RedisQueuePort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisQueueAdapter(
    private val redisTemplate: RedisTemplate<String, Any>
) : RedisQueuePort {

    companion object {
        private const val QUEUE_CHAT_SAVE = "queue:chat:save"
        private const val QUEUE_PENDING_QUESTIONS = "queue:pending:questions"
    }

    override fun pushChatSaveJob(userId: String, userMessage: String, assistantMessage: String, mode: String) {
        redisTemplate.opsForList().rightPush(QUEUE_CHAT_SAVE, ChatSaveJob(userId, userMessage, assistantMessage, mode))
    }

    @Suppress("UNCHECKED_CAST")
    override fun popChatSaveJob(): ChatSaveJob? {
        val raw = redisTemplate.opsForList().leftPop(QUEUE_CHAT_SAVE) ?: return null
        return mapToChatSaveJob(raw)
    }

    override fun pushPendingQuestion(userId: String, question: String, mode: String) {
        val payload = mapOf(
            "userId" to userId,
            "question" to question,
            "mode" to mode,
            "createdAt" to System.currentTimeMillis()
        )
        redisTemplate.opsForList().rightPush(QUEUE_PENDING_QUESTIONS, payload)
    }

    @Suppress("UNCHECKED_CAST")
    override fun rangePendingQuestions(start: Long, end: Long): List<PendingQuestion> {
        val list = redisTemplate.opsForList().range(QUEUE_PENDING_QUESTIONS, start, end) ?: return emptyList()
        return list.mapNotNull { raw ->
            (raw as? Map<*, *>)?.let { m ->
                PendingQuestion(
                    userId = m["userId"] as? String ?: "",
                    question = m["question"] as? String ?: "",
                    mode = m["mode"] as? String ?: "",
                    createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0L
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun popPendingQuestion(): PendingQuestion? {
        val raw = redisTemplate.opsForList().leftPop(QUEUE_PENDING_QUESTIONS) ?: return null
        return (raw as? Map<*, *>)?.let { m ->
            PendingQuestion(
                userId = m["userId"] as? String ?: "",
                question = m["question"] as? String ?: "",
                mode = m["mode"] as? String ?: "",
                createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    private fun mapToChatSaveJob(raw: Any): ChatSaveJob? = (raw as? Map<*, *>)?.let { m ->
        ChatSaveJob(
            userId = m["userId"] as? String ?: "",
            userMessage = m["userMessage"] as? String ?: "",
            assistantMessage = m["assistantMessage"] as? String ?: "",
            mode = m["mode"] as? String ?: "",
            createdAt = (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
