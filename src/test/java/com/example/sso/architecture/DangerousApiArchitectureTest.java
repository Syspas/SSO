package com.example.sso.architecture;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.beans.XMLDecoder;
import java.io.ObjectInputStream;
import java.lang.ProcessBuilder;

import static com.example.sso.architecture.ProductionClasses.APP;
import static com.example.sso.architecture.ProductionClasses.CLASSES;
import static com.example.sso.support.Steps.step;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@Epic("Архитектура")
@Feature("Безопасные API")
@Tag("architecture")
@DisplayName("Опасные API в production-коде SSO")
class DangerousApiArchitectureTest {

    @Test
    @DisplayName("Нет ObjectInputStream и XMLDecoder")
    void noInsecureDeserializationApis() {
        step("Убедиться, что production-классы не зависят от ObjectInputStream и XMLDecoder", () ->
                noClasses().that().resideInAPackage(APP + "..")
                        .should().dependOnClassesThat()
                        .belongToAnyOf(ObjectInputStream.class, XMLDecoder.class)
                        .because("небезопасная десериализация не нужна")
                        .check(CLASSES));
    }

    @Test
    @DisplayName("Нет Runtime.exec и ProcessBuilder")
    void noCommandExecutionApis() {
        step("Убедиться, что production-классы не зависят от ProcessBuilder и не зовут Runtime.exec", () -> {
            noClasses().that().resideInAPackage(APP + "..")
                    .should().dependOnClassesThat()
                    .belongToAnyOf(ProcessBuilder.class)
                    .because("ProcessBuilder — это command injection")
                    .check(CLASSES);
            noClasses().that().resideInAPackage(APP + "..")
                    .should().callMethod(Runtime.class, "exec", String.class)
                    .because("Runtime.exec принимает строку команды")
                    .check(CLASSES);
        });
    }

    @Test
    @DisplayName("JPQL только без nativeQuery")
    void noNativeQueries() {
        step("Убедиться, что @Query не включает nativeQuery=true", () ->
                noMethods().that().areAnnotatedWith(Query.class)
                        .should(useNativeQuery())
                        .allowEmptyShould(true)
                        .because("native SQL со склейкой строк — путь к инъекции")
                        .check(CLASSES));
    }

    private static ArchCondition<JavaMethod> useNativeQuery() {
        return new ArchCondition<>("задавать nativeQuery=true") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Query query = method.getAnnotationOfType(Query.class);
                if (query != null && query.nativeQuery()) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " задаёт nativeQuery=true"));
                }
            }
        };
    }
}
