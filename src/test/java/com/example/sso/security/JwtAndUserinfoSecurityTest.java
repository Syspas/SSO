package com.example.sso.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static com.example.sso.support.Steps.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Подделка JWT и атаки на GET /userinfo; утечки секретов в ответах.
 */
@Epic("Безопасность")
@Feature("JWT и userinfo")
@Tag("security")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SSO: JWT / userinfo атаки")
class JwtAndUserinfoSecurityTest {

    private static final String WEBAPP_REDIRECT =
            "http://127.0.0.1:8080/login/sso/callback";
    private static final String WEBAPP_SECRET = "webapp-secret-local";
    private static final String LOCAL_JWT_SECRET =
            "change-me-sso-jwt-local-only-min-32-chars!!";
    private static final String LOCAL_ISSUER = "http://127.0.0.1:8090/sso";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("alg=none, чужой HMAC, другой iss, просроченный токен → 401")
    void forgedAndExpiredTokensRejected() throws Exception {
        step("alg=none", () ->
                mockMvc.perform(get("/userinfo")
                                .header("Authorization", "Bearer " + unsignedAlgNoneToken()))
                        .andExpect(status().isUnauthorized()));

        step("HMAC чужим ключом", () ->
                mockMvc.perform(get("/userinfo")
                                .header("Authorization", "Bearer " + tokenWithWrongKey()))
                        .andExpect(status().isUnauthorized()));

        step("Другой issuer", () ->
                mockMvc.perform(get("/userinfo")
                                .header("Authorization", "Bearer " + tokenWithWrongIssuer()))
                        .andExpect(status().isUnauthorized()));

        step("Просроченный токен", () ->
                mockMvc.perform(get("/userinfo")
                                .header("Authorization", "Bearer " + expiredToken()))
                        .andExpect(status().isUnauthorized()));
    }

    @Test
    @DisplayName("Заголовки без Bearer / Basic / пустой Bearer → 401")
    void badAuthorizationHeadersRejected() throws Exception {
        step("Без префикса Bearer", () ->
                mockMvc.perform(get("/userinfo")
                                .header("Authorization", "raw-token-value"))
                        .andExpect(status().isUnauthorized()));
        step("Basic вместо Bearer", () ->
                mockMvc.perform(get("/userinfo")
                                .header("Authorization", "Basic YWRtaW46cGFzcw=="))
                        .andExpect(status().isUnauthorized()));
        step("Пустой Bearer", () ->
                mockMvc.perform(get("/userinfo")
                                .header("Authorization", "Bearer "))
                        .andExpect(status().isUnauthorized()));
    }

    @Test
    @DisplayName("Ответ /token не содержит client_secret; /userinfo — без пароля")
    void responsesDoNotLeakSecrets() throws Exception {
        String code = issueCode();
        MvcResult tokenResult = step("Обменять code на token", () ->
                mockMvc.perform(post("/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("grant_type", "authorization_code")
                                .param("code", code)
                                .param("client_id", "webapp")
                                .param("client_secret", WEBAPP_SECRET)
                                .param("redirect_uri", WEBAPP_REDIRECT))
                        .andExpect(status().isOk())
                        .andReturn());
        String tokenBody = tokenResult.getResponse().getContentAsString();
        step("В ответе /token нет client_secret и пароля", () -> {
            assertThat(tokenBody).doesNotContain("client_secret");
            assertThat(tokenBody).doesNotContain(WEBAPP_SECRET);
            assertThat(tokenBody).doesNotContain("PortalSeed9");
            assertThat(tokenBody).doesNotContain("password");
        });

        JsonNode tokenJson = objectMapper.readTree(tokenBody);
        String accessToken = tokenJson.get("access_token").asText();
        MvcResult userinfo = step("Запросить /userinfo", () ->
                mockMvc.perform(get("/userinfo")
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andReturn());
        String infoBody = userinfo.getResponse().getContentAsString();
        step("В ответе /userinfo нет пароля и хеша", () -> {
            assertThat(infoBody).doesNotContain("password");
            assertThat(infoBody).doesNotContain("PortalSeed9");
            assertThat(infoBody).doesNotContain("$2a$");
            assertThat(infoBody).contains("admin@local");
        });
    }

    private String issueCode() throws Exception {
        MvcResult authorize = mockMvc.perform(get("/authorize")
                        .param("client_id", "webapp")
                        .param("redirect_uri", WEBAPP_REDIRECT)
                        .with(user("admin@local").roles("ADMIN", "USER")))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = authorize.getResponse().getHeader("Location");
        assertThat(location).contains("code=");
        return location.replaceAll(".*[?&]code=([^&]+).*", "$1");
    }

    private static String unsignedAlgNoneToken() {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"admin@local\",\"iss\":\"" + LOCAL_ISSUER + "\"}")
                        .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".";
    }

    private static String tokenWithWrongKey() {
        SecretKey key = Keys.hmacShaKeyFor(
                "totally-different-hmac-secret-key-32b!".getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(LOCAL_ISSUER)
                .subject("admin@local")
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }

    private static String tokenWithWrongIssuer() {
        SecretKey key = Keys.hmacShaKeyFor(LOCAL_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("https://evil.example/sso")
                .subject("admin@local")
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }

    private static String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(LOCAL_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        return Jwts.builder()
                .issuer(LOCAL_ISSUER)
                .subject("admin@local")
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();
    }
}
