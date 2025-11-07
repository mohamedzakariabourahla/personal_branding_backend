package saas.personal_branding.api.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString(exclude = "cipherText")
public class EncryptedSecret {
    private final String cipherText;
    private final byte[] initializationVector;
}
