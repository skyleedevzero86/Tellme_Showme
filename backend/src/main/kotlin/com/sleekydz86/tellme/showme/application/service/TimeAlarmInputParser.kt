package com.sleekydz86.tellme.showme.application.service

import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class TimeAlarmInputParser {

    fun parseSchedule(input: String): ParsedAlarmInput? =
        parseDailyTime(input) ?: parseInterval(input)

    fun parseInterval(input: String): ParsedAlarmInput? {
        val normalized = normalize(input)
        if (normalized.isBlank()) {
            return null
        }

        normalized.toIntOrNull()?.let { minutes ->
            return when {
                minutes in 1..60 -> ParsedAlarmInput.Interval(minutes)
                minutes > 60 -> ParsedAlarmInput.AmbiguousLongMinutes(minutes)
                else -> null
            }
        }

        if (CLOCK_TEXT_REGEX.containsMatchIn(normalized) || CLOCK_COLON_REGEX.containsMatchIn(normalized)) {
            return null
        }

        val hasHourUnit = HOUR_REGEX.containsMatchIn(normalized)
        val minuteMatches = MINUTE_REGEX.findAll(normalized).toList()
        val hasMinuteUnit = minuteMatches.isNotEmpty()
        if (!hasHourUnit && !hasMinuteUnit) {
            return null
        }

        val hours = HOUR_REGEX.findAll(normalized).sumOf { it.groupValues[1].toInt() }
        val minutesOnly = minuteMatches.sumOf { it.groupValues[1].toInt() }
        val totalMinutes = (hours * 60) + minutesOnly

        if (totalMinutes <= 0) {
            return null
        }
        if (hours == 0 && minutesOnly > 60) {
            return ParsedAlarmInput.AmbiguousLongMinutes(minutesOnly)
        }
        if (totalMinutes > MAX_INTERVAL_MINUTES) {
            return ParsedAlarmInput.TooLargeInterval(totalMinutes)
        }

        return ParsedAlarmInput.Interval(totalMinutes)
    }

    fun parseDailyTime(input: String): ParsedAlarmInput.DailyTime? {
        val normalized = normalize(input)
        if (normalized.isBlank()) {
            return null
        }

        if (normalized.contains("자정")) {
            return ParsedAlarmInput.DailyTime(LocalTime.MIDNIGHT)
        }
        if (normalized.contains("정오")) {
            return ParsedAlarmInput.DailyTime(LocalTime.NOON)
        }

        CLOCK_COLON_REGEX.find(normalized)?.let { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            if (hour in 0..23 && minute in 0..59) {
                return ParsedAlarmInput.DailyTime(LocalTime.of(hour, minute))
            }
        }

        CLOCK_TEXT_REGEX.find(normalized)?.let { match ->
            val meridiem = match.groupValues[1]
            val rawHour = match.groupValues[2].toInt()
            val minute = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.toInt() ?: 0
            if (minute !in 0..59) {
                return null
            }

            val convertedHour = convertClockHour(rawHour, meridiem) ?: return null
            return ParsedAlarmInput.DailyTime(LocalTime.of(convertedHour, minute))
        }

        return null
    }

    private fun convertClockHour(rawHour: Int, meridiem: String): Int? {
        val token = meridiem.lowercase()
        return when (token) {
            "" -> rawHour.takeIf { it in 0..23 }
            "오전", "am", "아침", "새벽" -> when (rawHour) {
                in 1..11 -> rawHour
                12 -> 0
                else -> null
            }

            "오후", "pm", "저녁", "밤" -> when (rawHour) {
                in 1..11 -> rawHour + 12
                12 -> 12
                else -> null
            }

            else -> null
        }
    }

    private fun normalize(input: String): String =
        input.trim()
            .replace("한시간", "1시간")
            .replace("한 시간", "1시간")
            .replace(Regex("\\s+"), " ")

    sealed interface ParsedAlarmInput {
        data class Interval(val minutes: Int) : ParsedAlarmInput
        data class DailyTime(val time: LocalTime) : ParsedAlarmInput
        data class AmbiguousLongMinutes(val minutes: Int) : ParsedAlarmInput
        data class TooLargeInterval(val minutes: Int) : ParsedAlarmInput
    }

    companion object {
        private const val MAX_INTERVAL_MINUTES = 1440
        private val HOUR_REGEX = Regex("""(\d{1,2})\s*시간""")
        private val MINUTE_REGEX = Regex("""(\d{1,4})\s*분(?:\s*(?:뒤|후|간격|마다))?""")
        private val CLOCK_COLON_REGEX = Regex("""(?<!\d)(\d{1,2})\s*:\s*(\d{1,2})(?!\d)""")
        private val CLOCK_TEXT_REGEX =
            Regex("""(?:(오전|오후|am|pm|아침|새벽|저녁|밤)\s*)?(\d{1,2})\s*시(?!간)(?:\s*(\d{1,2})\s*분)?""")
    }
}
