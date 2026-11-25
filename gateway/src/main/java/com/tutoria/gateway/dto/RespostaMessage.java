package com.tutoria.gateway.dto;

import java.time.Instant;

public record RespostaMessage(
        String correlationId,
        String userId,
        String pergunta,
        String resposta,
        String status,
        Instant timestamp
) {
}
