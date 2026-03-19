package com.sleekydz86.tellme.showme.domain

enum class BotCommand(
    val value: String,
    private vararg val aliases: String
) {
    START("/start", "start"),
    TIME("/time", "time"),
    HELP("/help", "help"),
    LOTTO("/lotto", "lotto"),
    GOD("/god", "god"),
    ENG("/eng", "eng"),
    ALARM_STOP("/alarmstop", "alarmstop", "alarm-stop", "알람중지", "알람중단"),
    END("/end", "end"),
    SEARCH("/search", "search");

    companion object {
        fun from(text: String?, allowBareAliases: Boolean = false): BotCommand? {
            if (text == null || text.isBlank()) return null
            val trimmed = text.trim()
            val commandToken = trimmed
                .substringBefore(" ")
                .substringBefore("@")
            entries.find { it.value.equals(commandToken, ignoreCase = true) }?.let { return it }
            if (!allowBareAliases) {
                return null
            }
            return entries.find { it.matchesBareAlias(trimmed) }
        }
    }

    private fun matchesBareAlias(text: String): Boolean {
        return aliases.any { alias -> alias.equals(text, ignoreCase = true) }
    }
}
