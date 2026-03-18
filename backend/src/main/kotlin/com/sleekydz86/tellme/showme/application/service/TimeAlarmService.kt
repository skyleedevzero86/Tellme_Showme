package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.global.config.TelegramBotProperties
import com.sleekydz86.tellme.showme.domain.AlarmType
import com.sleekydz86.tellme.showme.infrastructure.persistence.TelegramAlarmEntity
import com.sleekydz86.tellme.showme.infrastructure.persistence.TelegramAlarmRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class TimeAlarmService(
    private val setupStateStore: TimeAlarmSetupStateStore,
    private val alarmRepository: TelegramAlarmRepository,
    private val properties: TelegramBotProperties
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE a h시 mm분 ss초", Locale.KOREAN)
    private val shortFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 a h시 mm분", Locale.KOREAN)
    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")

    fun startSetup(chatId: Long): String {
        setupStateStore.put(chatId, TimeAlarmSetupState(TimeAlarmSetupStep.CONFIRM))
        return buildCurrentTimeMessage() + "\n알람을 받을까요? 예/아니오 로 답해 주세요."
    }

    fun hasPendingSetup(chatId: Long): Boolean = setupStateStore.get(chatId) != null

    fun cancelSetup(chatId: Long): Boolean = setupStateStore.clear(chatId) != null

    fun continueSetup(chatId: Long, input: String): String {
        val state = setupStateStore.get(chatId) ?: return "진행 중인 알람 설정이 없습니다."
        val normalized = input.trim()

        if (isCancelInput(normalized)) {
            setupStateStore.clear(chatId)
            return "알람 설정을 취소했어요."
        }

        return when (state.step) {
            TimeAlarmSetupStep.CONFIRM -> handleConfirm(chatId, normalized)
            TimeAlarmSetupStep.TYPE -> handleType(chatId, normalized)
            TimeAlarmSetupStep.INTERVAL_MINUTES -> handleIntervalMinutes(chatId, normalized)
            TimeAlarmSetupStep.DAILY_TIME -> handleDailyTime(chatId, normalized)
        }
    }

    fun findDueAlarms(now: LocalDateTime = nowLocalDateTime()): List<TelegramAlarmEntity> =
        alarmRepository.findByActiveTrueAndNextTriggerAtLessThanEqualOrderByNextTriggerAtAsc(now)

    fun markTriggered(alarm: TelegramAlarmEntity, triggeredAt: LocalDateTime = nowLocalDateTime()) {
        alarm.lastTriggeredAt = triggeredAt
        alarm.nextTriggerAt = when (alarm.alarmType) {
            AlarmType.INTERVAL -> triggeredAt.plusMinutes(alarm.intervalMinutes?.toLong() ?: 1L)
            AlarmType.DAILY_TIME -> computeNextDailyTrigger(triggeredAt, alarm.timeOfDay ?: LocalTime.of(9, 0))
        }
        alarm.updatedAt = triggeredAt
        alarmRepository.save(alarm)
    }

    fun buildAlarmNotificationMessage(alarm: TelegramAlarmEntity): String {
        val typeLabel = when (alarm.alarmType) {
            AlarmType.INTERVAL -> "반복 간격"
            AlarmType.DAILY_TIME -> "특정 시간"
        }
        return buildString {
            appendLine("다시 한 번 현재 시각입니다. 알람 메시지입니다.")
            appendLine("알람 종류: $typeLabel")
            append("현재 시각: ${format(nowZonedDateTime())}")
        }
    }

    fun buildCurrentTimeMessage(): String = "현재 시각은 ${format(nowZonedDateTime())}입니다."

    private fun handleConfirm(chatId: Long, input: String): String =
        when {
            isPositive(input) -> {
                setupStateStore.put(chatId, TimeAlarmSetupState(TimeAlarmSetupStep.TYPE))
                "좋아요. 시간 간격으로 받을까요, 특정 시간에 받을까요? 간격 또는 시간 으로 답해 주세요."
            }

            isNegative(input) -> {
                setupStateStore.clear(chatId)
                "알람 설정 없이 현재 시각만 안내했어요."
            }

            else -> "알람을 받을지 먼저 알려주세요. 예 또는 아니오 로 답해 주세요."
        }

    private fun handleType(chatId: Long, input: String): String =
        when {
            isIntervalType(input) -> {
                setupStateStore.put(
                    chatId,
                    TimeAlarmSetupState(
                        step = TimeAlarmSetupStep.INTERVAL_MINUTES,
                        alarmType = AlarmType.INTERVAL
                    )
                )
                "몇 분 간격으로 받을까요? 10 처럼 분 단위 숫자를 입력해 주세요."
            }

            isSpecificTimeType(input) -> {
                setupStateStore.put(
                    chatId,
                    TimeAlarmSetupState(
                        step = TimeAlarmSetupStep.DAILY_TIME,
                        alarmType = AlarmType.DAILY_TIME
                    )
                )
                "매일 언제 받을까요? HH:mm 형식으로 입력해 주세요. 예: 09:30"
            }

            else -> "간격 또는 시간 중 하나로 답해 주세요."
        }

    private fun handleIntervalMinutes(chatId: Long, input: String): String {
        val minutes = input.toIntOrNull()
        if (minutes == null || minutes !in 1..1440) {
            return "간격은 1분에서 1440분 사이 숫자로 입력해 주세요."
        }

        val now = nowLocalDateTime()
        val nextTriggerAt = now.plusMinutes(minutes.toLong())
        val saved = alarmRepository.save(
            TelegramAlarmEntity(
                chatId = chatId,
                alarmType = AlarmType.INTERVAL,
                intervalMinutes = minutes,
                nextTriggerAt = nextTriggerAt,
                createdAt = now,
                updatedAt = now
            )
        )
        setupStateStore.clear(chatId)
        return "알람을 저장했어요. ${minutes}분 간격으로 보내드릴게요. 첫 알람 예정 시각: ${formatShort(saved.nextTriggerAt)}"
    }

    private fun handleDailyTime(chatId: Long, input: String): String {
        val time = parseTime(input)
            ?: return "시간 형식이 올바르지 않습니다. HH:mm 형식으로 입력해 주세요. 예: 21:30"

        val now = nowLocalDateTime()
        val nextTriggerAt = computeNextDailyTrigger(now, time)
        val saved = alarmRepository.save(
            TelegramAlarmEntity(
                chatId = chatId,
                alarmType = AlarmType.DAILY_TIME,
                timeOfDay = time,
                nextTriggerAt = nextTriggerAt,
                createdAt = now,
                updatedAt = now
            )
        )
        setupStateStore.clear(chatId)
        return "알람을 저장했어요. 매일 ${time.format(timeFormatter)}에 보내드릴게요. 첫 알람 예정 시각: ${formatShort(saved.nextTriggerAt)}"
    }

    private fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value.trim(), timeFormatter) }.getOrNull()

    private fun computeNextDailyTrigger(reference: LocalDateTime, timeOfDay: LocalTime): LocalDateTime {
        val todayCandidate = reference.toLocalDate().atTime(timeOfDay)
        return if (todayCandidate.isAfter(reference)) todayCandidate else todayCandidate.plusDays(1)
    }

    private fun isPositive(input: String): Boolean =
        input.lowercase() in setOf("예", "네", "응", "ㅇ", "yes", "y", "알람", "좋아")

    private fun isNegative(input: String): Boolean =
        input.lowercase() in setOf("아니오", "아니요", "아니", "no", "n")

    private fun isIntervalType(input: String): Boolean =
        input.lowercase() in setOf("간격", "시간간격", "interval", "반복")

    private fun isSpecificTimeType(input: String): Boolean =
        input.lowercase() in setOf("시간", "특정시간", "특정", "time", "daily")

    private fun isCancelInput(input: String): Boolean =
        input.lowercase() in setOf("취소", "cancel", "그만", "bye")

    private fun nowZonedDateTime(): ZonedDateTime = ZonedDateTime.now(resolveZoneId())

    private fun nowLocalDateTime(): LocalDateTime = nowZonedDateTime().toLocalDateTime()

    private fun resolveZoneId(): ZoneId =
        runCatching { ZoneId.of(properties.alarm.zoneId) }.getOrElse { ZoneId.of("Asia/Seoul") }

    private fun format(dateTime: ZonedDateTime): String = dateTime.format(formatter)

    private fun formatShort(dateTime: LocalDateTime): String = dateTime.atZone(resolveZoneId()).format(shortFormatter)
}
