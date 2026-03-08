package com.sleekydz86.tellme.showme.infrastructure.adapter

import com.sleekydz86.tellme.global.config.TelegramBotProperties
import com.sleekydz86.tellme.showme.application.port.ExternalContentPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors


@Component
class ExternalContentAdapter(
    private val properties: TelegramBotProperties
) : ExternalContentPort {
    private val log = LoggerFactory.getLogger(ExternalContentAdapter::class.java)
    private val objectMapper = ObjectMapper()

    override val lottoNumbers: String?
        get() = computeLottoNumbers()

    override val bible: String?
        get() = fetchBible()

    override val english: String?
        get() = fetchEnglish()

    private fun computeLottoNumbers(): String {
        val numbers = IntArray(POOL_SIZE)
        for (i in numbers.indices) {
            numbers[i] = i + 1
        }
        for (i in 0..<SHUFFLE_ROUNDS) {
            val pick = (Math.random() * POOL_SIZE).toInt()
            val temp = numbers[0]
            numbers[0] = numbers[pick]
            numbers[pick] = temp
        }
        val builder = StringBuilder()
        for (i in 0..<PICK_COUNT) {
            if (i > 0) {
                builder.append(", ")
            }
            builder.append(numbers[i])
        }
        return builder.toString()
    }

    private fun fetchBible(): String? {
        val apiUrl = nullToEmpty(properties.bibleApiUrl)
        if (apiUrl.isBlank()) {
            return BIBLE_PLACEHOLDER
        }
        try {
            val body = fetchGet(apiUrl)
            val root = objectMapper.readTree(body)
            if (root != null && root.has("saying_eng") && root.has("saying_kor")) {
                return root.get("saying_eng").asText() + "\n" + root.get("saying_kor").asText()
            }
        } catch (e: Exception) {
            log.debug("Bible API error", e)
        }
        return ERROR_MESSAGE
    }

    private fun fetchEnglish(): String? {
        val apiUrl = nullToEmpty(properties.englishApiUrl)
        if (apiUrl.isBlank()) {
            return ENGLISH_PLACEHOLDER
        }
        try {
            return fetchGet(apiUrl)
        } catch (e: Exception) {
            log.debug("English API error", e)
        }
        return ERROR_MESSAGE
    }

    @Throws(Exception::class)
    private fun fetchGet(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestMethod("GET")
        conn.setDoInput(true)
        try {
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return ERROR_MESSAGE
            }
            return BufferedReader(InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining())
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val POOL_SIZE = 45
        private const val SHUFFLE_ROUNDS = 100
        private const val PICK_COUNT = 6
        private const val BIBLE_PLACEHOLDER = "성경 API URL을 application.yml 등에 설정하면 구절을 불러옵니다."
        private const val ENGLISH_PLACEHOLDER = "영어 API URL을 application.yml 등에 설정하면 문장을 불러옵니다."
        private const val ERROR_MESSAGE = "오류가 발생했습니다."

        private fun nullToEmpty(s: String?): String {
            return if (s != null) s else ""
        }
    }
}
