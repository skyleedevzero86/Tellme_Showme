package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SendMessageResponse(
    @JsonProperty("status")
    val status: String? = null,
    @JsonProperty("message")
    val message: String? = null
)
