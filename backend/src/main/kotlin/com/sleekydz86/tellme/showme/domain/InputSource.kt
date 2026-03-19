package com.sleekydz86.tellme.showme.domain

enum class InputSource {
    TELEGRAM,
    FRONTEND;

    fun supportsAlarmSetup(): Boolean = this == TELEGRAM
}
