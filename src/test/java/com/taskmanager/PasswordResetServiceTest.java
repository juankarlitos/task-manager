package com.taskmanager;

import com.taskmanager.model.PasswordResetToken;
import com.taskmanager.model.User;
import com.taskmanager.repository.PasswordResetTokenRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.service.EmailService;
import com.taskmanager.service.PasswordResetServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private PasswordResetServiceImpl service;

    // ── fixtures ──────────────────────────────────────────────────────────────

    private User buildUser() {
        return User.builder()
                .id(1L)
                .username("juankita")
                .email("juankita@example.com")
                .password("$2a$10$hash")
                .role("ROLE_USER")
                .build();
    }

    private PasswordResetToken validToken(User user, String rawToken) {
        return PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .tokenHash(PasswordResetServiceImpl.sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(25))
                .used(false)
                .build();
    }

    // ── requestReset ──────────────────────────────────────────────────────────

    @Test
    void requestReset_cuandoCorreoNoExiste_retornaSilenciosamenteSinCrearToken() {
        when(userRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        service.requestReset("noexiste@example.com");

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void requestReset_conUsuarioValido_creaTokenYEnviaCorreo() {
        User user = buildUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.countRecentByUser(eq(user), any())).thenReturn(0L);

        service.requestReset(user.getEmail());

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).invalidateAllForUser(user);
        verify(tokenRepository).save(captor.capture());

        PasswordResetToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTokenHash()).isNotBlank().hasSize(64);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(saved.isUsed()).isFalse();

        verify(emailService).sendPasswordResetEmail(eq(user), argThat(t -> t != null && !t.isBlank()));
    }

    @Test
    void requestReset_superaRateLimit_noEnviaCorreoNiCreaToken() {
        User user = buildUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.countRecentByUser(eq(user), any())).thenReturn(3L);

        service.requestReset(user.getEmail());

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void requestReset_fallaElCorreo_noLanzaExcepcion() {
        User user = buildUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.countRecentByUser(eq(user), any())).thenReturn(0L);
        doThrow(new RuntimeException("Brevo no disponible"))
                .when(emailService).sendPasswordResetEmail(any(), any());

        service.requestReset(user.getEmail());

        verify(tokenRepository).save(any());
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Test
    void resetPassword_conTokenValido_cambiaContrasenaYMarcaTokenUsado() {
        String rawToken = "tokenValido123";
        User user = buildUser();
        PasswordResetToken token = validToken(user, rawToken);

        when(tokenRepository.findByTokenHash(PasswordResetServiceImpl.sha256(rawToken)))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("nuevaClave123")).thenReturn("$2a$10$nuevo");

        service.resetPassword(rawToken, "nuevaClave123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$10$nuevo");
        assertThat(userCaptor.getValue().getPasswordChangedAt()).isNotNull();

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().isUsed()).isTrue();

        verify(emailService).sendPasswordChangedEmail(user);
    }

    @Test
    void resetPassword_conTokenYaUsado_lanzaIllegalArgument() {
        String rawToken = "tokenUsado";
        User user = buildUser();
        PasswordResetToken used = PasswordResetToken.builder()
                .id(2L).user(user)
                .tokenHash(PasswordResetServiceImpl.sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true)
                .build();

        when(tokenRepository.findByTokenHash(PasswordResetServiceImpl.sha256(rawToken)))
                .thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.resetPassword(rawToken, "nueva1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya fue utilizado");
    }

    @Test
    void resetPassword_conTokenExpirado_lanzaIllegalArgument() {
        String rawToken = "tokenExpirado";
        User user = buildUser();
        PasswordResetToken expired = PasswordResetToken.builder()
                .id(3L).user(user)
                .tokenHash(PasswordResetServiceImpl.sha256(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();

        when(tokenRepository.findByTokenHash(PasswordResetServiceImpl.sha256(rawToken)))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword(rawToken, "nueva1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void resetPassword_conHashInexistente_lanzaIllegalArgument() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("tokenFalso", "nueva1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void sha256_produceSiempreElMismoHash() {
        String h1 = PasswordResetServiceImpl.sha256("test");
        String h2 = PasswordResetServiceImpl.sha256("test");
        assertThat(h1).isEqualTo(h2).hasSize(64);
    }
}
