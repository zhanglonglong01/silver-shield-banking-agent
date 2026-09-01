package com.fintechathon.silvershield.tool.capability;

import com.fintechathon.silvershield.tool.model.ToolExecutionContext;
import com.fintechathon.silvershield.tool.model.ToolId;
import java.util.Set;

public interface CapabilityPolicy {

    AllowedToolSetSnapshot issue(ToolExecutionContext context, Set<ToolId> requestedCandidates);

    CapabilityVerification verify(
            AllowedToolSetSnapshot snapshot, ToolExecutionContext context, ToolId requestedTool);
}
