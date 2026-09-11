package com.fintechathon.silvershield.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

class OpenApiConfigurationTest {

    private final OpenApiConfiguration configuration = new OpenApiConfiguration();

    @Test
    void declaresOpaqueBearerSessionScheme() {
        OpenAPI api = configuration.silverShieldOpenApi();

        assertThat(api.getInfo().getTitle()).contains("银盾智行");
        assertThat(api.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfiguration.BEARER_SCHEME);
        assertThat(api.getComponents().getSecuritySchemes().get(OpenApiConfiguration.BEARER_SCHEME).getScheme())
                .isEqualTo("bearer");
    }

    @Test
    void protectsBusinessOperationsButLeavesLoginPublic() {
        Operation login = new Operation();
        Operation accounts = new Operation();
        OpenAPI api = configuration.silverShieldOpenApi().paths(new Paths()
                .addPathItem("/api/v1/auth/login", new PathItem().post(login))
                .addPathItem("/api/v1/accounts", new PathItem().get(accounts)));

        configuration.authenticatedOperations().customise(api);

        assertThat(login.getSecurity()).isNull();
        assertThat(accounts.getSecurity()).singleElement()
                .satisfies(requirement -> assertThat(requirement).containsKey(OpenApiConfiguration.BEARER_SCHEME));
    }
}
