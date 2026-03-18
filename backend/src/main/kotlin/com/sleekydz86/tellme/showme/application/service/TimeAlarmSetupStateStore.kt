package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.domain.AlarmType
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TimeAlarmSetupStateStore(
    private val redisTemplate: StringRedisTemplate
) {
    fun get(chatId: Long): TimeAlarmSetupState? {
        val raw = redisTemplate.opsForValue().get(key(chatId)) ?: return null
        val tokens = raw.split(DELIMITER, limit = 2)
        val step = runCatching { TimeAlarmSetupStep.valueOf(tokens[0]) }.getOrNull() ?: return null
        val alarmType = tokens.getOrNull(1)
            ?.takeIf { it.isNotBlank() && it != NULL_TOKEN }
            ?.let { runCatching { AlarmType.valueOf(it) }.getOrNull() }

        return TimeAlarmSetupState(step = step, alarmType = alarmType)
    }

    fun put(chatId: Long, state: TimeAlarmSetupState) {
        val raw = listOf(
            state.step.name,
            state.alarmType?.name ?: NULL_TOKEN
        ).joinToString(DELIMITER)
        redisTemplate.opsForValue().set(key(chatId), raw, SETUP_TTL)
    }

    fun clear(chatId: Long): TimeAlarmSetupState? {
        val state = get(chatId)
        redisTemplate.delete(key(chatId))
        return state
    }

    private fun key(chatId: Long): String = "telegram:time-alarm-setup:$chatId"

    companion object {
        private const val DELIMITER = "|"
        private const val NULL_TOKEN = "-"
        private val SETUP_TTL: Duration = Duration.ofMinutes(30)
    }
}

data class TimeAlarmSetupState(
    val step: TimeAlarmSetupStep,
    val alarmType: AlarmType? = null
)

enum class TimeAlarmSetupStep {
    CONFIRM,
    TYPE,
    INTERVAL_MINUTES,
    DAILY_TIME
}
