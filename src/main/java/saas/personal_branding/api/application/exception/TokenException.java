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
}
