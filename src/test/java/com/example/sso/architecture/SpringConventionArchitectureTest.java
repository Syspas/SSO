package com.example.sso.architecture;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.example.sso.architecture.ProductionClasses.APP;
import static com.example.sso.architecture.ProductionClasses.CLASSES;
import static com.example.sso.architecture.ProductionClasses.REPOSITORY;
import static com.example.sso.architecture.ProductionClasses.SERVICE;
import static com.example.sso.architecture.ProductionClasses.WEB;
import static com.example.sso.support.Steps.step;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

@Epic("Архитектура")
@Feature("Соглашения Spring")
@Tag("architecture")
@DisplayName("Соглашения Spring SSO")
class SpringConventionArchitectureTest {

    @Test
    @DisplayName("HTTP-контроллеры живут в sso.web")
    void controllersStayInWebPackage() {
        step("Убедиться, что @Controller/@RestController только в sso.web", () -> {
            classes().that().areAnnotatedWith(Controller.class)
                    .or().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage(WEB)
                    .because("HTTP-ручки — пакет sso.web")
                    .check(CLASSES);
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage(WEB)
                    .check(CLASSES);
        });
    }

    @Test
    @DisplayName("Сервисы помечены @Service")
    void servicePackageContainsServices() {
        step("Убедиться, что классы *Service в ..service.. с @Service", () ->
                classes().that().resideInAPackage(SERVICE)
                        .and().haveSimpleNameEndingWith("Service")
                        .should().beAnnotatedWith(Service.class)
                        .check(CLASSES));
    }

    @Test
    @DisplayName("Репозитории — интерфейсы")
    void repositoriesAreInterfaces() {
        step("Убедиться, что ..repository.. — интерфейсы", () ->
                classes().that().resideInAPackage(REPOSITORY)
                        .should().beInterfaces()
                        .check(CLASSES));
    }

    @Test
    @DisplayName("Нет @Autowired на поле")
    void noFieldAutowired() {
        step("Убедиться, что зависимости внедряются через конструктор", () ->
                noFields().that().areDeclaredInClassesThat().resideInAPackage(APP + "..")
                        .should().beAnnotatedWith(Autowired.class)
                        .because("внедряй зависимости через конструктор")
                        .check(CLASSES));
    }

    @Test
    @DisplayName("Репозитории без @Repository на кастомном классе-имплементации")
    void noConcreteRepositoryClasses() {
        step("Убедиться, что нет классов с @Repository вне Spring Data", () ->
                classes().that().areAnnotatedWith(Repository.class)
                        .should().resideInAPackage(REPOSITORY)
                        .allowEmptyShould(true)
                        .check(CLASSES));
    }
}
