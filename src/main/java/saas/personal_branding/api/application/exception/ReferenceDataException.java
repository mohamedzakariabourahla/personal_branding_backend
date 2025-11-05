package saas.personal_branding.api.application.exception;

public class ReferenceDataException {

    public static class ReferenceDataNotFoundException extends BusinessException {
        public ReferenceDataNotFoundException(String type, Iterable<?> missingIds) {
            super("Reference data not found for type %s and ids %s".formatted(type, missingIds), "REFERENCE_DATA_NOT_FOUND");
        }
    }
}
