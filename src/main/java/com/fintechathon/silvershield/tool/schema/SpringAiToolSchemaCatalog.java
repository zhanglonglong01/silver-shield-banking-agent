package com.fintechathon.silvershield.tool.schema;

import com.fintechathon.silvershield.tool.capability.AllowedToolSetSnapshot;
import com.fintechathon.silvershield.tool.registry.ExplicitToolRegistry;
import com.fintechathon.silvershield.tool.registry.ToolHandler;
import java.util.List;
import java.util.Set;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.stereotype.Service;

@Service
public class SpringAiToolSchemaCatalog implements ToolSchemaCatalog {

    private static final Set<String> FORBIDDEN_SECURITY_ARGUMENTS = Set.of(
            "userId", "role", "capability", "authenticationLevel", "assuranceLevel", "allowedTools", "snapshot");

    private final ExplicitToolRegistry registry;

    public SpringAiToolSchemaCatalog(ExplicitToolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<AgentToolSchema> schemasFor(AllowedToolSetSnapshot snapshot) {
        return snapshot.allowedTools().stream().map(allowed -> {
            ToolHandler<?> handler = registry.resolve(allowed.id())
                    .orElseThrow(() -> new IllegalStateException("Allowed tool is absent from the explicit registry"));
            if (!allowed.descriptorDigest().equals(handler.definition().descriptorDigest())) {
                throw new IllegalStateException("Allowed tool definition changed after policy evaluation");
            }
            if (handler.definition().acceptedArguments().stream().anyMatch(FORBIDDEN_SECURITY_ARGUMENTS::contains)) {
                throw new IllegalStateException("Tool definition exposes server-owned security context");
            }
            String jsonSchema = JsonSchemaGenerator.generateForType(handler.argumentsType());
            return new AgentToolSchema(
                    allowed.id(), allowed.description(), allowed.inputSchemaVersion(), jsonSchema,
                    allowed.descriptorDigest());
        }).toList();
    }
}
