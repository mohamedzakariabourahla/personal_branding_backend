package saas.personal_branding.api.auth.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;
import saas.personal_branding.api.application.service.EmailVerificationRateLimiter;
import saas.personal_branding.api.application.service.PasswordResetService;
import saas.personal_branding.api.auth.application.ListSessionsUseCase;
import saas.personal_branding.api.auth.application.LoginUseCase;
import saas.personal_branding.api.auth.application.LogoutUseCase;
import saas.personal_branding.api.auth.application.RefreshTokensUseCase;
import saas.personal_branding.api.auth.application.RegisterUserUseCase;
import saas.personal_branding.api.auth.application.RequestPasswordResetUseCase;
import saas.personal_branding.api.auth.application.ResendVerificationForEmailUseCase;
import saas.personal_branding.api.auth.application.ResendVerificationForUserUseCase;
import saas.personal_branding.api.auth.application.ResetPasswordUseCase;
import saas.personal_branding.api.auth.application.RevokeSessionUseCase;
import saas.personal_branding.api.auth.application.VerifyEmailUseCase;
import saas.personal_branding.api.domain.model.OnboardingStatus;
import saas.personal_branding.api.domain.model.Role;
import saas.personal_branding.api.domain.model.User;
import io.micrometer.core.instrument.MeterRegistry;
import saas.personal_branding.api.infrastructure.security.ResponseStatusMetricsFilter;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;
    @MockBean
    private LoginUseCase loginUseCase;
    @MockBean
    private RefreshTokensUseCase refreshTokensUseCase;
    @MockBean
    private LogoutUseCase logoutUseCase;
    @MockBean
    private ListSessionsUseCase listSessionsUseCase;
    @MockBean
    private RevokeSessionUseCase revokeSessionUseCase;
    @MockBean
    private RequestPasswordResetUseCase requestPasswordResetUseCase;
    @MockBean
    private ResetPasswordUseCase resetPasswordUseCase;
    @MockBean
    private ResendVerificationForUserUseCase resendVerificationForUserUseCase;
    @MockBean
    private ResendVerificationForEmailUseCase resendVerificationForEmailUseCase;
    @MockBean
    private VerifyEmailUseCase verifyEmailUseCase;
    @MockBean
    private AuthenticatedUserProvider authenticatedUserProvider;
    @MockBean
    private EmailVerificationRateLimiter emailVerificationRateLimiter;
    @MockBean
    private java.time.Clock clock;
    @MockBean
    private MeterRegistry meterRegistry;
    @MockBean
    private ResponseStatusMetricsFilter responseStatusMetricsFilter;

    private AuthService.AuthResult authResult;
    private Instant fixedNow;

    @BeforeEach
    void setUp() {
        fixedNow = Instant.parse("2025-01-01T00:00:00Z");
        org.mockito.Mockito.when(clock.instant()).thenReturn(fixedNow);
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("hash")
                .active(true)
                .emailVerified(true)
                .onboardingStatus(OnboardingStatus.NOT_STARTED)
                .role(Role.CLIENT)
                .build();
        authResult = new AuthService.AuthResult(
                user,
                "access-token",
                "refresh-token",
                fixedNow.plusSeconds(3600),
                fixedNow,
                "device-123",
                "My Device");
    }

    @Test
    void registerReturnsCreated() throws Exception {
        when(registerUserUseCase.execute(any())).thenReturn(
                new AuthService.RegistrationResult(1L, "user@example.com", Instant.now().plusSeconds(600))
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"P@ssword123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void loginReturnsTokensAndSetsCookie() throws Exception {
        when(loginUseCase.execute(any(), any())).thenReturn(authResult);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"P@ssword123","deviceName":"My Device"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void passwordResetRequestReturnsAccepted() throws Exception {
        when(requestPasswordResetUseCase.execute(any())).thenReturn(
                new PasswordResetService.RequestResetResult(true, Instant.now().plusSeconds(900))
        );

        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isAccepted());
    }
}
