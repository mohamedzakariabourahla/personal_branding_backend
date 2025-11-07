package saas.personal_branding.api.infrastructure.security.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.security.SecretCipher;
import saas.personal_branding.api.domain.model.EncryptedSecret;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmSecretCipher implements SecretCipher {

    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int DEFAULT_IV_LENGTH_BYTES = 12;

    private final EncryptionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKeySpec secretKeySpec;

    public AesGcmSecretCipher(EncryptionProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        this.secretKeySpec = new SecretKeySpec(properties.keyBytes(), "AES");
    }

    @Override
    public EncryptedSecret encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[DEFAULT_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return EncryptedSecret.builder()
                    .cipherText(Base64.getEncoder().encodeToString(cipherBytes))
                    .initializationVector(iv)
                    .build();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to encrypt secret", ex);
        }
    }

    @Override
    public String decrypt(EncryptedSecret secret) {
        if (secret == null || secret.getCipherText() == null || secret.getInitializationVector() == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, secret.getInitializationVector()));
            byte[] plainBytes = cipher.doFinal(Base64.getDecoder().decode(secret.getCipherText()));
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to decrypt secret", ex);
        }
    }
}
