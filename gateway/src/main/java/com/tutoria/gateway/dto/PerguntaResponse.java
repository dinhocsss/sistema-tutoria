package com.tutoria.gateway.dto;

import java.time.Instant;

public record PerguntaResponse(
        String correlationId,
        String status,
        Instant timestamp
) {
}
