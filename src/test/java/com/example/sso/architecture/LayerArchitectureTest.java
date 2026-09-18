package com.example.sso.architecture;

import com.tngtech.archunit.library.Architectures;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.example.sso.architecture.ProductionClasses.APP;
import static com.example.sso.architecture.ProductionClasses.CLASSES;
import static com.example.sso.architecture.ProductionClasses.CLIENT;
import static com.example.sso.architecture.ProductionClasses.CONFIG;
import static com.example.sso.architecture.ProductionClasses.DTO;
import static com.example.sso.architecture.ProductionClasses.ENTITY;
import static com.example.sso.architecture.ProductionClasses.EXCEPTION;
import static com.example.sso.architecture.ProductionClasses.MAPPER;
import static com.example.sso.architecture.ProductionClasses.REPOSITORY;
import static com.example.sso.architecture.ProductionClasses.SERVICE;
import static com.example.sso.architecture.ProductionClasses.WEB;
import static com.example.sso.support.Steps.step;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Epic("Архитектура")
@Feature("Слои")
@Tag("architecture")
@DisplayName("Слои SSO")
class LayerArchitectureTest {

    @Test
    @DisplayName("Зависимости слоёв идут только внутрь")
    void layersDependOnlyInward() {
        step("Проверить layeredArchitecture: web → service → repository", () ->
                Architectures.layeredArchitecture()
                        .consideringOnlyDependenciesInAnyPackage(APP + "..")
                        .layer("Application").definedBy(APP)
                        .layer("Web").definedBy(WEB)
                        .layer("Services").definedBy(SERVICE)
                        .layer("Mappers").definedBy(MAPPER)
                        .layer("Repositories").definedBy(REPOSITORY)
                        .layer("Entities").definedBy(ENTITY)
                        .layer("Dto").definedBy(DTO)
                        .layer("Config").definedBy(CONFIG)
                        .layer("Client").definedBy(CLIENT)
                        .layer("Exception").definedBy(EXCEPTION)
                        .whereLayer("Application").mayNotBeAccessedByAnyLayer()
                        .whereLayer("Application").mayOnlyAccessLayers("Config")
                        .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                        .whereLayer("Services").mayOnlyBeAccessedByLayers("Web", "Config")
                        .whereLayer("Mappers").mayOnlyBeAccessedByLayers("Web", "Services")
                        .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services", "Config")
                        .whereLayer("Entities").mayOnlyBeAccessedByLayers(
                                "Web", "Services", "Mappers", "Repositories", "Config", "Dto")
                        .whereLayer("Dto").mayOnlyBeAccessedByLayers("Web", "Services", "Mappers")
                        .whereLayer("Config").mayOnlyBeAccessedByLayers(
                                "Web", "Application", "Services", "Client")
                        .whereLayer("Client").mayOnlyBeAccessedByLayers("Services", "Config")
                        .whereLayer("Exception").mayOnlyBeAccessedByLayers(
                                "Web", "Services", "Config", "Client")
                        .because("контроллер спрашивает сервис, репозиторий — только CRUD")
                        .check(CLASSES));
    }

    @Test
    @DisplayName("Контроллеры не зависят от репозиториев")
    void controllersDoNotDependOnRepositories() {
        step("Убедиться, что web не импортирует repository", () ->
                noClasses().that().resideInAPackage(WEB)
                        .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
                        .because("контроллер спрашивает сервис, а не чужие таблицы")
                        .check(CLASSES));
    }

    @Test
    @DisplayName("Сервисы не зависят от контроллеров")
    void servicesDoNotDependOnControllers() {
        step("Убедиться, что service не импортирует web", () ->
                noClasses().that().resideInAPackage(SERVICE)
                        .should().dependOnClassesThat().resideInAPackage(WEB)
                        .because("бизнес-логика не должна знать HTTP-ручки")
                        .check(CLASSES));
    }

    @Test
    @DisplayName("Репозитории не зависят от верхних слоёв")
    void repositoriesDoNotDependOnUpperLayers() {
        step("Убедиться, что repository не импортирует service, web, config", () ->
                noClasses().that().resideInAPackage(REPOSITORY)
                        .should().dependOnClassesThat().resideInAnyPackage(SERVICE, WEB, CONFIG, CLIENT)
                        .because("репозиторий — только CRUD")
                        .check(CLASSES));
    }

    @Test
    @DisplayName("Сущности не зависят от верхних слоёв")
    void entitiesDoNotDependOnUpperLayers() {
        step("Убедиться, что entity не импортирует web, service, repository, config", () ->
                noClasses().that().resideInAPackage(ENTITY)
                        .should().dependOnClassesThat().resideInAnyPackage(
                                WEB, SERVICE, REPOSITORY, CONFIG, CLIENT)
                        .because("таблица в Java не должна знать про вход")
                        .check(CLASSES));
    }

    @Test
    @DisplayName("DTO не зависят от сервисов и репозиториев")
    void dtoDoNotDependOnServicesOrRepositories() {
        step("Убедиться, что dto не импортирует service и repository", () ->
                noClasses().that().resideInAPackage(DTO)
                        .should().dependOnClassesThat().resideInAnyPackage(SERVICE, REPOSITORY, WEB)
                        .because("DTO — контракт API")
                        .check(CLASSES));
    }
}
