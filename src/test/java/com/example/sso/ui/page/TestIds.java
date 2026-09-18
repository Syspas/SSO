package com.example.sso.ui.page;

import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;

/**
 * Локатор по {@code data-testid}. CSS-классы и XPath в UI-тестах не используем.
 */
public final class TestIds {

    private TestIds() {
    }

    /**
     * Элемент с точным {@code data-testid}.
     *
     * @param page   страница HtmlUnit
     * @param testId значение атрибута
     * @return элемент или {@code null}
     */
    public static DomElement byTestId(HtmlPage page, String testId) {
        return page.querySelector("[data-testid='" + testId + "']");
    }

    /**
     * Типизированный элемент с точным {@code data-testid}.
     *
     * @param page   страница
     * @param testId значение атрибута
     * @param <T>    тип HtmlUnit-элемента
     * @return элемент
     */
    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> T byTestId(HtmlPage page, String testId, Class<T> type) {
        DomElement element = byTestId(page, testId);
        if (element == null) {
            return null;
        }
        if (!type.isInstance(element)) {
            throw new IllegalStateException(
                    "data-testid='" + testId + "' ожидался " + type.getSimpleName()
                            + ", получен " + element.getClass().getSimpleName());
        }
        return (T) element;
    }
}
