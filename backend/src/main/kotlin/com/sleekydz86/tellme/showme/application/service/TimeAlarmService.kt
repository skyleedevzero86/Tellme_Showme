package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.global.config.TelegramBotProperties
import com.sleekydz86.tellme.showme.application.service.TimeAlarmInputParser.ParsedAlarmInput
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
    private val properties: TelegramBotProperties,
    private val inputParser: TimeAlarmInputParser
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

    fun stopActiveAlarms(chatId: Long): Int {
        val alarms = alarmRepository.findByChatIdAndActiveTrue(chatId)
        if (alarms.isEmpty()) {
            return 0
        }

        val now = nowLocalDateTime()
        alarms.forEach { alarm ->
            alarm.active = false
            alarm.updatedAt = now
        }
        alarmRepository.saveAll(alarms)
        return alarms.size
    }

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
            TimeAlarmSetupStep.MESSAGE -> handleMessage(chatId, state, normalized)
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
        val body = alarm.messageText?.trim().takeIf { !it.isNullOrBlank() }
            ?: "다시 한 번 현재 시각입니다. 알람 메시지입니다."
        return buildString {
            appendLine(body)
            appendLine()
            append("현재 시각: ${format(nowZonedDateTime())}")
        }
    }

    fun buildCurrentTimeMessage(): String = "현재 시각은 ${format(nowZonedDateTime())}입니다."

    private fun handleConfirm(chatId: Long, input: String): String {
        if (isNegative(input)) {
            setupStateStore.clear(chatId)
            return "알람 설정 없이 현재 시각만 안내했어요."
        }

        createAlarmIfPossible(chatId, input)?.let { return it }

        return if (isPositive(input)) {
            setupStateStore.put(chatId, TimeAlarmSetupState(TimeAlarmSetupStep.TYPE))
            "좋아요. 시간 간격으로 받을까요, 특정 시간에 받을까요? 간격 또는 시간 으로 답해 주세요. 예: 5분 뒤, 1시간 10분, 오후 9시 30분"
        } else {
            "알람을 받을지 먼저 알려주세요. 예 또는 아니오 로 답해 주세요. 바로 5분 뒤, 오후 9시 30분처럼 말해도 돼요."
        }
    }

    private fun handleType(chatId: Long, input: String): String {
        createAlarmIfPossible(chatId, input)?.let { return it }

        return when {
            isIntervalType(input) -> {
                setupStateStore.put(
                    chatId,
                    TimeAlarmSetupState(
                        step = TimeAlarmSetupStep.INTERVAL_MINUTES,
                        alarmType = AlarmType.INTERVAL
                    )
                )
                "몇 분 간격으로 받을까요? 숫자만 보내면 1분에서 60분까지 이해할 수 있어요. 1시간 이상이면 1시간 10분처럼 적어 주세요."
            }

            isSpecificTimeType(input) -> {
                setupStateStore.put(
                    chatId,
                    TimeAlarmSetupState(
                        step = TimeAlarmSetupStep.DAILY_TIME,
                        alarmType = AlarmType.DAILY_TIME
                    )
                )
                "몇 시 몇 분에 받을까요? 예: 21:30, 오후 9시 30분, 오전 7시"
            }

            else -> "간격 또는 시간 중 하나로 답해 주세요. 바로 5분 뒤, 1시간 1분, 오후 9시 30분처럼 말해도 됩니다."
        }
    }

    private fun handleIntervalMinutes(chatId: Long, input: String): String =
        when (val parsed = inputParser.parseInterval(input)) {
            is ParsedAlarmInput.Interval -> prepareAlarmMessage(chatId, AlarmType.INTERVAL, parsed.minutes, null)
            is ParsedAlarmInput.AmbiguousLongMinutes -> buildLongMinuteClarification(parsed.minutes)
            is ParsedAlarmInput.TooLargeInterval ->
                "간격은 최대 24시간까지만 설정할 수 있어요. 더 짧게 다시 입력해 주세요."
            is ParsedAlarmInput.DailyTime ->
                prepareAlarmMessage(chatId, AlarmType.DAILY_TIME, null, parsed.time)
            null -> "간격을 이해하지 못했어요. 5, 5분, 1시간, 1시간 10분처럼 입력해 주세요."
        }

    private fun handleDailyTime(chatId: Long, input: String): String =
        when (val parsed = inputParser.parseSchedule(input)) {
            is ParsedAlarmInput.DailyTime -> prepareAlarmMessage(chatId, AlarmType.DAILY_TIME, null, parsed.time)
            is ParsedAlarmInput.Interval -> prepareAlarmMessage(chatId, AlarmType.INTERVAL, parsed.minutes, null)
            is ParsedAlarmInput.AmbiguousLongMinutes -> buildLongMinuteClarification(parsed.minutes)
            is ParsedAlarmInput.TooLargeInterval ->
                "간격은 최대 24시간까지만 설정할 수 있어요. 더 짧게 다시 입력해 주세요."
            null -> "시간 형식을 이해하지 못했어요. 21:30, 오후 9시 30분, 오전 7시처럼 입력해 주세요."
        }

    private fun handleMessage(chatId: Long, state: TimeAlarmSetupState, input: String): String {
        val messageText = normalizeAlarmMessage(input)
        return when (state.alarmType) {
            AlarmType.INTERVAL -> {
                val minutes = state.intervalMinutes
                    ?: return resetBrokenSetup(chatId, "알람 간격 정보가 사라졌어요. /time 으로 다시 시작해 주세요.")
                saveIntervalAlarm(chatId, minutes, messageText)
            }

            AlarmType.DAILY_TIME -> {
                val time = state.timeOfDay
                    ?: return resetBrokenSetup(chatId, "알람 시간 정보가 사라졌어요. /time 으로 다시 시작해 주세요.")
                saveDailyAlarm(chatId, time, messageText)
            }

            null -> resetBrokenSetup(chatId, "알람 정보가 충분하지 않아요. /time 으로 다시 시작해 주세요.")
        }
    }

    private fun createAlarmIfPossible(chatId: Long, input: String): String? =
        when (val parsed = inputParser.parseSchedule(input)) {
            is ParsedAlarmInput.Interval -> prepareAlarmMessage(chatId, AlarmType.INTERVAL, parsed.minutes, null)
            is ParsedAlarmInput.DailyTime -> prepareAlarmMessage(chatId, AlarmType.DAILY_TIME, null, parsed.time)
            is ParsedAlarmInput.AmbiguousLongMinutes -> buildLongMinuteClarification(parsed.minutes)
            is ParsedAlarmInput.TooLargeInterval ->
                "간격은 최대 24시간까지만 설정할 수 있어요. 더 짧게 다시 입력해 주세요."
            null -> null
        }

    private fun prepareAlarmMessage(
        chatId: Long,
        alarmType: AlarmType,
        intervalMinutes: Int?,
        timeOfDay: LocalTime?
    ): String {
        setupStateStore.put(
            chatId,
            TimeAlarmSetupState(
                step = TimeAlarmSetupStep.MESSAGE,
                alarmType = alarmType,
                intervalMinutes = intervalMinutes,
                timeOfDay = timeOfDay
            )
        )

        val scheduleSummary = when (alarmType) {
            AlarmType.INTERVAL -> "${formatDuration(intervalMinutes ?: 1)} 간격"
            AlarmType.DAILY_TIME -> "매일 ${timeOfDay?.format(timeFormatter) ?: "09:00"}"
        }

        return "좋아요. 알람 문구를 입력해 주세요.\n" +
            "기본 문구를 쓰려면 기본 또는 skip 이라고 보내 주세요.\n" +
            "설정한 일정: $scheduleSummary"
    }

    private fun saveIntervalAlarm(chatId: Long, minutes: Int, messageText: String?): String {
        val now = nowLocalDateTime()
        val nextTriggerAt = now.plusMinutes(minutes.toLong())
        val saved = alarmRepository.save(
            TelegramAlarmEntity(
                chatId = chatId,
                alarmType = AlarmType.INTERVAL,
                intervalMinutes = minutes,
                messageText = messageText,
                nextTriggerAt = nextTriggerAt,
                createdAt = now,
                updatedAt = now
            )
        )
        setupStateStore.clear(chatId)
        return "알람을 저장했어요. ${formatDuration(minutes)} 간격으로 보내드릴게요.\n" +
            "알람 문구: ${describeAlarmMessage(messageText)}\n" +
            "첫 알람 예정 시각: ${formatShort(saved.nextTriggerAt)}\n" +
            "중지하려면 /alarmstop 또는 /end 를 입력해 주세요."
    }

    private fun saveDailyAlarm(chatId: Long, time: LocalTime, messageText: String?): String {
        val now = nowLocalDateTime()
        val nextTriggerAt = computeNextDailyTrigger(now, time)
        val saved = alarmRepository.save(
            TelegramAlarmEntity(
                chatId = chatId,
                alarmType = AlarmType.DAILY_TIME,
                timeOfDay = time,
                messageText = messageText,
                nextTriggerAt = nextTriggerAt,
                createdAt = now,
                updatedAt = now
            )
        )
        setupStateStore.clear(chatId)
        return "알람을 저장했어요. 매일 ${time.format(timeFormatter)}에 보내드릴게요.\n" +
            "알람 문구: ${describeAlarmMessage(messageText)}\n" +
            "첫 알람 예정 시각: ${formatShort(saved.nextTriggerAt)}\n" +
            "중지하려면 /alarmstop 또는 /end 를 입력해 주세요."
    }

    private fun computeNextDailyTrigger(reference: LocalDateTime, timeOfDay: LocalTime): LocalDateTime {
        val todayCandidate = reference.toLocalDate().atTime(timeOfDay)
        return if (todayCandidate.isAfter(reference)) todayCandidate else todayCandidate.plusDays(1)
    }

    private fun buildLongMinuteClarification(minutes: Int): String {
        val hours = minutes / 60
        val remainMinutes = minutes % 60
        val suggestion = buildString {
            if (hours > 0) {
                append("${hours}시간")
            }
            if (remainMinutes > 0) {
                if (isNotEmpty()) append(" ")
                append("${remainMinutes}분")
            }
        }.ifBlank { "1시간" }

        return "분으로만 60분을 넘게 입력하면 해석이 헷갈릴 수 있어요. ${suggestion}처럼 다시 정확히 말해 주세요."
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val remainMinutes = minutes % 60
        return buildString {
            if (hours > 0) {
                append("${hours}시간")
            }
            if (remainMinutes > 0) {
                if (isNotEmpty()) append(" ")
                append("${remainMinutes}분")
            }
            if (isEmpty()) {
                append("${minutes}분")
            }
        }
    }

    private fun normalizeAlarmMessage(input: String): String? =
        input.trim()
            .takeIf { it.isNotBlank() }
            ?.takeUnless { isDefaultMessageInput(it) }

    private fun describeAlarmMessage(messageText: String?): String =
        messageText?.takeIf { it.isNotBlank() } ?: "기본 문구"

    private fun resetBrokenSetup(chatId: Long, message: String): String {
        setupStateStore.clear(chatId)
        return message
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

    private fun isDefaultMessageInput(input: String): Boolean =
        input.lowercase() in setOf("기본", "skip", "건너뛰기", "없음", "default")

    private fun nowZonedDateTime(): ZonedDateTime = ZonedDateTime.now(resolveZoneId())

    private fun nowLocalDateTime(): LocalDateTime = nowZonedDateTime().toLocalDateTime()

    private fun resolveZoneId(): ZoneId =
        runCatching { ZoneId.of(properties.alarm.zoneId) }.getOrElse { ZoneId.of("Asia/Seoul") }

    private fun format(dateTime: ZonedDateTime): String = dateTime.format(formatter)

    private fun formatShort(dateTime: LocalDateTime): String = dateTime.atZone(resolveZoneId()).format(shortFormatter)
}
