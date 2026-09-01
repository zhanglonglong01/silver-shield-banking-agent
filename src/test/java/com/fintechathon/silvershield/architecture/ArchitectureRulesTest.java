package com.fintechathon.silvershield.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.fintechathon.silvershield")
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule agent_must_not_depend_on_repositories = noClasses()
            .that().resideInAnyPackage("..agent..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");

    @ArchTest
    static final ArchRule tools_must_not_depend_on_repositories = noClasses()
            .that().resideInAnyPackage("..tool..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");
}
