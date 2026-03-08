package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonProperty
import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class SendMessageResponse {
    @JsonProperty("status")
    private var status: String? = null

    @JsonProperty("message")
    private var message: String? = null
}