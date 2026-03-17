package com.sleekydz86.tellme.aiserver.infrastructure.scheduler

import com.sleekydz86.tellme.aiserver.aplication.port.RedisQueuePort
import com.sleekydz86.tellme.aiserver.domain.model.ConversationMessageEntity
import com.sleekydz86.tellme.aiserver.domain.model.UserQuestionEntity
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.ConversationMessageRepository
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.UserQuestionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class QueueDrainScheduler(
    private val redisQueuePort: RedisQueuePort,
    private val conversationMessageRepository: ConversationMessageRepository,
    private val userQuestionRepository: UserQuestionRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 2000)
    fun drainChatSaveQueue() {
        var count = 0
        while (true) {
            val job = redisQueuePort.popChatSaveJob() ?: break
            try {
                conversationMessageRepository.save(
                    ConversationMessageEntity(
                        userId = job.userId,
                        role = "user",
                        content = job.userMessage,
                        mode = job.mode
                    )
                )
                conversationMessageRepository.save(
                    ConversationMessageEntity(userId = job.userId, role = "assistant", content = job.assistantMessage, mode = job.mode)
                )
                count++
            } catch (e: Exception) {
                logger.warn("채팅 저장 실패: userId={}", job.userId, e)
            }
        }
        if (count > 0) logger.debug("채팅 저장 작업 {}건 처리됨", count)
    }

    @Scheduled(fixedDelay = 3000)
    fun drainPendingQuestions() {
        var count = 0
        while (true) {
            val q = redisQueuePort.popPendingQuestion() ?: break
            try {
                userQuestionRepository.save(
                    UserQuestionEntity(
                        userId = q.userId,
                        questionText = q.question,
                        mode = q.mode,
                        createdAt = Instant.ofEpochMilli(q.createdAt)
                    )
                )
                count++
            } catch (e: Exception) {
                logger.warn("대기 질문 저장 실패: userId={}", q.userId, e)
            }
        }
        if (count > 0) logger.debug("대기 질문 {}건 처리됨", count)
    }
}
