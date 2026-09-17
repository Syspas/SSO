package com.example.sso.sso.service;

import com.example.sso.config.SsoProperties;
import com.example.sso.exception.SsoBadRequestException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Одноразовые authorization code в памяти (v1).
 *
 * <p>После рестарта SSO все code пропадают. В проде — Redis или БД с TTL.
 */
@Service
public class AuthorizationCodeService {

    private final SsoProperties properties;
    private final Map<String, CodeRecord> codes = new ConcurrentHashMap<>();

    public AuthorizationCodeService(SsoProperties properties) {
        this.properties = properties;
    }

    public String issue(String email, String clientId, String redirectUri) {
        String code = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(properties.getCode().getTtl());
        codes.put(code, new CodeRecord(email, clientId, redirectUri, expiresAt));
        return code;
    }

    public CodeRecord consume(String code, String clientId, String redirectUri) {
        CodeRecord record = codes.remove(code);
        if (record == null || record.expiresAt().isBefore(Instant.now())) {
            throw new SsoBadRequestException("Invalid or expired code");
        }
        if (!record.clientId().equals(clientId)) {
            throw new SsoBadRequestException("code was issued for another client");
        }
        if (!record.redirectUri().equals(redirectUri)) {
            throw new SsoBadRequestException("redirect_uri mismatch");
        }
        return record;
    }

    /** Выданный code: кому, какому клиенту, куда вернуть. */
    public record CodeRecord(String email, String clientId, String redirectUri, Instant expiresAt) {
    }
}
