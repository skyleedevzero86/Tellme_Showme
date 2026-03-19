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
            val commandToken = text.trim()
                .substringBefore(" ")
                .substringBefore("@")
            return entries.find { it.matches(commandToken, allowBareAliases) }
        }
    }

    private fun matches(commandToken: String, allowBareAliases: Boolean): Boolean {
        if (value.equals(commandToken, ignoreCase = true)) {
            return true
        }
        if (!allowBareAliases) {
            return false
        }
        return aliases.any { alias -> alias.equals(commandToken, ignoreCase = true) }
    }
}
