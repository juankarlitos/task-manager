package com.taskmanager.service;

import com.taskmanager.model.PasswordResetToken;
import com.taskmanager.model.User;
import com.taskmanager.repository.PasswordResetTokenRepository;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES  = 30;
    private static final int RATE_LIMIT_MAX         = 3;
    private static final int RATE_LIMIT_WINDOW_MIN  = 15;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void requestReset(String email) {
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            return;
        }
        User user = optUser.get();

        LocalDateTime since = LocalDateTime.now().minusMinutes(RATE_LIMIT_WINDOW_MIN);
        if (tokenRepository.countRecentByUser(user, since) >= RATE_LIMIT_MAX) {
            log.warn("Rate limit de reset superado para usuario '{}'", user.getUsername());
            return;
        }

        tokenRepository.invalidateAllForUser(user);

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken  = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = sha256(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .build();
        tokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetEmail(user, rawToken);
        } catch (Exception e) {
            log.error("Error al enviar correo de reset a '{}': {}", email, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = sha256(rawToken);

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("El token ya fue utilizado");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token ha expirado");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        try {
            emailService.sendPasswordChangedEmail(user);
        } catch (Exception e) {
            log.error("Error al enviar correo de confirmación a '{}': {}", user.getEmail(), e.getMessage());
        }
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
