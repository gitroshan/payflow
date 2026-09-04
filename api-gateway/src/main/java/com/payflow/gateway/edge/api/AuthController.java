package com.payflow.gateway.edge.api;

import com.payflow.gateway.edge.security.JwtProperties;
import com.payflow.gateway.edge.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Minimal client-credentials token endpoint so the platform is demoable without
 * an external identity provider. A client exchanges its id + secret for a JWT.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final JwtProperties properties;

    public AuthController(JwtService jwtService, JwtProperties properties) {
        this.jwtService = jwtService;
        this.properties = properties;
    }

    public record TokenRequest(@NotBlank String clientId, @NotBlank String clientSecret) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }

    @PostMapping("/token")
    public Mono<ResponseEntity<TokenResponse>> token(@Valid @RequestBody TokenRequest request) {
        JwtProperties.ClientCredential client = properties.getClients().get(request.clientId());
        if (client == null || !client.getSecret().equals(request.clientSecret())) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        String jwt = jwtService.issue(client.getMerchantId(), request.clientId());
        return Mono.just(ResponseEntity.ok(
                new TokenResponse(jwt, "Bearer", jwtService.getTtlSeconds())));
    }
}
