package com.example.sso.ui;

import com.example.sso.config.DataInitializer;
import com.example.sso.config.SsoProperties;
import com.example.sso.support.Steps;
import com.example.sso.ui.page.LoginPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HtmlUnit: циклы выход/вход одним и несколькими пользователями.
 */
@Epic("UI")
@Feature("Циклы вход/выход")
@Tag("ui")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("SSO login/logout UI cycles")
class LoginLogoutCycleUiTest {

    private static final String ADMIN = "admin@mail.ru";
    private static final String USER = "user@mail.ru";
    private static final String LOCAL_ADMIN = "admin@local";

    @LocalServerPort
    private int port;

    @Autowired
    private SsoProperties ssoProperties;

    private WebClient webClient;
    private LoginPage loginPage;

    @BeforeEach
    void setUp() {
        webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setRedirectEnabled(false);
        loginPage = new LoginPage(webClient, base());
    }

    @AfterEach
    void tearDown() {
        if (webClient != null) {
            webClient.close();
        }
    }

    private String base() {
        return "http://127.0.0.1:" + port + "/sso";
    }

    @Nested
    @DisplayName("Один пользователь")
    class OneUser {

        @Test
        @DisplayName("/login?logout → вход → dashboard, не /sso/")
        void loginAfterLogoutLandingGoesToDashboard() throws Exception {
            Steps.step("Открыть /login?logout", () ->
                    loginPage.bind(webClient.getPage(base() + "/login?logout")).shouldBeOpen());
            Page after = Steps.step("Войти " + ADMIN, () ->
                    loginPage.login(ADMIN, DataInitializer.DEMO_PASSWORD));
            Steps.step("Location — dashboard портала", () ->
                    assertDashboardRedirect(after));
        }

        @Test
        @DisplayName("войти → выйти → войти → выйти")
        void loginLogoutTwice() throws Exception {
            loginThenLogout(ADMIN);
            loginThenLogout(ADMIN);
        }
    }

    @Nested
    @DisplayName("Несколько пользователей")
    class ManyUsers {

        @Test
        @DisplayName("Три учётки подряд: вход и выход")
        void threeUsersInARow() throws Exception {
            loginThenLogout(ADMIN);
            loginThenLogout(USER);
            loginThenLogout(LOCAL_ADMIN);
        }
    }

    @Nested
    @DisplayName("Смешанно")
    class Mixed {

        @Test
        @DisplayName("A, B, снова A")
        void switchBackToFirstUser() throws Exception {
            loginThenLogout(ADMIN);
            loginThenLogout(USER);
            loginThenLogout(ADMIN);
        }
    }

    private void loginThenLogout(String email) throws Exception {
        Steps.step("Открыть /login?logout", () ->
                loginPage.bind(webClient.getPage(base() + "/login?logout")).shouldBeOpen());
        Page afterLogin = Steps.step("Войти " + email, () ->
                loginPage.login(email, DataInitializer.DEMO_PASSWORD));
        Steps.step("Проверить редирект на dashboard", () -> assertDashboardRedirect(afterLogin));
        webClient.getOptions().setRedirectEnabled(true);
        HtmlPage afterLogout = Steps.step("Выйти", () -> webClient.getPage(base() + "/logout"));
        webClient.getOptions().setRedirectEnabled(false);
        Steps.step("Снова форма с logout-ok", () -> {
            loginPage.bind(afterLogout).shouldBeOpen();
            loginPage.shouldShowLogout("Вы вышли из SSO");
        });
    }

    private void assertDashboardRedirect(Page afterLogin) {
        String location = afterLogin.getWebResponse().getResponseHeaderValue("Location");
        if (location == null) {
            location = afterLogin.getUrl().toString();
        }
        assertThat(location).isEqualTo(ssoProperties.getPostLoginRedirect());
        assertThat(location).doesNotContain("/sso/");
    }
}
