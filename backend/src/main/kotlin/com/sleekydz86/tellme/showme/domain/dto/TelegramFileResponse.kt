package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import lombok.Data

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class TelegramFileResponse {
    @JsonProperty("ok")
    private val ok: Boolean? = null

    @JsonProperty("result")
    private val result: FileResult? = null

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class FileResult {
        @JsonProperty("file_id")
        private val fileId: String? = null

        @JsonProperty("file_unique_id")
        private val fileUniqueId: String? = null

        @JsonProperty("file_size")
        private val fileSize: Long? = null

        @JsonProperty("file_path")
        private val filePath: String? = null
    }
}