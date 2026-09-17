package com.example.sso.sso.service;

import com.example.sso.config.SsoProperties;
import com.example.sso.exception.SsoUnauthorizedException;
import com.example.sso.user.entity.SsoUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Выпуск и проверка HMAC JWT для клиентов (webapp, excel).
 *
 * <p>TTL короткий (по умолчанию 5 минут) — handoff после code→token.
 * Долгую сессию держит cookie на SSO, не этот токен.
 */
@Service
public class JwtService {

    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLES = "roles";

    private final SsoProperties properties;
    private final SecretKey key;

    public JwtService(SsoProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "sso.jwt.secret must be at least 32 bytes (HMAC-SHA)");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String issueAccessToken(SsoUser user) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getJwt().getAccessTokenTtl());
        List<String> roles = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(user.getEmail())
                .claim(CLAIM_NAME, user.getDisplayName())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new SsoUnauthorizedException("Invalid or expired access token");
        }
    }
}
