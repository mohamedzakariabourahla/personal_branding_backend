package saas.personal_branding.api.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;
import saas.personal_branding.api.application.service.PlatformCredentialService;
import saas.personal_branding.api.application.service.platform.MetaOAuthApplicationService;
import saas.personal_branding.api.application.service.platform.TikTokOAuthApplicationService;
import saas.personal_branding.api.presentation.dto.request.PlatformOAuthCallbackRequest;
import saas.personal_branding.api.presentation.dto.response.PlatformAuthorizationResponse;
import saas.personal_branding.api.presentation.dto.response.PlatformConnectionResponse;
import saas.personal_branding.api.presentation.mapper.PlatformConnectionDtoMapper;

import java.util.List;

@RestController
@RequestMapping("/api/platforms")
@Validated
public class PlatformConnectionController {

    private final PlatformCredentialService platformCredentialService;
    private final MetaOAuthApplicationService metaOAuthApplicationService;
    private final TikTokOAuthApplicationService tikTokOAuthApplicationService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public PlatformConnectionController(PlatformCredentialService platformCredentialService,
                                        MetaOAuthApplicationService metaOAuthApplicationService,
                                        TikTokOAuthApplicationService tikTokOAuthApplicationService,
                                        AuthenticatedUserProvider authenticatedUserProvider) {
        this.platformCredentialService = platformCredentialService;
        this.metaOAuthApplicationService = metaOAuthApplicationService;
        this.tikTokOAuthApplicationService = tikTokOAuthApplicationService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/connections")
    public ResponseEntity<List<PlatformConnectionResponse>> listConnections() {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        List<PlatformConnectionResponse> responses = platformCredentialService.findConnections(userId).stream()
                .map(PlatformConnectionDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long connectionId) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        platformCredentialService.findConnectionById(connectionId)
                .filter(connection -> connection.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found"));
        platformCredentialService.deleteConnection(connectionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/meta/oauth/start")
    public ResponseEntity<PlatformAuthorizationResponse> startMetaOAuth() {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        var context = metaOAuthApplicationService.startAuthorization(userId);
        return ResponseEntity.ok(new PlatformAuthorizationResponse(
                context.authorizationUrl(),
                context.state(),
                context.expiresAt()
        ));
    }

    @PostMapping("/meta/oauth/complete")
    public ResponseEntity<PlatformConnectionResponse> completeMetaOAuth(@Valid @RequestBody PlatformOAuthCallbackRequest request) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        var connection = metaOAuthApplicationService.completeAuthorization(userId, request.state(), request.code());
        return ResponseEntity.ok(PlatformConnectionDtoMapper.toResponse(connection));
    }

    @PostMapping("/tiktok/oauth/start")
    public ResponseEntity<PlatformAuthorizationResponse> startTikTokOAuth() {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        var context = tikTokOAuthApplicationService.startAuthorization(userId);
        return ResponseEntity.ok(new PlatformAuthorizationResponse(
                context.authorizationUrl(),
                context.state(),
                context.expiresAt()
        ));
    }

    @PostMapping("/tiktok/oauth/complete")
    public ResponseEntity<PlatformConnectionResponse> completeTikTokOAuth(@Valid @RequestBody PlatformOAuthCallbackRequest request) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        var connection = tikTokOAuthApplicationService.completeAuthorization(userId, request.state(), request.code());
        return ResponseEntity.ok(PlatformConnectionDtoMapper.toResponse(connection));
    }
}
