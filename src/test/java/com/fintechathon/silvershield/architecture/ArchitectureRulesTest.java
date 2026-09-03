package com.fintechathon.silvershield.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.fintechathon.silvershield",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule agent_must_not_depend_on_repositories = noClasses()
            .that().resideInAnyPackage("..agent..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule tools_must_not_depend_on_repositories = noClasses()
            .that().resideInAnyPackage("..tool..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule agent_must_not_depend_on_tool_handlers = noClasses()
            .that().resideInAnyPackage("..agent..")
            .should().dependOnClassesThat().resideInAnyPackage("..tool.handler..", "..tool.registry..")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule only_gateway_registry_handlers_and_configuration_may_reference_tool_handlers = noClasses()
            .that().resideOutsideOfPackages(
                    "..tool.gateway..", "..tool.registry..", "..tool.handler..", "..tool.schema..", "..config..")
            .should().dependOnClassesThat().areAssignableTo(
                    com.fintechathon.silvershield.tool.registry.ToolHandler.class)
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule agent_must_not_use_the_direct_tool_runtime_service = noClasses()
            .that().resideInAnyPackage("..agent..")
            .should().dependOnClassesThat().haveSimpleName("ToolRuntimeService")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule read_only_agent_must_depend_on_secure_tool_gateway = classes()
            .that().haveSimpleName("ReadOnlyBankingAgent")
            .should().dependOnClassesThat().haveSimpleName("SecureToolGateway")
            .allowEmptyShould(false);

    @ArchTest
    static void architectureRulesMustScanConcreteM2GatewayAndToolClasses(JavaClasses classes) {
        assertThat(classes.stream().map(javaClass -> javaClass.getName()).toList())
                .contains(
                        "com.fintechathon.silvershield.tool.gateway.SecureToolGateway",
                        "com.fintechathon.silvershield.agent.application.ReadOnlyBankingAgent",
                        "com.fintechathon.silvershield.agent.provider.SpringAiBankingModelClient",
                        "com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools$GetAccountsTool",
                        "com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools$ListWealthProductsTool",
                        "com.fintechathon.silvershield.tool.handler.SecureTransferTools$PrepareTransferTool",
                        "com.fintechathon.silvershield.operation.application.SecureTransferOperationService",
                        "com.fintechathon.silvershield.tool.handler.SecureManagedOperationTools$PrepareWealthPurchaseTool",
                        "com.fintechathon.silvershield.operation.application.SecureManagedOperationService",
                        "com.fintechathon.silvershield.operation.application.SecureOperationCommandRouter",
                        "com.fintechathon.silvershield.agent.planning.AgentOrchestrationService",
                        "com.fintechathon.silvershield.agent.planning.TaskPlanValidator");
    }
}
