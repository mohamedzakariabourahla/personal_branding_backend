package saas.personal_branding.api.application.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssetStorageService {

    private final Path rootDir;
    private final String baseUrl;

    public AssetStorageService(@Value("${app.assets.dir:./tmp-home/uploads}") String rootDir,
                               @Value("${app.assets.base-url:}") String baseUrl) {
        this.rootDir = Path.of(rootDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
    }

    public StoredAsset store(MultipartFile file, URI requestUri) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        try {
            Files.createDirectories(rootDir);
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (ext != null ? "." + ext : "");
            Path target = rootDir.resolve(filename);
            file.transferTo(target);
            String url = resolveUrl(filename, requestUri);
            return new StoredAsset(filename, url, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    private String resolveUrl(String filename, URI requestUri) {
        if (!baseUrl.isBlank()) {
            return baseUrl.endsWith("/") ? baseUrl + filename : baseUrl + "/" + filename;
        }
        if (requestUri != null && requestUri.getScheme() != null && requestUri.getAuthority() != null) {
            String origin = requestUri.getScheme() + "://" + requestUri.getAuthority();
            return origin + "/uploads/" + filename;
        }
        // Fallback to relative path if request URI is unavailable
        return "/uploads/" + filename;
    }

    public record StoredAsset(String filename, String url, long size, String contentType, Instant uploadedAt) {
        public StoredAsset(String filename, String url, long size, String contentType) {
            this(filename, url, size, contentType, Instant.now());
        }
    }
}
