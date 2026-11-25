package com.tutoria.gateway.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryUserService {

    public record UserRecord(String userId, String username, String passwordHash) {
    }

    private final Map<String, UserRecord> usersByUsername = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public InMemoryUserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        // Usuarios iniciais para testes rapidos e demonstracao de isolamento entre sessoes
        seedUser("user-aluno1", "aluno1", "123456");
        seedUser("user-aluno2", "aluno2", "123456");
    }

    private void seedUser(String userId, String username, String rawPassword) {
        usersByUsername.put(username.toLowerCase(), new UserRecord(userId, username, passwordEncoder.encode(rawPassword)));
    }

    public Optional<UserRecord> register(String username, String rawPassword) {
        String normalized = username.trim().toLowerCase();
        UserRecord newUser = new UserRecord(
                "user-" + UUID.randomUUID().toString().substring(0, 8),
                username.trim(),
                passwordEncoder.encode(rawPassword)
        );
        UserRecord existing = usersByUsername.putIfAbsent(normalized, newUser);
        return existing == null ? Optional.of(newUser) : Optional.empty();
    }

    public Optional<UserRecord> authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return Optional.empty();
        }
        UserRecord user = usersByUsername.get(username.trim().toLowerCase());
        if (user == null || !passwordEncoder.matches(rawPassword, user.passwordHash())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }
}
