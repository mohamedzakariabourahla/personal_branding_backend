package saas.personal_branding.api.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import saas.personal_branding.api.application.service.AssetStorageService;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetStorageService assetStorageService;

    public AssetController(AssetStorageService assetStorageService) {
        this.assetStorageService = assetStorageService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file,
                                                      HttpServletRequest request) {
        URI uri = ServletUriComponentsBuilder.fromRequestUri(request).build().toUri();
        AssetStorageService.StoredAsset asset = assetStorageService.store(file, uri);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "url", asset.url(),
                        "filename", asset.filename(),
                        "size", asset.size(),
                        "contentType", asset.contentType(),
                        "uploadedAt", asset.uploadedAt().toString()
                ));
    }
}
