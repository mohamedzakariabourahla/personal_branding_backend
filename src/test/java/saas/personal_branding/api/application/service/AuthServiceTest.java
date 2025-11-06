package saas.personal_branding.api.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.domain.model.OnboardingStatus;
import saas.personal_branding.api.domain.model.RefreshToken;
import saas.personal_branding.api.domain.model.Role;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.RefreshTokenRepository;
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
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private TokenHashService tokenHashService;
    @Mock
    private LoginRateLimiter loginRateLimiter;
    @Mock
    private RefreshRateLimiter refreshRateLimiter;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private SecurityAuditLogger auditLogger;

    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final Duration refreshTokenTtl = Duration.ofMinutes(30);
    private SimpleMeterRegistry meterRegistry;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                tokenService,
                tokenHashService,
                clock,
                refreshTokenTtl,
                loginRateLimiter,
                refreshRateLimiter,
                emailVerificationService,
                5,
                meterRegistry,
                auditLogger
        );
    }

    @Test
    void registerCreatesUserAndTriggersVerification() {
        AuthService.RegisterUserCommand command = new AuthService.RegisterUserCommand(" Test@Example.com ", "Secret1!");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret1!")).thenReturn("encoded");

        User persisted = User.builder()
                .id(42L)
                .email("test@example.com")
                .passwordHash("encoded")
                .active(true)
                .emailVerified(false)
                .onboardingStatus(OnboardingStatus.NOT_STARTED)
                .role(Role.CLIENT)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(persisted);

        Instant expiresAt = Instant.parse("2025-01-02T00:00:00Z");
        when(emailVerificationService.sendVerificationEmail(42L, "test@example.com")).thenReturn(expiresAt);

        AuthService.RegistrationResult result = authService.register(command);

        assertEquals(42L, result.userId());
        assertEquals("test@example.com", result.email());
        assertEquals(expiresAt, result.verificationExpiresAt());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("encoded", savedUser.getPasswordHash());
        assertTrue(savedUser.isActive());
        assertFalse(savedUser.isEmailVerified());

        verify(emailVerificationService).sendVerificationEmail(42L, "test@example.com");
        verify(auditLogger).log(eq("USER_REGISTERED"), argThat(map -> "42".equals(map.get("userId"))));
    }

    @Test
    void authenticateRecordsFailureWhenCredentialsInvalid() {
        AuthService.LoginCommand command = new AuthService.LoginCommand("User@Example.com", "wrong-pass");
        User storedUser = User.builder()
                .id(7L)
                .email("user@example.com")
                .passwordHash("hash")
                .active(true)
                .emailVerified(true)
                .role(Role.CLIENT)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("wrong-pass", "hash")).thenReturn(false);

        assertThrows(UserException.InvalidCredentialsException.class, () -> authService.authenticate(command, new AuthService.DeviceMetadata(null, "My Laptop", "UA", "127.0.0.1")));

        verify(loginRateLimiter).checkAllowed("user@example.com");
        verify(loginRateLimiter).recordFailure("user@example.com");
        verify(auditLogger).log(eq("USER_LOGIN_FAILURE"), argThat(map -> "user@example.com".equals(map.get("email"))));
        verify(loginRateLimiter, never()).recordSuccess(anyString());
    }

    @Test
    void refreshTokensReissuesTokensAndRevokesOldOnes() {
        String incomingToken = "existing-refresh";
        AuthService.RefreshTokenCommand command = new AuthService.RefreshTokenCommand(incomingToken);

        when(tokenHashService.hash(anyString())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            return "existing-refresh".equals(value) ? "incoming-hash" : value + "-hash";
        });

        RefreshToken activeToken = RefreshToken.builder()
                .id(11L)
                .userId(21L)
                .tokenHash("incoming-hash")
                .expiresAt(clock.instant().plus(Duration.ofMinutes(5)))
                .createdAt(clock.instant().minus(Duration.ofMinutes(10)))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findActiveByTokenHash("incoming-hash")).thenReturn(Optional.of(activeToken));

        User tokenOwner = User.builder()
                .id(21L)
                .email("owner@example.com")
                .passwordHash("hash")
                .active(true)
                .emailVerified(true)
                .role(Role.CLIENT)
                .build();
        when(userRepository.findById(21L)).thenReturn(Optional.of(tokenOwner));

        when(tokenService.generateAccessToken(tokenOwner)).thenReturn("new-access");
        when(refreshTokenRepository.revokeByUserIdAndDeviceId(eq(21L), anyString())).thenReturn(1);
        when(refreshTokenRepository.findActiveByUserId(21L)).thenReturn(java.util.List.of());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken toSave = invocation.getArgument(0);
            return toSave.toBuilder().id(99L).build();
        });

        AuthService.AuthResult result = authService.refreshTokens(command, new AuthService.DeviceMetadata(null, null, "UA", "127.0.0.1"));

        assertEquals(tokenOwner, result.user());
        assertEquals("new-access", result.accessToken());
        assertNotNull(result.refreshToken());
        assertEquals(clock.instant().plus(refreshTokenTtl), result.refreshTokenExpiresAt());
        assertNotNull(result.deviceId());

        verify(refreshRateLimiter).checkAllowed(incomingToken);
        verify(refreshTokenRepository).revokeById(11L);
        verify(refreshTokenRepository).revokeByUserIdAndDeviceId(eq(21L), anyString());
        verify(refreshRateLimiter).recordSuccess(incomingToken);
        assertEquals(2.0, meterRegistry.get("security.refresh.revoked").counter().count());
    }

    @Test
    void listSessionsFiltersExpiredTokens() {
        RefreshToken active = RefreshToken.builder()
                .id(1L)
                .userId(5L)
                .tokenHash("hash")
                .expiresAt(clock.instant().plusSeconds(60))
                .revoked(false)
                .createdAt(clock.instant().minusSeconds(120))
                .deviceId("device-1")
                .deviceName("MacBook")
                .userAgent("UA")
                .ipAddress("127.0.0.1")
                .lastUsedAt(clock.instant().minusSeconds(30))
                .build();
        RefreshToken expired = active.toBuilder()
                .id(2L)
                .deviceId("device-2")
                .expiresAt(clock.instant().minusSeconds(5))
                .lastUsedAt(clock.instant().minusSeconds(10))
                .build();

        when(refreshTokenRepository.findActiveByUserId(5L)).thenReturn(java.util.List.of(active, expired));

        var sessions = authService.listSessions(5L);

        assertEquals(1, sessions.size());
        assertEquals("device-1", sessions.getFirst().deviceId());
    }
}
