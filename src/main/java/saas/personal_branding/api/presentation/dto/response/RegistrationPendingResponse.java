package saas.personal_branding.api.presentation.dto.response;

import java.time.Instant;

public record RegistrationPendingResponse(String email,
                                          Instant verificationExpiresAt,
                                          boolean verificationRequired,
                                          String message) {
}
