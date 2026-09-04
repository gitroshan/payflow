package com.payflow.gateway.edge.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Gateway auth configuration. In production the secret comes from a secrets
 * manager and clients from an identity provider; here they are externalised
 * config so the demo is self-contained.
 */
@ConfigurationProperties(prefix = "payflow.auth")
public class JwtProperties {

    /** HS256 signing secret. MUST be at least 32 bytes. */
    private String secret = "change-me-please-change-me-please-32bytes!";

    /** Access-token lifetime in seconds. */
    private long tokenTtlSeconds = 3600;

    /** clientId -> clientSecret for the demo client-credentials flow. */
    private Map<String, ClientCredential> clients = Map.of();

    public static class ClientCredential {
        private String secret;
        private String merchantId;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getTokenTtlSeconds() { return tokenTtlSeconds; }
    public void setTokenTtlSeconds(long tokenTtlSeconds) { this.tokenTtlSeconds = tokenTtlSeconds; }
    public Map<String, ClientCredential> getClients() { return clients; }
    public void setClients(Map<String, ClientCredential> clients) { this.clients = clients; }
}
