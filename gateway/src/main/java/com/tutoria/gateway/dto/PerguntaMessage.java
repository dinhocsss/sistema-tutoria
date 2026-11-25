package com.tutoria.gateway.dto;

import java.time.Instant;

public record PerguntaMessage(
        String correlationId,
        String userId,
        String texto,
        Instant timestamp
) {
}
