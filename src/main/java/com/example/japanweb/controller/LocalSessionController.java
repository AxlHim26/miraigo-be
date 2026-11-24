package com.example.japanweb.controller;

import com.example.japanweb.dto.common.ApiResponse;
import com.example.japanweb.entity.User;
import com.example.japanweb.exception.ApiException;
import com.example.japanweb.exception.ErrorCode;
import com.example.japanweb.repository.UserRepository;
import com.example.japanweb.security.AuthTokenStore;
import com.example.japanweb.security.JwtService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/local-session")
@Profile("local")
@ConditionalOnProperty(name = "app.security.local-session.enabled", havingValue = "true")
@RequiredArgsConstructor
public class LocalSessionController {
    private static final String USERNAME = "miraigo-local-preview";
    private static final String EMAIL = "preview@miraigo.local";
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthTokenStore tokens;
    private final EntityManager entityManager;

    public record Session(String token) {}

    @PostMapping
    @Transactional
    public ApiResponse<Session> create() {
        // Serialize first-use provisioning across simultaneous local clients until commit.
        entityManager.createNativeQuery("select pg_advisory_xact_lock(73121941)").getResultList();
        User user = users.findByUsername(USERNAME).orElseGet(() -> {
            User preview = User.builder()
                .username(USERNAME)
                .email(EMAIL)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(User.Role.USER)
                .emailVerified(true)
                .build();
            return users.saveAndFlush(preview);
        });
        if (user.getRole() != User.Role.USER || !EMAIL.equals(user.getEmail()) || !user.isEmailVerified()) {
            throw new ApiException(ErrorCode.AUTH_ACCESS_DENIED);
        }
        String id = UUID.randomUUID().toString();
        String token = jwtService.generateAccessToken(user, id, Map.of("email", EMAIL, "role", "USER"));
        tokens.storeAccessToken(id, USERNAME, jwtService.getAccessTokenExpiration());
        return ApiResponse.success(new Session(token));
    }
}
