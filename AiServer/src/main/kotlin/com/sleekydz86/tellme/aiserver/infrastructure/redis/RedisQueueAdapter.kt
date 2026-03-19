package com.sleekydz86.tellme.aiserver.infrastructure.redis

import com.sleekydz86.tellme.aiserver.aplication.port.ChatSaveJob
import com.sleekydz86.tellme.aiserver.aplication.port.PendingQuestion
import com.sleekydz86.tellme.aiserver.aplication.port.RedisQueuePort
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class RedisQueueAdapter(
    private val redisTemplate: RedisTemplate<String, Any>
) : RedisQueuePort {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val lastFailureLogAt = AtomicLong(0L)

    companion object {
        private const val QUEUE_CHAT_SAVE = "queue:chat:save"
        private const val QUEUE_PENDING_QUESTIONS = "queue:pending:questions"
        private const val FAILURE_LOG_INTERVAL_MS = 30_000L
    }

    override fun pushChatSaveJob(userId: String, userMessage: String, assistantMessage: String, mode: String) {
        safeWrite("rightPush", QUEUE_CHAT_SAVE) {
            redisTemplate.opsForList().rightPush(QUEUE_CHAT_SAVE, ChatSaveJob(userId, userMessage, assistantMessage, mode))
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun popChatSaveJob(): ChatSaveJob? {
        val raw = safeRead("leftPop", QUEUE_CHAT_SAVE) {
            redisTemplate.opsForList().leftPop(QUEUE_CHAT_SAVE)
        } ?: return null
        return mapToChatSaveJob(raw)
    }

    override fun pushPendingQuestion(userId: String, question: String, mode: String) {
        val payload = mapOf(
            "userId" to userId,
            "question" to question,
            "mode" to mode,
            "createdAt" to System.currentTimeMillis()
        )
        safeWrite("rightPush", QUEUE_PENDING_QUESTIONS) {
            redisTemplate.opsForList().rightPush(QUEUE_PENDING_QUESTIONS, payload)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun rangePendingQuestions(start: Long, end: Long): List<PendingQuestion> {
        val list = safeRead("range", QUEUE_PENDING_QUESTIONS) {
            redisTemplate.opsForList().range(QUEUE_PENDING_QUESTIONS, start, end)
        } ?: return emptyList()
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
        val raw = safeRead("leftPop", QUEUE_PENDING_QUESTIONS) {
            redisTemplate.opsForList().leftPop(QUEUE_PENDING_QUESTIONS)
        } ?: return null
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

    private fun safeWrite(action: String, queue: String, operation: () -> Unit) {
        try {
            operation()
        } catch (error: Exception) {
            logQueueFailure(action, queue, error)
        }
    }

    private fun <T> safeRead(action: String, queue: String, operation: () -> T?): T? =
        try {
            operation()
        } catch (error: Exception) {
            logQueueFailure(action, queue, error)
            null
        }

    private fun logQueueFailure(action: String, queue: String, error: Exception) {
        val now = System.currentTimeMillis()
        val lastLoggedAt = lastFailureLogAt.get()
        val shouldWarn = now - lastLoggedAt >= FAILURE_LOG_INTERVAL_MS && lastFailureLogAt.compareAndSet(lastLoggedAt, now)
        if (shouldWarn) {
            logger.warn(
                "Redis queue operation failed: action={}, queue={}, error={}. Queue processing will retry later.",
                action,
                queue,
                error.message,
                error
            )
        } else {
            logger.debug(
                "Redis queue operation failed: action={}, queue={}, error={}",
                action,
                queue,
                error.message
            )
        }
    }
}
