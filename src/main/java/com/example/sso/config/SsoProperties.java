package com.example.sso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Настройки SSO из {@code application.yml} (префикс {@code sso}).
 *
 * <p>Клиенты (webapp, excel) регистрируются в конфиге — без БД на v1.
 */
@ConfigurationProperties(prefix = "sso")
public class SsoProperties {

    private final Jwt jwt = new Jwt();
    private final Session session = new Session();
    private final Code code = new Code();
    private Map<String, Client> clients = new LinkedHashMap<>();

    public Jwt getJwt() {
        return jwt;
    }

    public Session getSession() {
        return session;
    }

    public Code getCode() {
        return code;
    }

    public Map<String, Client> getClients() {
        return clients;
    }

    public void setClients(Map<String, Client> clients) {
        this.clients = clients != null ? clients : new LinkedHashMap<>();
    }

    /** Параметры HMAC JWT. */
    public static class Jwt {
        private String secret;
        private Duration accessTokenTtl = Duration.ofMinutes(5);
        private String issuer = "http://127.0.0.1:8090/sso";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    /** Сессия на самом SSO (cookie). */
    public static class Session {
        private Duration timeout = Duration.ofHours(8);

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    /** TTL одноразового authorization code. */
    public static class Code {
        private Duration ttl = Duration.ofSeconds(120);

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    /** Зарегистрированный клиент (OAuth-подобный). */
    public static class Client {
        private String clientId;
        private String clientSecret;
        private List<String> redirectUris = new ArrayList<>();
        private List<String> postLogoutRedirectUris = new ArrayList<>();

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public List<String> getRedirectUris() {
            return redirectUris;
        }

        public void setRedirectUris(List<String> redirectUris) {
            this.redirectUris = redirectUris != null ? redirectUris : new ArrayList<>();
        }

        public List<String> getPostLogoutRedirectUris() {
            return postLogoutRedirectUris;
        }

        public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
            this.postLogoutRedirectUris = postLogoutRedirectUris != null
                    ? postLogoutRedirectUris
                    : new ArrayList<>();
        }
    }
}
