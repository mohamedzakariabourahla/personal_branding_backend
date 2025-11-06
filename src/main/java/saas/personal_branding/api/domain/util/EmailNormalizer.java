package saas.personal_branding.api.domain.util;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
