package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.domain.AlarmType
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
class TimeAlarmSetupStateStore(
    private val redisTemplate: StringRedisTemplate
) {
    fun get(chatId: Long): TimeAlarmSetupState? {
        val raw = redisTemplate.opsForValue().get(key(chatId)) ?: return null
        val tokens = raw.split(DELIMITER)
        val step = tokens.getOrNull(0)
            ?.let { runCatching { TimeAlarmSetupStep.valueOf(it) }.getOrNull() }
            ?: return null
        val alarmType = tokens.getOrNull(1)
            ?.takeIf { it.isNotBlank() && it != NULL_TOKEN }
            ?.let { runCatching { AlarmType.valueOf(it) }.getOrNull() }
        val intervalMinutes = tokens.getOrNull(2)
            ?.takeIf { it.isNotBlank() && it != NULL_TOKEN }
            ?.toIntOrNull()
        val timeOfDay = tokens.getOrNull(3)
            ?.takeIf { it.isNotBlank() && it != NULL_TOKEN }
            ?.let { runCatching { LocalTime.parse(it, TIME_FORMATTER) }.getOrNull() }

        return TimeAlarmSetupState(
            step = step,
            alarmType = alarmType,
            intervalMinutes = intervalMinutes,
            timeOfDay = timeOfDay
        )
    }

    fun put(chatId: Long, state: TimeAlarmSetupState) {
        val raw = listOf(
            state.step.name,
            state.alarmType?.name ?: NULL_TOKEN,
            state.intervalMinutes?.toString() ?: NULL_TOKEN,
            state.timeOfDay?.format(TIME_FORMATTER) ?: NULL_TOKEN
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
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

data class TimeAlarmSetupState(
    val step: TimeAlarmSetupStep,
    val alarmType: AlarmType? = null,
    val intervalMinutes: Int? = null,
    val timeOfDay: LocalTime? = null
)

enum class TimeAlarmSetupStep {
    CONFIRM,
    TYPE,
    INTERVAL_MINUTES,
    DAILY_TIME,
    MESSAGE
}
