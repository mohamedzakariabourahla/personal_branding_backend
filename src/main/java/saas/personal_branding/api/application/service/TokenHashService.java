package saas.personal_branding.api.application.service;

public interface TokenHashService {
    String hash(String rawToken);
}
