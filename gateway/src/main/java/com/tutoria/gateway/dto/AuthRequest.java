package com.tutoria.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank(message = "O nome de usuario e obrigatorio")
        @Size(min = 3, max = 50, message = "O usuario deve ter entre 3 e 50 caracteres")
        String username,

        @NotBlank(message = "A senha e obrigatoria")
        @Size(min = 4, max = 100, message = "A senha deve ter no minimo 4 caracteres")
        String password
) {
}
