package com.fintechathon.silvershield.tool.registry;

import com.fintechathon.silvershield.tool.model.ToolDefinition;
import com.fintechathon.silvershield.tool.model.ToolId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExplicitToolRegistry {

    private final Map<ToolId, ToolHandler<?>> handlers;

    public ExplicitToolRegistry(List<ToolHandler<?>> explicitlyApprovedHandlers) {
        if (explicitlyApprovedHandlers == null || explicitlyApprovedHandlers.isEmpty()) {
            throw new IllegalArgumentException("Explicit tool registry must not be empty");
        }
        Map<ToolId, ToolHandler<?>> registered = new LinkedHashMap<>();
        for (ToolHandler<?> handler : explicitlyApprovedHandlers) {
            ToolId id = handler.definition().id();
            if (registered.putIfAbsent(id, handler) != null) {
                throw new IllegalArgumentException("Duplicate tool registration: " + id);
            }
        }
        this.handlers = Map.copyOf(registered);
    }

    public Optional<ToolHandler<?>> resolve(ToolId id) {
        return Optional.ofNullable(handlers.get(id));
    }

    public Optional<ToolDefinition> definition(ToolId id) {
        return resolve(id).map(ToolHandler::definition);
    }

    public Collection<ToolDefinition> definitions() {
        return handlers.values().stream().map(ToolHandler::definition).toList();
    }
}
