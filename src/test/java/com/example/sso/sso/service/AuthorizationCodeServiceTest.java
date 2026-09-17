package com.example.sso.sso.service;

import com.example.sso.config.SsoProperties;
import com.example.sso.exception.SsoBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthorizationCodeService")
class AuthorizationCodeServiceTest {

    private AuthorizationCodeService codes;

    @BeforeEach
    void setUp() {
        SsoProperties properties = new SsoProperties();
        properties.getCode().setTtl(Duration.ofMinutes(2));
        codes = new AuthorizationCodeService(properties);
    }

    @Test
    @DisplayName("issue + consume возвращает email и сжигает code")
    void issueAndConsumeOnce() {
        String code = codes.issue("admin@local", "webapp", "http://cb");
        AuthorizationCodeService.CodeRecord record =
                codes.consume(code, "webapp", "http://cb");
        assertThat(record.email()).isEqualTo("admin@local");

        assertThatThrownBy(() -> codes.consume(code, "webapp", "http://cb"))
                .isInstanceOf(SsoBadRequestException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    @DisplayName("consume отклоняет чужой client_id")
    void rejectWrongClient() {
        String code = codes.issue("admin@local", "webapp", "http://cb");
        assertThatThrownBy(() -> codes.consume(code, "excel", "http://cb"))
                .isInstanceOf(SsoBadRequestException.class)
                .hasMessageContaining("another client");
    }

    @Test
    @DisplayName("consume отклоняет mismatch redirect_uri")
    void rejectRedirectMismatch() {
        String code = codes.issue("admin@local", "webapp", "http://cb");
        assertThatThrownBy(() -> codes.consume(code, "webapp", "http://other"))
                .isInstanceOf(SsoBadRequestException.class)
                .hasMessageContaining("redirect_uri mismatch");
    }
}
