package com.example.sso;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("Юнит")
@Feature("OAuth happy path")
@SpringBootTest
@AutoConfigureMockMvc
class SsoFlowIntegrationTest {

    private static final String REDIRECT =
            "http://127.0.0.1:8080/login/sso/callback";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authorizeTokenUserinfoHappyPath() throws Exception {
        MvcResult authorize = mockMvc.perform(get("/authorize")
                        .param("client_id", "webapp")
                        .param("redirect_uri", REDIRECT)
                        .param("state", "abc")
                        .with(user("admin@local").roles("ADMIN", "USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("code=")))
                .andReturn();

        String location = authorize.getResponse().getHeader("Location");
        assertThat(location).contains("state=abc");
        String code = location.replaceAll(".*[?&]code=([^&]+).*", "$1");

        MvcResult tokenResult = mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type": "authorization_code",
                                  "code": "%s",
                                  "client_id": "webapp",
                                  "client_secret": "webapp-secret-local",
                                  "redirect_uri": "%s"
                                }
                                """.formatted(code, REDIRECT)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tokenJson = objectMapper.readTree(
                tokenResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String accessToken = tokenJson.get("access_token").asText();
        assertThat(accessToken).isNotBlank();

        MvcResult userinfo = mockMvc.perform(get("/userinfo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode info = objectMapper.readTree(
                userinfo.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(info.get("email").asText()).isEqualTo("admin@local");
        assertThat(info.get("lastName").asText()).isEqualTo("Локальный");
        assertThat(info.get("firstName").asText()).isEqualTo("Админ");
        assertThat(info.path("middleName").isNull() || info.path("middleName").asText("").isEmpty())
                .isTrue();
        assertThat(info.get("roles").toString()).contains("ROLE_ADMIN");
    }

    @Test
    void userinfoForPortalAdminIncludesFio() throws Exception {
        MvcResult authorize = mockMvc.perform(get("/authorize")
                        .param("client_id", "webapp")
                        .param("redirect_uri", REDIRECT)
                        .param("state", "fio")
                        .with(user("admin@mail.ru").roles("ADMIN", "USER")))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String code = authorize.getResponse().getHeader("Location")
                .replaceAll(".*[?&]code=([^&]+).*", "$1");

        MvcResult tokenResult = mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grant_type": "authorization_code",
                                  "code": "%s",
                                  "client_id": "webapp",
                                  "client_secret": "webapp-secret-local",
                                  "redirect_uri": "%s"
                                }
                                """.formatted(code, REDIRECT)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(
                        tokenResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("access_token").asText();

        JsonNode info = objectMapper.readTree(mockMvc.perform(get("/userinfo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
        assertThat(info.get("email").asText()).isEqualTo("admin@mail.ru");
        assertThat(info.get("lastName").asText()).isEqualTo("Иванов");
        assertThat(info.get("firstName").asText()).isEqualTo("Иван");
        assertThat(info.get("middleName").asText()).isEqualTo("Иванович");
    }

    @Test
    void tokenFormUrlEncodedWorks() throws Exception {
        MvcResult authorize = mockMvc.perform(get("/authorize")
                        .param("client_id", "webapp")
                        .param("redirect_uri", REDIRECT)
                        .with(user("admin@local").roles("ADMIN", "USER")))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = authorize.getResponse().getHeader("Location");
        String code = location.replaceAll(".*[?&]code=([^&]+).*", "$1");

        mockMvc.perform(post("/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("client_id", "webapp")
                        .param("client_secret", "webapp-secret-local")
                        .param("redirect_uri", REDIRECT))
                .andExpect(status().isOk());
    }

    @Test
    void unknownClientIsBadRequest() throws Exception {
        mockMvc.perform(get("/authorize")
                        .param("client_id", "unknown")
                        .param("redirect_uri", REDIRECT)
                        .with(user("admin@local").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void logoutWithRegisteredPostLogoutRedirectGoesBackToPortal() throws Exception {
        mockMvc.perform(get("/logout")
                        .param("client_id", "webapp")
                        .param("post_logout_redirect_uri", "http://127.0.0.1:8088/login?logout")
                        .with(user("admin@mail.ru").roles("ADMIN", "USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://127.0.0.1:8088/login?logout"));
    }

    @Test
    void logoutRejectsUnregisteredPostLogoutRedirect() throws Exception {
        mockMvc.perform(get("/logout")
                        .param("client_id", "webapp")
                        .param("post_logout_redirect_uri", "http://evil.example/phish")
                        .with(user("admin@mail.ru").roles("ADMIN", "USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login?logout")));
    }
}
