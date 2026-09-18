package com.example.sso.sso.service;

import com.example.sso.client.ClientRegistry;
import com.example.sso.config.SsoProperties;
import com.example.sso.exception.SsoUnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Выдача authorization code и URL редиректа на клиента.
 */
@Service
public class AuthorizationService {

    private final ClientRegistry clients;
    private final AuthorizationCodeService codes;

    public AuthorizationService(ClientRegistry clients, AuthorizationCodeService codes) {
        this.clients = clients;
        this.codes = codes;
    }

    public String buildRedirect(String clientId, String redirectUri, String state, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SsoUnauthorizedException("Login required");
        }
        SsoProperties.Client client = clients.requireClient(clientId);
        clients.validateRedirectUri(client, redirectUri);

        String email = authentication.getName();
        String code = codes.issue(email, clientId, redirectUri);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code);
        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }
        // encode: state с кавычками/HTML не должен валить authorize 500
        return builder.build().encode().toUriString();
    }
}
