package com.sleekydz86.tellme.aiserver.infrastructure.web

import com.sleekydz86.tellme.aiserver.aplication.port.RateLimitPort
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RateLimitInterceptor(
    private val rateLimitPort: RateLimitPort,
    @Value("\${app.rate-limit.max-requests:60}") private val maxRequests: Int,
    @Value("\${app.rate-limit.window-seconds:60}") private val windowSeconds: Long
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val key = request.getHeader("X-User-Id") ?: request.remoteAddr ?: "anonymous"
        val rateKey = "api:$key"
        if (!rateLimitPort.isAllowed(rateKey, maxRequests, windowSeconds)) {
            response.status = 429
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"status":429,"msg":"요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."}""")
            return false
        }
        return true
    }
}
