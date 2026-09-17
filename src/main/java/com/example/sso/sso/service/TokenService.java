package com.example.sso.sso.service;

import com.example.sso.client.ClientRegistry;
import com.example.sso.config.SsoProperties;
import com.example.sso.exception.SsoBadRequestException;
import com.example.sso.sso.dto.TokenRequest;
import com.example.sso.sso.dto.TokenResponse;
import com.example.sso.user.entity.SsoUser;
import com.example.sso.user.repository.SsoUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Обмен code → JWT. Бизнес-логика без HTTP.
 */
@Service
public class TokenService {

    private static final String GRANT_AUTHORIZATION_CODE = "authorization_code";

    private final ClientRegistry clients;
    private final AuthorizationCodeService codes;
    private final SsoUserRepository users;
    private final JwtService jwtService;
    private final SsoProperties properties;

    public TokenService(
            ClientRegistry clients,
            AuthorizationCodeService codes,
            SsoUserRepository users,
            JwtService jwtService,
            SsoProperties properties) {
        this.clients = clients;
        this.codes = codes;
        this.users = users;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public TokenResponse exchange(TokenRequest request) {
        if (!GRANT_AUTHORIZATION_CODE.equals(request.getGrantType())) {
            throw new SsoBadRequestException("Unsupported grant_type");
        }
        SsoProperties.Client client = clients.requireClient(request.getClientId());
        clients.validateSecret(client, request.getClientSecret());
        clients.validateRedirectUri(client, request.getRedirectUri());

        AuthorizationCodeService.CodeRecord record = codes.consume(
                request.getCode(), request.getClientId(), request.getRedirectUri());

        SsoUser user = users.findByEmailIgnoreCase(record.email())
                .orElseThrow(() -> new SsoBadRequestException("User no longer exists"));

        String token = jwtService.issueAccessToken(user);
        long expiresIn = properties.getJwt().getAccessTokenTtl().toSeconds();
        return new TokenResponse(token, "Bearer", expiresIn);
    }
}
