package saas.personal_branding.api.application.exception;

public class UserException {

    public static class EmailAlreadyExistsException extends BusinessException {
        public EmailAlreadyExistsException(String email) {
            super("An account already exists with this email address. Try signing in or resetting your password.", "USER_EMAIL_EXISTS");
        }
    }

    public static class InvalidCredentialsException extends BusinessException {
        public InvalidCredentialsException() {
            super("Invalid email or password", "INVALID_CREDENTIALS");
        }
    }

    public static class EmailNotRegisteredException extends BusinessException {
        public EmailNotRegisteredException(String email) {
            super("No account found for this email. Please sign up first.", "USER_NOT_REGISTERED");
        }
    }

    public static class UserNotFoundException extends BusinessException {
        public UserNotFoundException(Long userId) {
            super("User not found with id: " + userId, "USER_NOT_FOUND");
        }
    }

    public static class OnboardingAlreadyCompletedException extends BusinessException {
        public OnboardingAlreadyCompletedException(Long userId) {
            super("Onboarding already completed for user id: " + userId, "ONBOARDING_ALREADY_COMPLETED");
        }
    }

    public static class InactiveAccountException extends BusinessException {
        public InactiveAccountException(Long userId) {
            super("Account is inactive for user id: " + userId, "USER_INACTIVE");
        }
    }

    public static class EmailNotVerifiedException extends BusinessException {
        public EmailNotVerifiedException(Long userId) {
            super("Email address is not verified for user id: " + userId, "USER_EMAIL_NOT_VERIFIED");
        }
    }

    public static class EmailAlreadyVerifiedException extends BusinessException {
        public EmailAlreadyVerifiedException(Long userId) {
            super("Email address is already verified for user id: " + userId, "USER_EMAIL_ALREADY_VERIFIED");
        }
    }

    public static class TooManyLoginAttemptsException extends BusinessException {
        private final long retryAfterSeconds;

        public TooManyLoginAttemptsException(long retryAfterSeconds) {
            super("Too many login attempts. Please try again later.", "LOGIN_RATE_LIMITED");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    public static class EmailVerificationResendRateLimitedException extends BusinessException {
        private final long retryAfterSeconds;

        public EmailVerificationResendRateLimitedException(long retryAfterSeconds) {
            super("You requested a verification email too recently. Please wait before trying again.", "EMAIL_VERIFICATION_RATE_LIMITED");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    public static class EmailVerificationServiceUnavailableException extends BusinessException {
        public EmailVerificationServiceUnavailableException() {
            super("Email verification is temporarily unavailable. Please try again in a moment.", "EMAIL_VERIFICATION_UNAVAILABLE");
        }
    }

    public static class EmailDispatchFailedException extends BusinessException {
        public EmailDispatchFailedException(String detail) {
            super(detail, "EMAIL_DISPATCH_FAILED");
        }
    }
}
