package saas.personal_branding.api.application.security;

import saas.personal_branding.api.domain.model.EncryptedSecret;

public interface SecretCipher {

    EncryptedSecret encrypt(String plainText);

    String decrypt(EncryptedSecret secret);
}
