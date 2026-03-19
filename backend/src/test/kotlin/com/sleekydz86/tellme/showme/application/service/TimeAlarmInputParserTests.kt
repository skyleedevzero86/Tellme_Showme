package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.application.service.TimeAlarmInputParser.ParsedAlarmInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.LocalTime

class TimeAlarmInputParserTests {

    private val parser = TimeAlarmInputParser()

    @Test
    fun `parse plain minute number`() {
        val result = parser.parseInterval("5")

        assertInstanceOf(ParsedAlarmInput.Interval::class.java, result)
        assertEquals(5, (result as ParsedAlarmInput.Interval).minutes)
    }

    @Test
    fun `parse relative minute expression`() {
        val result = parser.parseSchedule("5분 뒤 해줘")

        assertInstanceOf(ParsedAlarmInput.Interval::class.java, result)
        assertEquals(5, (result as ParsedAlarmInput.Interval).minutes)
    }

    @Test
    fun `parse hour and minute interval expression`() {
        val result = parser.parseSchedule("1시간 1분")

        assertInstanceOf(ParsedAlarmInput.Interval::class.java, result)
        assertEquals(61, (result as ParsedAlarmInput.Interval).minutes)
    }

    @Test
    fun `request clarification for long minute only expression`() {
        val result = parser.parseSchedule("90분")

        assertInstanceOf(ParsedAlarmInput.AmbiguousLongMinutes::class.java, result)
        assertEquals(90, (result as ParsedAlarmInput.AmbiguousLongMinutes).minutes)
    }

    @Test
    fun `parse daily time with meridiem`() {
        val result = parser.parseSchedule("오후 9시 30분")

        assertInstanceOf(ParsedAlarmInput.DailyTime::class.java, result)
        assertEquals(LocalTime.of(21, 30), (result as ParsedAlarmInput.DailyTime).time)
    }

    @Test
    fun `parse daily time with colon`() {
        val result = parser.parseSchedule("21:30")

        assertInstanceOf(ParsedAlarmInput.DailyTime::class.java, result)
        assertEquals(LocalTime.of(21, 30), (result as ParsedAlarmInput.DailyTime).time)
    }
}
