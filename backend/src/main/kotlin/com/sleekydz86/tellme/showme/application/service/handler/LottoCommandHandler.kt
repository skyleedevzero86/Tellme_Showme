package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.application.port.ExternalContentPort
import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class LottoCommandHandler(private val externalContent: ExternalContentPort) : CommandHandler {
    override fun handle(ctx: MessageContext?): Mono<String> {
        return Mono.justOrEmpty(externalContent.lottoNumbers)
            .map { numbers -> String.format(TEMPLATE, numbers) }
    }

    companion object {
        private const val TEMPLATE = "봇이 추측한 로또 번호는 %s 입니다."
    }
}
