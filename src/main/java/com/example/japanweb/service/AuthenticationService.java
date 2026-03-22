package com.example.japanweb.service;

import com.example.japanweb.dto.request.auth.AuthRequest;
import com.example.japanweb.dto.request.auth.ConfirmEmailVerificationRequest;
import com.example.japanweb.dto.request.auth.RegisterRequest;
import com.example.japanweb.dto.request.auth.ResendEmailVerificationRequest;
import com.example.japanweb.dto.response.auth.EmailVerificationStatusResponse;
import com.example.japanweb.entity.User;
import com.example.japanweb.entity.EmailVerificationToken;
import com.example.japanweb.exception.ApiException;
import com.example.japanweb.exception.ErrorCode;
import com.example.japanweb.repository.EmailVerificationTokenRepository;
import com.example.japanweb.repository.UserRepository;
import com.example.japanweb.security.AuthTokenStore;
import com.example.japanweb.security.JwtService;
import com.example.japanweb.config.properties.EmailVerificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthTokenStore authTokenStore;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationProperties emailVerificationProperties;
    private final EmailVerificationNotificationService emailVerificationNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public EmailVerificationStatusResponse register(RegisterRequest request) {
        if (repository.existsByUsername(request.getUsername())) {
            throw new ApiException(ErrorCode.AUTH_USERNAME_EXISTS);
        }
        if (repository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.AUTH_EMAIL_EXISTS);
        }

        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .emailVerified(false)
                .build();
        repository.save(user);
        return issueAndSendVerificationToken(user);
    }

    public IssuedTokens authenticate(AuthRequest request) {
        var user = repository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        if (!user.isEmailVerified()) {
            throw new ApiException(
                    ErrorCode.AUTH_EMAIL_NOT_VERIFIED,
                    "Please verify your email before signing in"
            );
        }
        return issueTokens(user);
    }

    public IssuedTokens refresh(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        if (username == null) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID, "Refresh token subject is missing");
        }

        User user = repository.findByUsername(username)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_USER_NOT_FOUND));

        if (!jwtService.isRefreshToken(refreshToken) || !jwtService.isTokenValid(refreshToken, user)) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID, "Refresh token is invalid");
        }

        String refreshTokenId = jwtService.extractTokenId(refreshToken);
        if (refreshTokenId == null || !authTokenStore.isRefreshTokenActive(refreshTokenId, username)) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID, "Refresh token is revoked or expired");
        }

        // Rotate refresh token to prevent replay.
        authTokenStore.revokeRefreshToken(refreshTokenId);
        return issueTokens(user);
    }

    public void logout(String accessToken, String refreshToken) {
        revokeAccessToken(accessToken);
        revokeRefreshToken(refreshToken);
    }

    @Transactional
    public IssuedTokens confirmEmailVerification(ConfirmEmailVerificationRequest request) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenHash(hashToken(request.getToken()))
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_EMAIL_VERIFICATION_INVALID));

        User user = verificationToken.getUser();
        if (user.isEmailVerified()) {
            throw new ApiException(ErrorCode.AUTH_EMAIL_ALREADY_VERIFIED);
        }
        if (verificationToken.getUsedAt() != null) {
            throw new ApiException(ErrorCode.AUTH_EMAIL_VERIFICATION_INVALID);
        }
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.AUTH_EMAIL_VERIFICATION_EXPIRED);
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        verificationToken.setUsedAt(LocalDateTime.now());
        repository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public EmailVerificationStatusResponse resendEmailVerification(ResendEmailVerificationRequest request) {
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_USER_NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new ApiException(ErrorCode.AUTH_EMAIL_ALREADY_VERIFIED);
        }

        return issueAndSendVerificationToken(user);
    }

    private IssuedTokens issueTokens(User user) {
        String accessTokenId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();

        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put("email", user.getEmail());
        accessClaims.put("role", user.getRole().name());
        String accessToken = jwtService.generateAccessToken(user, accessTokenId, accessClaims);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenId);

        authTokenStore.storeAccessToken(accessTokenId, user.getUsername(), jwtService.getAccessTokenExpiration());
        authTokenStore.storeRefreshToken(refreshTokenId, user.getUsername(), jwtService.getRefreshTokenExpiration());

        return new IssuedTokens(accessToken, refreshToken);
    }

    public record IssuedTokens(String accessToken, String refreshToken) {
    }

    private EmailVerificationStatusResponse issueAndSendVerificationToken(User user) {
        emailVerificationTokenRepository.deleteByUserAndUsedAtIsNull(user);

        String rawToken = generateVerificationToken();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plus(emailVerificationProperties.getTokenExpiration()))
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        try {
            emailVerificationNotificationService.sendVerificationEmail(user, rawToken);
        } catch (MailException ex) {
            throw new ApiException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    "Unable to send verification email right now",
                    ex
            );
        }

        return EmailVerificationStatusResponse.builder()
                .email(user.getEmail())
                .expiresInMinutes(emailVerificationProperties.getTokenExpiration().toMinutes())
                .build();
    }

    private String generateVerificationToken() {
        byte[] buffer = new byte[32];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private void revokeAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            String accessTokenId = jwtService.extractTokenId(accessToken);
            if (accessTokenId != null) {
                authTokenStore.revokeAccessToken(accessTokenId);
            }
        } catch (RuntimeException ignored) {
            // Ignore malformed or expired access token during logout.
        }
    }

    private void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            String refreshTokenId = jwtService.extractTokenId(refreshToken);
            if (refreshTokenId != null) {
                authTokenStore.revokeRefreshToken(refreshTokenId);
            }
        } catch (RuntimeException ignored) {
            // Ignore malformed or expired refresh token during logout.
        }
    }
}
