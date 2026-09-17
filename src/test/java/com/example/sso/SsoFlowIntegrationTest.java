package com.example.sso;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        JsonNode tokenJson = objectMapper.readTree(tokenResult.getResponse().getContentAsString());
        String accessToken = tokenJson.get("access_token").asText();
        assertThat(accessToken).isNotBlank();

        MvcResult userinfo = mockMvc.perform(get("/userinfo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode info = objectMapper.readTree(userinfo.getResponse().getContentAsString());
        assertThat(info.get("email").asText()).isEqualTo("admin@local");
        assertThat(info.get("roles").toString()).contains("ROLE_ADMIN");
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
}
