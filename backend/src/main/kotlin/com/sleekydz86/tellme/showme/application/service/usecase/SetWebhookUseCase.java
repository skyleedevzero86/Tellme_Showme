package com.sleekydz86.tellme.showme.application.service.usecase;

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort;
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SetWebhookUseCase {

    private final TelegramApiPort telegramApi;

    public Mono<TelegramSendResponse> setWebhook(boolean enabled) {
        return telegramApi.setWebhook(enabled)
                .onErrorResume(e -> Mono.just(errorResponse(e.getMessage())));
    }

    private static TelegramSendResponse errorResponse(String description) {
        TelegramSendResponse r = new TelegramSendResponse();
        r.setOk(false);
        r.setDescription(description);
        return r;
    }
}