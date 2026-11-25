package com.tutoria.gateway.controller;

import com.tutoria.gateway.dto.AuthRequest;
import com.tutoria.gateway.dto.AuthResponse;
import com.tutoria.gateway.security.InMemoryUserService;
import com.tutoria.gateway.security.InMemoryUserService.UserRecord;
import com.tutoria.gateway.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final InMemoryUserService userService;
    private final JwtService jwtService;

    public AuthController(InMemoryUserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        Optional<UserRecord> userOpt = userService.authenticate(request.username(), request.password());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario ou senha invalidos"));
        }
        UserRecord user = userOpt.get();
        String token = jwtService.generateToken(user.userId(), user.username());
        return ResponseEntity.ok(new AuthResponse(token, user.userId(), user.username()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        Optional<UserRecord> userOpt = userService.register(request.username(), request.password());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Nome de usuario ja cadastrado"));
        }
        UserRecord user = userOpt.get();
        String token = jwtService.generateToken(user.userId(), user.username());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.userId(), user.username()));
    }
}
