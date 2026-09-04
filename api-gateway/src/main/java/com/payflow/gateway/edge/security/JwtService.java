package com.payflow.gateway.edge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/** Mints and verifies HS256 JWTs carrying the merchant identity. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = properties.getTokenTtlSeconds();
    }

    public String issue(String merchantId, String clientId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(merchantId)
                .claim("clientId", clientId)
                .claim("scope", "payments")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    /** @return the verified claims, or throws if the token is invalid/expired. */
    public Claims verify(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}
