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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Перечисление путей и поверхностей атаки: методы HTTP, actuator, H2, CSRF, logout open redirect.
 *
 * <p>Сценарий «authorize без сессии → /login» — в {@code SsoClientSecurityIntegrationTest}.
 */
@Epic("Безопасность")
@Feature("Перечисление путей")
@Tag("security")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SSO: пути и поверхности атаки")
class PathEnumerationSecurityTest {

    private static final String WEBAPP_REDIRECT =
            "http://127.0.0.1:8080/login/sso/callback";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("PUT/DELETE/PATCH на /token не дают 500")
    void tokenRejectsUnsupportedMethods() throws Exception {
        step("PUT /token → 4xx", () ->
                mockMvc.perform(put("/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andExpect(status().is4xxClientError()));
        step("DELETE /token → 4xx", () ->
                mockMvc.perform(delete("/token"))
                        .andExpect(status().is4xxClientError()));
        step("PATCH /token → 4xx", () ->
                mockMvc.perform(patch("/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andExpect(status().is4xxClientError()));
    }

    @Test
    @DisplayName("PUT/DELETE/PATCH на /authorize и /userinfo не дают 500")
    void authorizeAndUserinfoRejectUnsupportedMethods() throws Exception {
        step("PUT /authorize → 4xx", () ->
                mockMvc.perform(put("/authorize")
                                .param("client_id", "webapp")
                                .param("redirect_uri", WEBAPP_REDIRECT)
                                .with(user("admin@local").roles("ADMIN", "USER")))
                        .andExpect(status().is4xxClientError()));
        step("DELETE /userinfo → 4xx", () ->
                mockMvc.perform(delete("/userinfo"))
                        .andExpect(status().is4xxClientError()));
        step("PATCH /userinfo → 4xx", () ->
                mockMvc.perform(patch("/userinfo"))
                        .andExpect(status().is4xxClientError()));
    }

    @Test
    @DisplayName("Actuator env/beans/mappings и H2-console закрыты")
    void sensitiveActuatorAndH2AreNotExposed() throws Exception {
        for (String path : new String[]{
                "/actuator/env",
                "/actuator/beans",
                "/actuator/mappings",
                "/h2-console",
                "/h2-console/"
        }) {
            String p = path;
            step("GET " + p + " не отдаёт 200 с секретами", () -> {
                MvcResult result = mockMvc.perform(get(p)).andReturn();
                int status = result.getResponse().getStatus();
                assertThat(status)
                        .as("путь %s не должен отдавать 200", p)
                        .isNotEqualTo(200);
                String body = result.getResponse().getContentAsString();
                assertThat(body).doesNotContain("webapp-secret-local");
                assertThat(body).doesNotContain("change-me-sso-jwt");
            });
        }
    }

    @Test
    @DisplayName("POST /login без CSRF отклоняется")
    void loginWithoutCsrfIsRejected() throws Exception {
        step("Отправить логин без CSRF-токена и получить отказ", () ->
                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("username", "admin@local")
                                .param("password", "PortalSeed9!Change"))
                        .andExpect(status().isForbidden()));
    }

    @Test
    @DisplayName("POST /token без CSRF допускается, но секрет обязателен")
    void tokenWithoutCsrfStillRequiresSecret() throws Exception {
        step("POST /token с верным code-заглушкой и неверным секретом → 400", () ->
                mockMvc.perform(post("/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "grant_type": "authorization_code",
                                          "code": "not-a-real-code",
                                          "client_id": "webapp",
                                          "client_secret": "wrong",
                                          "redirect_uri": "%s"
                                        }
                                        """.formatted(WEBAPP_REDIRECT)))
                        .andExpect(status().isBadRequest()));
    }

    @Test
    @DisplayName("Logout с чужим post_logout_redirect_uri не уводит на evil")
    void logoutRejectsEvilPostLogoutRedirect() throws Exception {
        step("Выйти с post_logout_redirect_uri=https://evil.example", () ->
                mockMvc.perform(get("/logout")
                                .param("client_id", "webapp")
                                .param("post_logout_redirect_uri", "https://evil.example/phish")
                                .with(user("admin@local").roles("ADMIN", "USER"))
                                .with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(header().string("Location", containsString("/login")))
                        .andExpect(header().string("Location", containsString("logout")))
                        .andExpect(header().string("Location", not(containsString("evil.example")))));
    }

    @Test
    @DisplayName("Logout с зарегистрированным post_logout_redirect_uri разрешён")
    void logoutAllowsRegisteredPostLogoutRedirect() throws Exception {
        String allowed = "http://127.0.0.1:8080/login?logout";
        step("Выйти с зарегистрированным post_logout_redirect_uri", () ->
                mockMvc.perform(get("/logout")
                                .param("client_id", "webapp")
                                .param("post_logout_redirect_uri", allowed)
                                .with(user("admin@local").roles("ADMIN", "USER"))
                                .with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(header().string("Location", allowed)));
    }
}
