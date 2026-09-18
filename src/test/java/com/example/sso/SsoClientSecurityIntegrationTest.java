package com.example.sso;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Негативные и граничные сценарии OAuth-потока SSO.
 */
@Epic("Безопасность")
@Feature("OAuth authorize/token/userinfo")
@Tag("security")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SSO: безопасность authorize/token/userinfo")
class SsoClientSecurityIntegrationTest {

    private static final String WEBAPP_REDIRECT =
            "http://127.0.0.1:8080/login/sso/callback";
    private static final String EXCEL_REDIRECT =
            "http://127.0.0.1:8081/excel/login/sso/callback";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("authorize без сессии уводит на /login")
    void authorizeWithoutSessionRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/authorize")
                        .param("client_id", "webapp")
                        .param("redirect_uri", WEBAPP_REDIRECT))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @DisplayName("authorize отклоняет незарегистрированный redirect_uri")
    void authorizeRejectsUnregisteredRedirectUri() throws Exception {
        mockMvc.perform(get("/authorize")
                        .param("client_id", "webapp")
                        .param("redirect_uri", "http://evil.example/callback")
                        .with(user("admin@local").roles("ADMIN", "USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("token отклоняет неверный client_secret")
    void tokenRejectsBadSecret() throws Exception {
        String code = issueCode("webapp", WEBAPP_REDIRECT);

        mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type": "authorization_code",
                                  "code": "%s",
                                  "client_id": "webapp",
                                  "client_secret": "wrong-secret",
                                  "redirect_uri": "%s"
                                }
                                """.formatted(code, WEBAPP_REDIRECT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("token отклоняет повторное использование code")
    void tokenRejectsCodeReuse() throws Exception {
        String code = issueCode("webapp", WEBAPP_REDIRECT);

        mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("client_id", "webapp")
                        .param("client_secret", "webapp-secret-local")
                        .param("redirect_uri", WEBAPP_REDIRECT))
                .andExpect(status().isOk());

        mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("client_id", "webapp")
                        .param("client_secret", "webapp-secret-local")
                        .param("redirect_uri", WEBAPP_REDIRECT))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("token отклоняет code клиента webapp при client_id=excel")
    void tokenRejectsCodeFromAnotherClient() throws Exception {
        String code = issueCode("webapp", WEBAPP_REDIRECT);

        mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("client_id", "excel")
                        .param("client_secret", "excel-secret-local")
                        .param("redirect_uri", EXCEL_REDIRECT))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("token отклоняет неверный grant_type")
    void tokenRejectsUnsupportedGrant() throws Exception {
        mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type": "password",
                                  "code": "x",
                                  "client_id": "webapp",
                                  "client_secret": "webapp-secret-local",
                                  "redirect_uri": "%s"
                                }
                                """.formatted(WEBAPP_REDIRECT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("excel-клиент: authorize → token → userinfo")
    void excelClientHappyPath() throws Exception {
        String code = issueCode("excel", EXCEL_REDIRECT);

        MvcResult tokenResult = mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("client_id", "excel")
                        .param("client_secret", "excel-secret-local")
                        .param("redirect_uri", EXCEL_REDIRECT))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tokenJson = objectMapper.readTree(tokenResult.getResponse().getContentAsString());
        String accessToken = tokenJson.get("access_token").asText();
        assertThat(accessToken).isNotBlank();

        MvcResult userinfo = mockMvc.perform(get("/userinfo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode info = objectMapper.readTree(userinfo.getResponse().getContentAsString());
        assertThat(info.get("email").asText()).isEqualTo("admin@local");
    }

    @Test
    @DisplayName("userinfo без Bearer — 401")
    void userinfoWithoutBearerIsUnauthorized() throws Exception {
        mockMvc.perform(get("/userinfo")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("userinfo с битым токеном — 401")
    void userinfoWithBadTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/userinfo")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    private String issueCode(String clientId, String redirectUri) throws Exception {
        MvcResult authorize = mockMvc.perform(get("/authorize")
                        .param("client_id", clientId)
                        .param("redirect_uri", redirectUri)
                        .param("state", "st")
                        .with(user("admin@local").roles("ADMIN", "USER")))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = authorize.getResponse().getHeader("Location");
        assertThat(location).contains("code=");
        return location.replaceAll(".*[?&]code=([^&]+).*", "$1");
    }
}
