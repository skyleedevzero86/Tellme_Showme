package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import lombok.Data


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class TelegramSendResponse {
    @JsonProperty("ok")
    private val ok: Boolean? = null

    @JsonProperty("description")
    private val description: String? = null

    @JsonProperty("result")
    private val result: Any? = null
}
