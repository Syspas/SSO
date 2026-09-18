package com.example.sso.security;

import com.example.sso.config.DataInitializer;
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

import static com.example.sso.support.Steps.step;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Инъекции в форму логина: SQL/LDAP-подобные строки не дают обхода аутентификации.
 */
@Epic("Безопасность")
@Feature("Инъекции логина")
@Tag("security")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SSO: инъекции в /login")
class LoginInjectionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SQL-подобная строка в username не аутентифицирует")
    void sqlLikeUsernameStaysUnauthenticated() throws Exception {
        step("Войти с username=admin@local' OR '1'='1", () ->
                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("username", "admin@local' OR '1'='1")
                                .param("password", DataInitializer.DEMO_PASSWORD)
                                .with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(unauthenticated()));
    }

    @Test
    @DisplayName("LDAP-подобная строка в username не аутентифицирует и не даёт 500")
    void ldapLikeUsernameStaysUnauthenticated() throws Exception {
        step("Войти с username=*)(uid=*))(|(uid=*", () ->
                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("username", "*)(uid=*))(|(uid=*")
                                .param("password", DataInitializer.DEMO_PASSWORD)
                                .with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(unauthenticated()));
    }

    @Test
    @DisplayName("Верный email с SQL-инъекцией в пароле не аутентифицирует")
    void sqlLikePasswordStaysUnauthenticated() throws Exception {
        step("Войти с password=' OR '1'='1", () ->
                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("username", "admin@local")
                                .param("password", "' OR '1'='1")
                                .with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(unauthenticated()));
    }
}
