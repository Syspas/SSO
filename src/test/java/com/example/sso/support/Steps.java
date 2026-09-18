package com.example.sso.support;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Шаг Allure с понятным русским именем.
 * Если шаг падает, имя шага попадает и в отчёт, и в текст исключения Surefire.
 */
public final class Steps {

    private static final Logger log = LoggerFactory.getLogger(Steps.class);

    private Steps() {
    }

    public static void step(String name, StepAction action) {
        Allure.step(name, () -> {
            log.info("Шаг: {}", name);
            try {
                action.run();
            } catch (AssertionError failure) {
                log.error("Шаг не пройден: {}", name, failure);
                throw new AssertionError("Шаг «" + name + "» не пройден: " + failure.getMessage(), failure);
            } catch (Exception error) {
                log.error("Шаг завершился ошибкой: {}", name, error);
                throw new IllegalStateException(
                        "Шаг «" + name + "» завершился ошибкой: " + error.getMessage(), error);
            }
        });
    }

    public static <T> T step(String name, StepSupplier<T> action) {
        return Allure.step(name, () -> {
            log.info("Шаг: {}", name);
            try {
                return action.get();
            } catch (AssertionError failure) {
                log.error("Шаг не пройден: {}", name, failure);
                throw new AssertionError("Шаг «" + name + "» не пройден: " + failure.getMessage(), failure);
            } catch (Exception error) {
                log.error("Шаг завершился ошибкой: {}", name, error);
                throw new IllegalStateException(
                        "Шаг «" + name + "» завершился ошибкой: " + error.getMessage(), error);
            }
        });
    }

    @FunctionalInterface
    public interface StepAction {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface StepSupplier<T> {
        T get() throws Exception;
    }
}
