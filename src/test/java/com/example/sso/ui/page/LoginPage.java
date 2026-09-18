package com.example.sso.ui.page;

import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Страница входа SSO. Пароль метод принимает, но в лог его не пишет — пишет вызывающий шаг.
 */
public class LoginPage {

    static final String PAGE = "sso-login-page";
    static final String FORM = "sso-login__form";
    static final String EMAIL = "sso-login__email";
    static final String PASSWORD = "sso-login__password";
    static final String SUBMIT = "sso-login__submit";
    static final String ERROR = "sso-login__error";
    static final String LOGOUT_OK = "sso-login__logout-ok";
    static final String TITLE = "sso-login__title";
    static final String CSRF = "sso-login__csrf";

    private final WebClient webClient;
    private final String baseUrl;
    private HtmlPage page;

    public LoginPage(WebClient webClient, String baseUrl) {
        this.webClient = webClient;
        this.baseUrl = baseUrl;
    }

    /**
     * Открывает форму входа.
     *
     * @return эта страница
     */
    public LoginPage open() throws Exception {
        page = webClient.getPage(baseUrl + "/login");
        shouldBeOpen();
        return this;
    }

    /**
     * Привязывает уже загруженную страницу (после POST/редиректа).
     *
     * @param loaded страница после навигации
     * @return эта страница
     */
    public LoginPage bind(HtmlPage loaded) {
        this.page = loaded;
        return this;
    }

    public HtmlPage getPage() {
        return page;
    }

    /**
     * Ждёт, что открыта именно форма входа.
     *
     * @return эта страница
     */
    public LoginPage shouldBeOpen() {
        assertThat(TestIds.byTestId(page, PAGE)).isNotNull();
        assertThat(TestIds.byTestId(page, FORM)).isNotNull();
        assertThat(TestIds.byTestId(page, EMAIL)).isNotNull();
        assertThat(TestIds.byTestId(page, PASSWORD)).isNotNull();
        assertThat(TestIds.byTestId(page, SUBMIT)).isNotNull();
        assertThat(TestIds.byTestId(page, CSRF)).isNotNull();
        assertThat(TestIds.byTestId(page, TITLE)).isNotNull();
        return this;
    }

    /**
     * Чистая страница входа без flash error/logout.
     */
    public void shouldHaveNoFlashMessages() {
        assertThat(TestIds.byTestId(page, ERROR)).isNull();
        assertThat(TestIds.byTestId(page, LOGOUT_OK)).isNull();
    }

    /**
     * Отправляет форму. Значение пароля в отчёт не попадает, если шаг его не назвал.
     *
     * <p>После успешного входа без {@code /authorize} редирект на портал
     * ({@code sso.post-login-redirect}) — это не {@link HtmlPage}. Cookie SSO уже есть.
     *
     * @param emailValue    логин
     * @param passwordValue пароль, не логировать
     * @return ответ после submit
     */
    public Page login(String emailValue, String passwordValue) throws Exception {
        HtmlInput email = TestIds.byTestId(page, EMAIL, HtmlInput.class);
        HtmlInput password = TestIds.byTestId(page, PASSWORD, HtmlInput.class);
        HtmlButton submit = TestIds.byTestId(page, SUBMIT, HtmlButton.class);
        email.setValueAttribute(emailValue);
        password.setValueAttribute(passwordValue);
        Page result = submit.click();
        if (result instanceof HtmlPage htmlPage) {
            page = htmlPage;
        }
        return result;
    }

    /**
     * Сообщение об отказе во входе.
     *
     * @param text ожидаемый текст
     */
    public void shouldShowError(String text) {
        DomElement error = TestIds.byTestId(page, ERROR);
        assertThat(error).isNotNull();
        assertThat(error.asNormalizedText()).contains(text);
        assertThat(TestIds.byTestId(page, LOGOUT_OK)).isNull();
    }

    /**
     * Сообщение после выхода.
     *
     * @param text ожидаемый текст
     */
    public void shouldShowLogout(String text) {
        DomElement ok = TestIds.byTestId(page, LOGOUT_OK);
        assertThat(ok).isNotNull();
        assertThat(ok.asNormalizedText()).contains(text);
        assertThat(TestIds.byTestId(page, ERROR)).isNull();
    }
}
