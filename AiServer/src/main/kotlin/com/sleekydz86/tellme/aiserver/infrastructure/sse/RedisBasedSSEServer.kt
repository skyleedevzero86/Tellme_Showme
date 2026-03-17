package com.sleekydz86.tellme.aiserver.infrastructure.sse

import com.sleekydz86.tellme.aiserver.aplication.event.DomainEventPublisher
import com.sleekydz86.tellme.aiserver.aplication.port.SSEPort
import com.sleekydz86.tellme.aiserver.domain.event.SseConnected
import com.sleekydz86.tellme.aiserver.domain.event.SseDisconnected
import com.sleekydz86.tellme.global.enums.SSEMsgType
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class RedisBasedSSEServer(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val redisMessageListenerContainer: RedisMessageListenerContainer,
    private val domainEventPublisher: DomainEventPublisher
) : SSEPort {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val connections = ConcurrentHashMap<String, SseEmitter>()
    private val channelTopics = ConcurrentHashMap<String, ChannelTopic>()

    override fun addConnection(userId: String, emitter: SseEmitter) {
        connections[userId] = emitter
        redisTemplate.opsForValue().set("sse:connection:$userId", "connected", java.time.Duration.ofMinutes(30))
        sendMsg(userId, "연결됨", SSEMsgType.ADD.name)
        setupUserChannel(userId)
        domainEventPublisher.publish(SseConnected(userId))
        logger.info("SSE 연결 추가됨: userId={}, total={}", userId, connections.size)
    }

    override fun sendMsg(userId: String, message: String, type: String) {
        try {
            val channel = "sse:user:$userId"
            val messageData = mapOf(
                "type" to type,
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )
            redisTemplate.convertAndSend(channel, messageData)
        } catch (e: Exception) {
            logger.error("Redis 전송 실패: userId={}", userId, e)
            sendDirect(userId, message, type)
        }
    }

    override fun close(userId: String) {
        connections.remove(userId)?.let { emitter ->
            try {
                emitter.complete()
                redisTemplate.delete("sse:connection:$userId")
                domainEventPublisher.publish(SseDisconnected(userId))
                logger.info("SSE 연결 종료됨: userId={}", userId)
            } catch (e: Exception) {
                logger.error("SSE 종료 실패: userId={}", userId, e)
            }
        }
    }

    private fun setupUserChannel(userId: String) {
        val topic = ChannelTopic("sse:user:$userId")
        channelTopics[userId] = topic
        redisMessageListenerContainer.addMessageListener(
            MessageListener { message, _ -> handleRedisMessage(userId, message) },
            topic
        )
    }

    private fun handleRedisMessage(userId: String, message: Message) {
        connections[userId]?.let { emitter ->
            try {
                @Suppress("UNCHECKED_CAST")
                val map = redisTemplate.valueSerializer?.deserialize(message.body) as? Map<*, *>
                val type = (map?.get("type") as? String) ?: "add"
                val content = (map?.get("message") as? String) ?: ""
                emitter.send(SseEmitter.event().name(type.lowercase()).data(content).build())
            } catch (e: Exception) {
                logger.error("Redis 메시지 처리 실패: userId={}", userId, e)
                close(userId)
            }
        }
    }

    private fun sendDirect(userId: String, message: String, type: String) {
        connections[userId]?.let { emitter ->
            try {
                emitter.send(SseEmitter.event().name(type.lowercase()).data(message).build())
            } catch (e: Exception) {
                logger.error("SSE 직접 전송 실패: userId={}", userId, e)
                close(userId)
            }
        }
    }
}
