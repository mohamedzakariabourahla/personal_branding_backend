package saas.personal_branding.api.application.exception;

public class UserException {

    public static class EmailAlreadyExistsException extends BusinessException {
        public EmailAlreadyExistsException(String email) {
            super("Email is already registered: " + email, "USER_EMAIL_EXISTS");
        }
    }

    public static class InvalidCredentialsException extends BusinessException {
        public InvalidCredentialsException() {
            super("Invalid email or password", "INVALID_CREDENTIALS");
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
}
