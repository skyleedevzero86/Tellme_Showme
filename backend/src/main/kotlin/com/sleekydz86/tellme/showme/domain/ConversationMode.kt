package com.sleekydz86.tellme.showme.domain

enum class ConversationMode(val aiMode: String, val label: String) {
    ENG("eng", "영어 대화"),
    GOD("god", "명언 대화");

    companion object {
        fun fromAiMode(value: String?): ConversationMode? =
            entries.find { it.aiMode.equals(value?.trim(), ignoreCase = true) }
    }
}
