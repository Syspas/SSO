package com.example.sso.sso.service;

import com.example.sso.config.SsoProperties;
import com.example.sso.exception.SsoUnauthorizedException;
import com.example.sso.user.entity.SsoUser;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        SsoProperties properties = new SsoProperties();
        properties.getJwt().setSecret("change-me-sso-jwt-local-only-min-32-chars!!");
        properties.getJwt().setIssuer("http://127.0.0.1:8090/sso");
        properties.getJwt().setAccessTokenTtl(Duration.ofMinutes(5));
        jwtService = new JwtService(properties);
    }

    @Test
    @DisplayName("issueAccessToken кладёт email, name и roles")
    void issuesClaims() {
        SsoUser user = new SsoUser();
        user.setEmail("admin@local");
        user.setLastName("Локальный");
        user.setFirstName("Админ");
        user.setRoles("ROLE_ADMIN,ROLE_USER");

        String token = jwtService.issueAccessToken(user);
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo("admin@local");
        assertThat(claims.get("name", String.class)).isEqualTo("Локальный Админ");
        assertThat(claims.get("roles", List.class)).contains("ROLE_ADMIN", "ROLE_USER");
        assertThat(claims.getIssuer()).isEqualTo("http://127.0.0.1:8090/sso");
    }

    @Test
    @DisplayName("parseAndValidate отклоняет мусор")
    void rejectsGarbage() {
        assertThatThrownBy(() -> jwtService.parseAndValidate("not.a.jwt"))
                .isInstanceOf(SsoUnauthorizedException.class);
    }
}
