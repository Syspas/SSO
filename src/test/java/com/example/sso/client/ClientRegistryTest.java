package com.example.sso.client;

import com.example.sso.config.SsoProperties;
import com.example.sso.exception.SsoBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ClientRegistry")
class ClientRegistryTest {

    private ClientRegistry registry;

    @BeforeEach
    void setUp() {
        SsoProperties.Client webapp = new SsoProperties.Client();
        webapp.setClientId("webapp");
        webapp.setClientSecret("webapp-secret");
        webapp.setRedirectUris(List.of("http://127.0.0.1:8080/login/sso/callback"));

        Map<String, SsoProperties.Client> clients = new LinkedHashMap<>();
        clients.put("webapp", webapp);

        SsoProperties properties = new SsoProperties();
        properties.setClients(clients);
        registry = new ClientRegistry(properties);
    }

    @Test
    @DisplayName("requireClient находит зарегистрированного клиента")
    void requireClientFindsKnown() {
        assertThat(registry.requireClient("webapp").getClientId()).isEqualTo("webapp");
    }

    @Test
    @DisplayName("requireClient отклоняет неизвестный client_id")
    void requireClientRejectsUnknown() {
        assertThatThrownBy(() -> registry.requireClient("unknown"))
                .isInstanceOf(SsoBadRequestException.class)
                .hasMessageContaining("Unknown client_id");
    }

    @Test
    @DisplayName("validateRedirectUri принимает только точное совпадение")
    void validateRedirectUriExactMatchOnly() {
        SsoProperties.Client client = registry.requireClient("webapp");
        registry.validateRedirectUri(client, "http://127.0.0.1:8080/login/sso/callback");

        assertThatThrownBy(() ->
                registry.validateRedirectUri(client, "http://127.0.0.1:8080/login/sso/callback/extra"))
                .isInstanceOf(SsoBadRequestException.class);
        assertThatThrownBy(() ->
                registry.validateRedirectUri(client, "http://evil.example/callback"))
                .isInstanceOf(SsoBadRequestException.class);
    }

    @Test
    @DisplayName("validateSecret сравнивает секреты безопасно")
    void validateSecret() {
        SsoProperties.Client client = registry.requireClient("webapp");
        registry.validateSecret(client, "webapp-secret");
        assertThatThrownBy(() -> registry.validateSecret(client, "other"))
                .isInstanceOf(SsoBadRequestException.class)
                .hasMessageContaining("client_secret");
    }
}
