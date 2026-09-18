package com.example.sso.security;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.example.sso.support.Steps.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Атаки на query-параметры GET /authorize.
 */
@Epic("Безопасность")
@Feature("Authorize параметры")
@Tag("security")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SSO: атаки на параметры /authorize")
class AuthorizeParamSecurityTest {

    private static final String WEBAPP_REDIRECT =
            "http://127.0.0.1:8080/login/sso/callback";
    private static final String EXCEL_REDIRECT =
            "http://127.0.0.1:8081/excel/login/sso/callback";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Отсутствующие client_id и redirect_uri → 4xx")
    void missingRequiredParamsRejected() throws Exception {
        step("Без client_id", () ->
                mockMvc.perform(get("/authorize")
                                .param("redirect_uri", WEBAPP_REDIRECT)
                                .with(user("admin@local").roles("ADMIN", "USER")))
                        .andExpect(status().is4xxClientError()));
        step("Без redirect_uri", () ->
                mockMvc.perform(get("/authorize")
                                .param("client_id", "webapp")
                                .with(user("admin@local").roles("ADMIN", "USER")))
                        .andExpect(status().is4xxClientError()));
    }

    @Test
    @DisplayName("redirect_uri из allowlist другого клиента отклоняется")
    void redirectUriOfAnotherClientRejected() throws Exception {
        step("client_id=webapp + excel redirect_uri → 400", () ->
                mockMvc.perform(get("/authorize")
                                .param("client_id", "webapp")
                                .param("redirect_uri", EXCEL_REDIRECT)
                                .with(user("admin@local").roles("ADMIN", "USER")))
                        .andExpect(status().isBadRequest()));
    }

    @Test
    @DisplayName("state с кавычками и script не ломает хост редиректа")
    void stateInjectionDoesNotBreakRedirectHost() throws Exception {
        String maliciousState = "\"><script>alert(1)</script>";
        MvcResult result = step("authorize со state-инъекцией", () ->
                mockMvc.perform(get("/authorize")
                                .param("client_id", "webapp")
                                .param("redirect_uri", WEBAPP_REDIRECT)
                                .param("state", maliciousState)
                                .with(user("admin@local").roles("ADMIN", "USER")))
                        .andExpect(status().is3xxRedirection())
                        .andReturn());
        String location = result.getResponse().getHeader("Location");
        step("Location начинается с allowlist URI и содержит code", () -> {
            assertThat(location).startsWith(WEBAPP_REDIRECT);
            assertThat(location).contains("code=");
            assertThat(location).doesNotContain("<script>");
            assertThat(location).doesNotStartWith("javascript:");
        });
    }

    @Test
    @DisplayName("Open-redirect варианты redirect_uri на /authorize отклоняются")
    void openRedirectVariantsOnAuthorizeRejected() throws Exception {
        String[] evilUris = {
                WEBAPP_REDIRECT + "/../evil",
                "http://127.0.0.1:8080/login/sso/callback.evil",
                "javascript:alert(1)",
                "//evil.example/callback",
                "https://evil.example/callback"
        };
        for (String uri : evilUris) {
            String u = uri;
            step("authorize redirect_uri=" + u, () ->
                    mockMvc.perform(get("/authorize")
                                    .param("client_id", "webapp")
                                    .param("redirect_uri", u)
                                    .with(user("admin@local").roles("ADMIN", "USER")))
                            .andExpect(status().isBadRequest()));
        }
    }
}
