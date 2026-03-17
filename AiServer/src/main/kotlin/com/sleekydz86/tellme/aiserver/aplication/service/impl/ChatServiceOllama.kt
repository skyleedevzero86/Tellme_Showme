package com.sleekydz86.tellme.aiserver.aplication.service.impl

import com.sleekydz86.tellme.aiserver.aplication.event.DomainEventPublisher
import com.sleekydz86.tellme.aiserver.aplication.port.RedisLockPort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisQueuePort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisSearchPort
import com.sleekydz86.tellme.aiserver.aplication.port.RedisSessionPort
import com.sleekydz86.tellme.aiserver.aplication.port.SSEPort
import com.sleekydz86.tellme.aiserver.aplication.service.ChatService
import com.sleekydz86.tellme.aiserver.domain.event.ChatMessageSent
import com.sleekydz86.tellme.aiserver.domain.event.ChatResponseGenerated
import com.sleekydz86.tellme.aiserver.domain.model.ChatEntity
import com.sleekydz86.tellme.global.enums.SSEMsgType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import jakarta.annotation.PostConstruct
import kotlin.random.Random

@Service
class ChatServiceOllama(
    private val ssePort: SSEPort,
    private val domainEventPublisher: DomainEventPublisher,
    private val redisLockPort: RedisLockPort,
    private val redisQueuePort: RedisQueuePort,
    private val redisSearchPort: RedisSearchPort,
    private val redisSessionPort: RedisSessionPort
) : ChatService {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${huggingface.chat.model-name:microsoft/DialoGPT-small}")
    private lateinit var modelName: String

    @Value("\${huggingface.chat.max-length:200}")
    private var maxLength: Int = 200

    @Value("\${huggingface.chat.temperature:0.7}")
    private var temperature: Double = 0.7

    private val personality = mapOf(
        "greeting" to listOf(
            "안녕하세요! 🙋‍♀️ 오늘 하루는 어떠세요?",
            "반가워요! 😊 뭔가 재미있는 얘기 없나요?",
            "안녕! 👋 좋은 하루 보내고 계신가요?"
        ),
        "tired_responses" to listOf(
            "아 정말 피곤하시겠어요 😴 요즘 밤늦게 자셨나요?",
            "졸음이 몰려오는군요... 커피 한 잔 어떠세요? ☕",
            "피곤하실 때는 잠깐 휴식을 취하시는 게 좋을 것 같아요 💤",
            "아 졸리시겠다... 충분한 수면이 제일 중요해요! 😌"
        ),
        "encouragement" to listOf(
            "힘내세요! 조금만 더 버티면 될 거예요 💪",
            "그래도 여기까지 오신 것만으로도 대단하세요!",
            "오늘 하루도 수고 많으셨어요 👏",
            "가끔은 쉬어가는 것도 필요해요 🌸"
        ),
        "casual_responses" to listOf(
            "그러게요! 저도 그런 생각 해봤어요 🤔",
            "정말요? 흥미롭네요! 더 자세히 얘기해주세요 ✨",
            "아하! 그런 일이 있으셨구나요 😮",
            "와 그건 정말 신기하네요! 🌟"
        )
    )

    private val emotionKeywords = mapOf(
        "tired" to listOf("졸려", "피곤", "잠", "힘들", "지쳐", "sleepy", "tired"),
        "sad" to listOf("슬퍼", "우울", "속상", "힘들", "sad", "depressed"),
        "happy" to listOf("기뻐", "좋아", "신나", "행복", "happy", "excited", "great"),
        "angry" to listOf("화나", "짜증", "분노", "angry", "mad", "frustrated"),
        "hungry" to listOf("배고파", "밥", "음식", "먹고싶", "hungry"),
        "bored" to listOf("심심", "지루", "재미없", "bored"),
        "stressed" to listOf("스트레스", "압박", "부담", "stressed")
    )

    @PostConstruct
    fun init() {
        logger.info("Ollama 스타일 채팅 초기화: model={}, maxLength={}, temperature={}", modelName, maxLength, temperature)
    }

    override fun streamOllama(chatEntity: ChatEntity) {
        val userId = chatEntity.currentUserName
        val prompt = chatEntity.message
        redisQueuePort.pushPendingQuestion(userId, prompt, "ollama")
        redisSearchPort.incrementQueryScore(prompt)
        redisSessionPort.set(userId, "lastActivity", System.currentTimeMillis().toString())
        domainEventPublisher.publish(ChatMessageSent(userId, prompt, false, "ollama"))
        val errorHandled = AtomicBoolean(false)
        val ran = redisLockPort.withLock("chat:$userId", 30L) {
            runBlocking {
                try {
                    generateConversationalResponse(userId, prompt, errorHandled)
                } catch (e: Exception) {
                    if (errorHandled.compareAndSet(false, true)) {
                        logger.error("Ollama 채팅 오류: {}", e.message)
                        ssePort.sendMsg(userId, "오류가 발생했습니다. 다시 시도해 주세요.", SSEMsgType.FINISH.name)
                        ssePort.close(userId)
                    }
                }
            }
        }
        if (!ran) {
            ssePort.sendMsg(userId, "다른 요청 처리 중입니다. 잠시 후 다시 시도해 주세요.", SSEMsgType.FINISH.name)
            ssePort.close(userId)
        }
    }

    override fun streamRag(chatEntity: ChatEntity) {}

    private suspend fun generateConversationalResponse(userId: String, prompt: String, errorHandled: AtomicBoolean) {
        delay(100)
        val detectedEmotion = detectEmotion(prompt)
        delay(150)
        val response = generateContextualResponse(prompt, detectedEmotion)
        val tokens = tokenizeForConversation(response)

        for ((index, token) in tokens.withIndex()) {
            if (errorHandled.get()) break
            val thinkingDelay = when {
                index == 0 -> Random.nextLong(300, 600)
                index < 3 -> Random.nextLong(150, 300)
                index < tokens.size - 3 -> Random.nextLong(80, 180)
                else -> Random.nextLong(120, 250)
            }
            delay(thinkingDelay)
            val chunk = if (index == tokens.size - 1) token else "$token "
            ssePort.sendMsg(userId, chunk, SSEMsgType.ADD.name)
        }

        if (!errorHandled.get()) {
            delay(200)
            domainEventPublisher.publish(ChatResponseGenerated(userId, response.length, "ollama"))
            redisQueuePort.pushChatSaveJob(userId, prompt, response, "ollama")
            ssePort.sendMsg(userId, "", SSEMsgType.FINISH.name)
            ssePort.close(userId)
        }
    }

    private fun detectEmotion(text: String): String =
        emotionKeywords.entries.find { (_, keywords) -> keywords.any { text.lowercase().contains(it) } }?.key ?: "neutral"

    private fun generateContextualResponse(prompt: String, emotion: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("안녕") || lower.contains("hello") || lower.contains("hi") -> personality["greeting"]!!.random()
            emotion == "tired" -> personality["tired_responses"]!!.random() + " " + personality["encouragement"]!!.random()
            emotion == "sad" -> "아 무슨 일 있으셨나요? 😢 속상한 일이 있으시면 얘기해 주세요."
            emotion == "happy" -> "와! 좋은 일이 있으셨나보네요! 😄✨"
            emotion == "angry" -> "어? 뭔가 화나는 일이 있으셨나요? 😤"
            emotion == "hungry" -> "아 배고프시겠어요! 🍽️ 뭐 드시고 싶으세요?"
            emotion == "bored" -> "심심하시구나! 🙃 재미있는 얘기라도 해볼까요?"
            emotion == "stressed" -> "스트레스 받으시는군요 😰 깊게 숨 한 번 쉬어보세요~"
            lower.contains("sse") || lower.contains("스트리밍") -> "SSE로 실시간 스트리밍되고 있어요! ✨"
            lower.contains("코틀린") || lower.contains("kotlin") -> "코틀린! 💎 정말 깔끔한 언어죠!"
            prompt.length <= 3 -> personality["casual_responses"]!!.random() + " 좀 더 자세히 얘기해 주세요!"
            else -> listOf("그런 얘기시는군요! 🤔", "음 흥미로운데요? 🌟", "아하! 그렇군요 😊").random() + " " +
                    listOf("더 자세히 얘기해 주시겠어요?", "어떤 기분이셨어요?").random()
        }
    }

    private fun tokenizeForConversation(response: String): List<String> =
        response.replace(Regex("([.!?😊😄😢😤🤔🌟✨🙃😰🤗💎🤖])"), " $1 ").split(Regex("\\s+")).filter { it.isNotBlank() }
}
