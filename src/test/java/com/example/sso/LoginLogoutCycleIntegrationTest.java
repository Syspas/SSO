package com.example.sso;

import com.example.sso.config.DataInitializer;
import com.example.sso.config.SsoProperties;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.example.sso.support.Steps.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Циклы выход → вход: прямой /login?logout должен уводить на портал,
 * незавершённый /authorize — обратно на выдачу code.
 */
@Epic("Юнит")
@Feature("Циклы вход/выход")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SSO: циклы login/logout")
class LoginLogoutCycleIntegrationTest {

    private static final String WEBAPP_REDIRECT =
            "http://127.0.0.1:8080/login/sso/callback";
    private static final String ADMIN = "admin@mail.ru";
    private static final String USER = "user@mail.ru";
    private static final String LOCAL_ADMIN = "admin@local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SsoProperties ssoProperties;

    @Nested
    @DisplayName("Один пользователь")
    class OneUser {

        @Test
        @DisplayName("После /login?logout вход ведёт на dashboard, не на /sso/")
        void loginAfterLogoutQueryGoesToPortalDashboard() throws Exception {
            MockHttpSession session = openLoginLogoutPage();
            String location = step("Войти " + ADMIN + " после ?logout", () ->
                    loginExpectDashboard(session).location());
            step("Location — портал, не корень SSO", () -> {
                assertThat(location).isEqualTo(ssoProperties.getPostLoginRedirect());
                assertThat(location).contains("/dashboard");
                assertThat(location).doesNotEndWith("/sso/");
            });
        }

        @Test
        @DisplayName("Выйти → войти → выйти → войти одним пользователем")
        void logoutLoginLogoutLoginSameUser() throws Exception {
            MockHttpSession afterFirstLogin = step("Первый вход", () ->
                    loginExpectDashboard(openLoginLogoutPage()).session());
            MockHttpSession afterFirstLogout = step("Первый выход", () -> logout(afterFirstLogin));
            MockHttpSession afterSecondLogin = step("Второй вход", () ->
                    loginExpectDashboard(afterFirstLogout).session());
            MockHttpSession afterSecondLogout = step("Второй выход", () -> logout(afterSecondLogin));
            step("Третий вход", () -> loginExpectDashboard(afterSecondLogout));
        }
    }

    @Nested
    @DisplayName("Несколько пользователей подряд")
    class ManyUsers {

        @Test
        @DisplayName("A выйти/войти, затем B выйти/войти")
        void sequentialUsersEachLogoutLogin() throws Exception {
            MockHttpSession afterAdmin = step("Войти " + ADMIN, () ->
                    loginExpectDashboard(openLoginLogoutPage(), ADMIN).session());
            MockHttpSession afterAdminOut = step("Выйти " + ADMIN, () -> logout(afterAdmin));
            MockHttpSession afterUser = step("Войти " + USER, () ->
                    loginExpectDashboard(afterAdminOut, USER).session());
            MockHttpSession afterUserOut = step("Выйти " + USER, () -> logout(afterUser));
            step("Войти " + LOCAL_ADMIN, () -> loginExpectDashboard(afterUserOut, LOCAL_ADMIN));
        }
    }

    @Nested
    @DisplayName("Смешанно")
    class Mixed {

        @Test
        @DisplayName("A → выход → B → выход → снова A")
        void switchUsersWithoutAuthorize() throws Exception {
            MockHttpSession afterA = step("Войти A", () ->
                    loginExpectDashboard(openLoginLogoutPage(), ADMIN).session());
            MockHttpSession afterAOut = step("Выйти A", () -> logout(afterA));
            MockHttpSession afterB = step("Войти B", () ->
                    loginExpectDashboard(afterAOut, USER).session());
            MockHttpSession afterBOut = step("Выйти B", () -> logout(afterB));
            step("Снова A", () -> loginExpectDashboard(afterBOut, ADMIN));
        }

        @Test
        @DisplayName("Сначала /authorize, после логина остаёмся в OAuth, не на dashboard")
        void pendingAuthorizeWinsOverDashboard() throws Exception {
            MvcResult authorize = step("Открыть /authorize без сессии", () ->
                    mockMvc.perform(get("/authorize")
                                    .param("client_id", "webapp")
                                    .param("redirect_uri", WEBAPP_REDIRECT)
                                    .param("state", "cycle"))
                            .andExpect(status().is3xxRedirection())
                            .andReturn());
            MockHttpSession session = (MockHttpSession) authorize.getRequest().getSession();
            MvcResult login = step("Войти после сохранённого authorize", () ->
                    mockMvc.perform(post("/login")
                                    .session(session)
                                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                    .param("username", ADMIN)
                                    .param("password", DataInitializer.DEMO_PASSWORD)
                                    .with(csrf()))
                            .andExpect(status().is3xxRedirection())
                            .andExpect(authenticated().withUsername(ADMIN))
                            .andReturn());
            String location = login.getResponse().getHeader("Location");
            step("Редирект на authorize, не на dashboard", () -> {
                assertThat(location).contains("/authorize");
                assertThat(location).isNotEqualTo(ssoProperties.getPostLoginRedirect());
            });
        }
    }

    private MockHttpSession openLoginLogoutPage() throws Exception {
        return step("Открыть /login?logout", () -> {
            MvcResult page = mockMvc.perform(get("/login").param("logout", ""))
                    .andExpect(status().isOk())
                    .andReturn();
            return (MockHttpSession) page.getRequest().getSession();
        });
    }

    private LoginResult loginExpectDashboard(MockHttpSession session, String email) throws Exception {
        MvcResult login = mockMvc.perform(post("/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", email)
                        .param("password", DataInitializer.DEMO_PASSWORD)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(email))
                .andExpect(header().string("Location", ssoProperties.getPostLoginRedirect()))
                .andReturn();
        return new LoginResult(
                (MockHttpSession) login.getRequest().getSession(),
                login.getResponse().getHeader("Location"));
    }

    private LoginResult loginExpectDashboard(MockHttpSession session) throws Exception {
        return loginExpectDashboard(session, ADMIN);
    }

    private record LoginResult(MockHttpSession session, String location) {
    }

    private MockHttpSession logout(MockHttpSession session) throws Exception {
        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated());
        return openLoginLogoutPage();
    }
}
