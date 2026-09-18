package com.example.sso.client;

import com.example.sso.config.SsoProperties;
import com.example.sso.exception.SsoBadRequestException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

/**
 * Реестр OAuth-клиентов из конфига (webapp, excel).
 */
@Component
public class ClientRegistry {

    private final SsoProperties properties;

    public ClientRegistry(SsoProperties properties) {
        this.properties = properties;
    }

    public SsoProperties.Client requireClient(String clientId) {
        return findByClientId(clientId)
                .orElseThrow(() -> new SsoBadRequestException("Unknown client_id: " + clientId));
    }

    public Optional<SsoProperties.Client> findByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return Optional.empty();
        }
        return properties.getClients().values().stream()
                .filter(c -> clientId.equals(c.getClientId()))
                .findFirst();
    }

    public void validateRedirectUri(SsoProperties.Client client, String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new SsoBadRequestException("redirect_uri is required");
        }
        boolean allowed = client.getRedirectUris().stream()
                .anyMatch(redirectUri::equals);
        if (!allowed) {
            throw new SsoBadRequestException("redirect_uri is not registered for client");
        }
    }

    /**
     * RP-initiated logout: {@code post_logout_redirect_uri} только из конфига клиента.
     */
    public boolean isAllowedPostLogoutRedirect(String clientId, String postLogoutRedirectUri) {
        if (postLogoutRedirectUri == null || postLogoutRedirectUri.isBlank()) {
            return false;
        }
        return findByClientId(clientId)
                .map(SsoProperties.Client::getPostLogoutRedirectUris)
                .orElse(List.of())
                .stream()
                .anyMatch(postLogoutRedirectUri::equals);
    }

    public void validateSecret(SsoProperties.Client client, String clientSecret) {
        if (!secretsMatch(clientSecret, client.getClientSecret())) {
            throw new SsoBadRequestException("Invalid client_secret");
        }
    }

    private static boolean secretsMatch(String provided, String expected) {
        if (provided == null || expected == null) {
            return false;
        }
        byte[] a = provided.getBytes(StandardCharsets.UTF_8);
        byte[] b = expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
