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
}
