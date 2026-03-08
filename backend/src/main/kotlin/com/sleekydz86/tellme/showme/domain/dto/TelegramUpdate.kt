package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import lombok.Data


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class TelegramUpdate {
    @JsonProperty("ok")
    private val ok: Boolean? = null

    @JsonProperty("result")
    private val result: MutableList<UpdateResult?>? = null

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class UpdateResult {
        @JsonProperty("update_id")
        private val updateId: Long? = null

        @JsonProperty("message")
        private val message: Message? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Message {
        @JsonProperty("message_id")
        private val messageId: Long? = null

        @JsonProperty("from")
        private val from: User? = null

        @JsonProperty("chat")
        private val chat: Chat? = null

        @JsonProperty("date")
        private val date: Long? = null

        @JsonProperty("text")
        private val text: String? = null

        @JsonProperty("document")
        private val document: Document? = null

        @JsonProperty("photo")
        private val photo: MutableList<PhotoSize?>? = null

        @JsonProperty("voice")
        private val voice: Voice? = null

        @JsonProperty("video")
        private val video: Video? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class User {
        @JsonProperty("id")
        private val id: Long? = null

        @JsonProperty("is_bot")
        private val isBot: Boolean? = null

        @JsonProperty("first_name")
        private val firstName: String? = null

        @JsonProperty("username")
        private val username: String? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Chat {
        @JsonProperty("id")
        private val id: Long? = null

        @JsonProperty("type")
        private val type: String? = null

        @JsonProperty("title")
        private val title: String? = null

        @JsonProperty("username")
        private val username: String? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Document {
        @JsonProperty("file_id")
        private val fileId: String? = null

        @JsonProperty("file_unique_id")
        private val fileUniqueId: String? = null

        @JsonProperty("file_name")
        private val fileName: String? = null

        @JsonProperty("mime_type")
        private val mimeType: String? = null

        @JsonProperty("file_size")
        private val fileSize: Long? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class PhotoSize {
        @JsonProperty("file_id")
        private val fileId: String? = null

        @JsonProperty("file_unique_id")
        private val fileUniqueId: String? = null

        @JsonProperty("width")
        private val width: Int? = null

        @JsonProperty("height")
        private val height: Int? = null

        @JsonProperty("file_size")
        private val fileSize: Long? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Voice {
        @JsonProperty("file_id")
        private val fileId: String? = null

        @JsonProperty("file_unique_id")
        private val fileUniqueId: String? = null

        @JsonProperty("duration")
        private val duration: Int? = null

        @JsonProperty("mime_type")
        private val mimeType: String? = null

        @JsonProperty("file_size")
        private val fileSize: Long? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Video {
        @JsonProperty("file_id")
        private val fileId: String? = null

        @JsonProperty("file_unique_id")
        private val fileUniqueId: String? = null

        @JsonProperty("width")
        private val width: Int? = null

        @JsonProperty("height")
        private val height: Int? = null

        @JsonProperty("duration")
        private val duration: Int? = null

        @JsonProperty("mime_type")
        private val mimeType: String? = null

        @JsonProperty("file_size")
        private val fileSize: Long? = null
    }
}
