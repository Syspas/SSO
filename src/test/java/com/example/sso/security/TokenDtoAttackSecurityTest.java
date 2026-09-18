package com.example.sso.security;

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

import static com.example.sso.support.Steps.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Атаки на DTO POST /token: валидация, grant_type, redirect_uri tricks, чужой секрет.
 */
@Epic("Безопасность")
@Feature("Token DTO")
@Tag("security")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SSO: атаки на TokenRequest/TokenForm")
class TokenDtoAttackSecurityTest {

    private static final String WEBAPP_REDIRECT =
            "http://127.0.0.1:8080/login/sso/callback";
    private static final String WEBAPP_SECRET = "webapp-secret-local";
    private static final String EXCEL_SECRET = "excel-secret-local";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Пустые поля JSON → 400 Invalid request без стектрейса")
    void blankJsonFieldsReturnInvalidRequest() throws Exception {
        step("Отправить JSON с пустыми полями", () -> {
            MvcResult result = mockMvc.perform(post("/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "grant_type": " ",
                                      "code": "",
                                      "client_id": "",
                                      "client_secret": "",
                                      "redirect_uri": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            assertThat(body).contains("Invalid request");
            assertThat(body).doesNotContain("Exception");
            assertThat(body).doesNotContain("at com.example");
        });
    }

    @Test
    @DisplayName("Обрезанный JSON и массив → 4xx")
    void malformedJsonRejected() throws Exception {
        step("Обрезанный JSON", () ->
                mockMvc.perform(post("/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"grant_type\":\"authorization_code\""))
                        .andExpect(status().is4xxClientError()));
        step("JSON-массив вместо объекта", () ->
                mockMvc.perform(post("/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[]"))
                        .andExpect(status().is4xxClientError()));
    }

    @Test
    @DisplayName("Неизвестный Content-Type на /token → 4xx")
    void unknownContentTypeRejected() throws Exception {
        step("text/plain на /token", () ->
                mockMvc.perform(post("/token")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("grant_type=authorization_code"))
                        .andExpect(status().is4xxClientError()));
    }

    @Test
    @DisplayName("Неподдерживаемые grant_type отклоняются")
    void unsupportedGrantsRejected() throws Exception {
        for (String grant : new String[]{"password", "client_credentials", "refresh_token"}) {
            String g = grant;
            step("grant_type=" + g, () ->
                    mockMvc.perform(post("/token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "grant_type": "%s",
                                              "code": "x",
                                              "client_id": "webapp",
                                              "client_secret": "%s",
                                              "redirect_uri": "%s"
                                            }
                                            """.formatted(g, WEBAPP_SECRET, WEBAPP_REDIRECT)))
                            .andExpect(status().isBadRequest()));
        }
    }

    @Test
    @DisplayName("authorization_code с чужим redirect_uri чем при выдаче code")
    void redirectUriMismatchOnTokenRejected() throws Exception {
        String code = issueCode("webapp", WEBAPP_REDIRECT);
        String otherRegistered = "http://localhost:8080/login/sso/callback";
        step("Обмен code с другим зарегистрированным redirect_uri → 400", () ->
                mockMvc.perform(post("/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("grant_type", "authorization_code")
                                .param("code", code)
                                .param("client_id", "webapp")
                                .param("client_secret", WEBAPP_SECRET)
                                .param("redirect_uri", otherRegistered))
                        .andExpect(status().isBadRequest()));
    }

    @Test
    @DisplayName("Open-redirect варианты redirect_uri на /token отклоняются")
    void openRedirectVariantsOnTokenRejected() throws Exception {
        String[] evilUris = {
                WEBAPP_REDIRECT + "?next=https://evil.example",
                WEBAPP_REDIRECT + "#evil",
                "http://127.0.0.1:8080/login/sso/callback.evil",
                "javascript:alert(1)",
                "//evil.example/callback",
                "http://127.0.0.1:8080/login/sso/callback\r\nLocation: https://evil.example"
        };
        for (String uri : evilUris) {
            String u = uri;
            step("redirect_uri атака: " + summarize(u), () ->
                    mockMvc.perform(post("/token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "grant_type": "authorization_code",
                                              "code": "dummy",
                                              "client_id": "webapp",
                                              "client_secret": "%s",
                                              "redirect_uri": %s
                                            }
                                            """.formatted(WEBAPP_SECRET, jsonString(u))))
                            .andExpect(status().isBadRequest()));
        }
    }

    @Test
    @DisplayName("Неизвестный client_id и секрет соседнего клиента")
    void unknownClientAndCrossClientSecretRejected() throws Exception {
        step("Неизвестный client_id", () ->
                mockMvc.perform(post("/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "grant_type": "authorization_code",
                                          "code": "x",
                                          "client_id": "attacker",
                                          "client_secret": "any",
                                          "redirect_uri": "%s"
                                        }
                                        """.formatted(WEBAPP_REDIRECT)))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("Unknown client_id"))));
        step("Секрет excel при client_id=webapp", () ->
                mockMvc.perform(post("/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "grant_type": "authorization_code",
                                          "code": "x",
                                          "client_id": "webapp",
                                          "client_secret": "%s",
                                          "redirect_uri": "%s"
                                        }
                                        """.formatted(EXCEL_SECRET, WEBAPP_REDIRECT)))
                        .andExpect(status().isBadRequest()));
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

    private static String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                + "\"";
    }

    private static String summarize(String uri) {
        if (uri.length() <= 48) {
            return uri.replace("\r", "\\r").replace("\n", "\\n");
        }
        return uri.substring(0, 45).replace("\r", "\\r").replace("\n", "\\n") + "...";
    }
}
