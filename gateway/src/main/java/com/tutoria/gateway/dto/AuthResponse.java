package com.tutoria.gateway.dto;

public record AuthResponse(
        String token,
        String userId,
        String username
) {
}
