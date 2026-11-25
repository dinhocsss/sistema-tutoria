package com.tutoria.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerguntaRequest(
        @NotBlank(message = "A pergunta nao pode estar vazia")
        @Size(max = 4000, message = "A pergunta deve ter no maximo 4000 caracteres")
        String texto
) {
}
