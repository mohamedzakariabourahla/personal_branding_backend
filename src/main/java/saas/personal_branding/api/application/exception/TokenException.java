package saas.personal_branding.api.application.exception;

public class TokenException {

    public static class RefreshTokenNotFoundException extends BusinessException {
        public RefreshTokenNotFoundException() {
            super("Refresh token is invalid", "REFRESH_TOKEN_NOT_FOUND");
        }
    }

    public static class RefreshTokenExpiredException extends BusinessException {
        public RefreshTokenExpiredException() {
            super("Refresh token has expired", "REFRESH_TOKEN_EXPIRED");
        }
    }

    public static class RefreshTokenRateLimitedException extends BusinessException {
        private final long retryAfterSeconds;

        public RefreshTokenRateLimitedException(long retryAfterSeconds) {
            super("Too many refresh attempts. Please try again later.", "REFRESH_RATE_LIMITED");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    public static class PasswordResetTokenNotFoundException extends BusinessException {
        public PasswordResetTokenNotFoundException() {
            super("Password reset token is invalid or has already been used", "PASSWORD_RESET_TOKEN_NOT_FOUND");
        }
    }

    public static class PasswordResetTokenExpiredException extends BusinessException {
        public PasswordResetTokenExpiredException() {
            super("Password reset token has expired", "PASSWORD_RESET_TOKEN_EXPIRED");
        }
    }

    public static class EmailVerificationTokenNotFoundException extends BusinessException {
        public EmailVerificationTokenNotFoundException() {
            super("Email verification token is invalid or has already been used", "EMAIL_VERIFICATION_TOKEN_NOT_FOUND");
        }
    }

    public static class EmailVerificationTokenExpiredException extends BusinessException {
        public EmailVerificationTokenExpiredException() {
            super("Email verification token has expired", "EMAIL_VERIFICATION_TOKEN_EXPIRED");
        }
    }
}
