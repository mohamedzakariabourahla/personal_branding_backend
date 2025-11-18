package saas.personal_branding.api.infrastructure.security.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionProperties {

    @NotBlank
    private String masterKey;

    @NotBlank
    private String algorithm = "AES/GCM/NoPadding";

    @NotBlank
    private String keyId = "default";

    private byte[] keyBytes;

    @PostConstruct
    void init() {
        this.keyBytes = decodeKey(masterKey);
        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new IllegalStateException("Encryption master key must be 16/24/32 bytes");
        }
    }

    public byte[] keyBytes() {
        return keyBytes.clone();
    }

    private static byte[] decodeKey(String value) {
        if (value == null) {
            throw new IllegalArgumentException("masterKey cannot be null");
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ignore) {
            // fall through
        }
        boolean looksHex = value.matches("^[0-9a-fA-F]+$") && (value.length() % 2 == 0);
        if (looksHex) {
            return HexFormat.of().parseHex(value);
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
