package com.fintechathon.silvershield.tool.schema;

import com.fintechathon.silvershield.tool.capability.AllowedToolSetSnapshot;
import java.util.List;

public interface ToolSchemaCatalog {

    List<AgentToolSchema> schemasFor(AllowedToolSetSnapshot snapshot);
}
