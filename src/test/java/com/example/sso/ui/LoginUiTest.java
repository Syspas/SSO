package com.example.sso.ui;

import com.example.sso.config.DataInitializer;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HtmlUnit: форма входа и authorize после сессии. Firefox не нужен.
 * Локаторы только по {@code data-testid}.
 */
@Epic("UI")
@Feature("Логин SSO")
@Tag("ui")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("SSO login UI (HtmlUnit)")
class LoginUiTest {

    private static final String WEBAPP_REDIRECT =
            "http://127.0.0.1:8080/login/sso/callback";
    private static final String WRONG_PASSWORD = "wrong-password";

    @LocalServerPort
    private int port;

    private WebClient webClient;
    private LoginPage loginPage;

    @BeforeEach
    void setUp() {
        webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setRedirectEnabled(true);
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

    @Test
    @DisplayName("Страница /login содержит форму email/пароль по data-testid")
    void loginPageShowsForm() throws Exception {
        Steps.step("Открыть /sso/login", () -> loginPage.open());
        Steps.step("На странице есть поля и CSRF по data-testid", () -> {
            assertThat(loginPage.getPage().getTitleText()).contains("SSO");
            loginPage.shouldBeOpen();
            loginPage.shouldHaveNoFlashMessages();
        });
    }

    @Test
    @DisplayName("Неверный пароль оставляет на логине с ошибкой")
    void badPasswordStaysOnLogin() throws Exception {
        Steps.step("Открыть /sso/login", () -> loginPage.open());
        Page after = Steps.step("Отправить форму с неверным паролем", () ->
                loginPage.login("admin@local", WRONG_PASSWORD));
        Steps.step("Показана ошибка входа по data-testid", () -> {
            assertThat(after).isInstanceOf(HtmlPage.class);
            loginPage.bind((HtmlPage) after).shouldBeOpen();
            assertThat(after.getUrl().toString()).contains("error");
            loginPage.shouldShowError("Неверный email или пароль");
        });
    }

    @Test
    @DisplayName("Верный пароль открывает authorize с code")
    void loginThenAuthorizeIssuesCode() throws Exception {
        webClient.getOptions().setRedirectEnabled(false);
        Steps.step("Открыть /sso/login", () -> loginPage.open());
        Steps.step("Войти демо-учёткой", () -> {
            Page after = loginPage.login("admin@local", DataInitializer.DEMO_PASSWORD);
            assertThat(after).isNotNull();
            String location = after.getWebResponse().getResponseHeaderValue("Location");
            assertThat(location).contains("/dashboard");
        });
        Page authorize = Steps.step("Открыть /authorize после сессии", () ->
                webClient.getPage(base() + "/authorize?client_id=webapp&redirect_uri="
                        + java.net.URLEncoder.encode(WEBAPP_REDIRECT, java.nio.charset.StandardCharsets.UTF_8)
                        + "&state=ui"));
        String location = authorize.getWebResponse().getResponseHeaderValue("Location");
        Steps.step("Редирект на callback с code", () -> {
            assertThat(location).startsWith(WEBAPP_REDIRECT);
            assertThat(location).contains("code=");
            assertThat(location).contains("state=ui");
        });
    }

    @Test
    @DisplayName("Выход показывает сообщение logout-ok по data-testid")
    void logoutShowsMessage() throws Exception {
        webClient.getOptions().setRedirectEnabled(false);
        Steps.step("Открыть /sso/login", () -> loginPage.open());
        Steps.step("Войти демо-учёткой", () ->
                loginPage.login("admin@local", DataInitializer.DEMO_PASSWORD));
        webClient.getOptions().setRedirectEnabled(true);
        HtmlPage afterLogout = Steps.step("Выйти через /logout", () ->
                webClient.getPage(base() + "/logout"));
        Steps.step("Увидеть сообщение о выходе", () -> {
            loginPage.bind(afterLogout).shouldBeOpen();
            loginPage.shouldShowLogout("Вы вышли из SSO");
        });
    }
}
