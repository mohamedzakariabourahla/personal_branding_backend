package saas.personal_branding.api.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.personal_branding.api.application.exception.TokenException;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.domain.model.EmailVerificationToken;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.EmailVerificationTokenRepository;
import saas.personal_branding.api.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private TokenHashService tokenHashService;
    @Mock
    private EmailVerificationNotifier emailVerificationNotifier;
    @Mock
    private SecurityAuditLogger auditLogger;

    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final Duration tokenTtl = Duration.ofHours(24);
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(
                userRepository,
                tokenRepository,
                tokenHashService,
                emailVerificationNotifier,
                clock,
                "http://localhost/verify?token=",
                tokenTtl,
                auditLogger
        );
        
    }

    @Test
    void verifyTokenMarksUserVerifiedAndConsumesToken() {
        String rawToken = "raw-token";
        when(tokenHashService.hash(rawToken)).thenReturn("token-hash");

        EmailVerificationToken persistedToken = EmailVerificationToken.builder()
                .id(55L)
                .userId(7L)
                .tokenHash("token-hash")
                .createdAt(clock.instant().minus(Duration.ofHours(1)))
                .expiresAt(clock.instant().plus(Duration.ofHours(1)))
                .build();
        when(tokenRepository.findByTokenHash("token-hash")).thenReturn(Optional.of(persistedToken));

        User storedUser = User.builder()
                .id(7L)
                .email("user@example.com")
                .passwordHash("hash")
                .active(true)
                .emailVerified(false)
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(storedUser));

        service.verifyToken(rawToken);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updated = userCaptor.getValue();
        assertTrue(updated.isEmailVerified());
        assertEquals(clock.instant(), updated.getEmailVerifiedAt());

        verify(tokenRepository).markUsed(55L);
        verify(tokenRepository).deleteByUserId(7L);
        verify(auditLogger).log(eq("EMAIL_VERIFICATION_CONFIRMED"), any(Map.class));
    }

    @Test
    void verifyTokenRejectsExpiredTokens() {
        String rawToken = "raw-token";
        when(tokenHashService.hash(rawToken)).thenReturn("token-hash");

        EmailVerificationToken expired = EmailVerificationToken.builder()
                .id(33L)
                .userId(2L)
                .tokenHash("token-hash")
                .createdAt(clock.instant().minus(Duration.ofHours(48)))
                .expiresAt(clock.instant().minus(Duration.ofMinutes(1)))
                .build();
        when(tokenRepository.findByTokenHash("token-hash")).thenReturn(Optional.of(expired));

        assertThrows(TokenException.EmailVerificationTokenExpiredException.class, () -> service.verifyToken(rawToken));

        verify(tokenRepository).markUsed(33L);
        verify(auditLogger).log(eq("EMAIL_VERIFICATION_REJECTED"), argThat(map -> "TOKEN_EXPIRED".equals(map.get("reason"))));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resendVerificationThrowsWhenUserAlreadyVerified() {
        User verifiedUser = User.builder()
                .id(9L)
                .email("verified@example.com")
                .passwordHash("hash")
                .active(true)
                .emailVerified(true)
                .build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(verifiedUser));

        assertThrows(UserException.EmailAlreadyVerifiedException.class, () -> service.resendVerification(9L));

        verify(tokenRepository, never()).save(any());
        verify(emailVerificationNotifier, never()).sendEmailVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resendVerificationForEmailSkipsBlankValues() {
        assertTrue(service.resendVerificationForEmail(null).isEmpty());
        assertTrue(service.resendVerificationForEmail(" ").isEmpty());
    }

    @Test
    void resendVerificationForEmailIssuesNewToken() {
        User pendingUser = User.builder()
                .id(15L)
                .email("pending@example.com")
                .passwordHash("hash")
                .active(true)
                .emailVerified(false)
                .build();
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pendingUser));

        when(tokenHashService.hash(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "-hash");
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Instant> maybeExpires = service.resendVerificationForEmail(" Pending@Example.COM ");

        assertTrue(maybeExpires.isPresent());
        assertEquals(clock.instant().plus(tokenTtl), maybeExpires.get());

        verify(tokenRepository).deleteByUserId(15L);
        verify(emailVerificationNotifier).sendEmailVerificationEmail(eq("pending@example.com"), contains("http://localhost/verify?token="), anyString());
    }
}
