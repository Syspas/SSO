package com.example.sso.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Один раз импортирует production-классы {@code com.example.sso}.
 * Тесты и JAR библиотек не входят.
 */
final class ProductionClasses {

    static final String APP = "com.example.sso";

    static final String WEB = APP + ".sso.web..";
    static final String SERVICE = APP + "..service..";
    static final String REPOSITORY = APP + "..repository..";
    static final String ENTITY = APP + "..entity..";
    static final String DTO = APP + "..dto..";
    static final String MAPPER = APP + "..mapper..";
    static final String CONFIG = APP + ".config..";
    static final String CLIENT = APP + ".client..";
    static final String EXCEPTION = APP + ".exception..";

    static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages(APP);

    private ProductionClasses() {
    }
}
