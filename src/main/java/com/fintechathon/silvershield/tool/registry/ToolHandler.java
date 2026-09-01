package com.fintechathon.silvershield.tool.registry;

import com.fintechathon.silvershield.tool.model.ToolArguments;
import com.fintechathon.silvershield.tool.model.ToolDefinition;
import com.fintechathon.silvershield.tool.model.ToolExecutionContext;
import com.fintechathon.silvershield.tool.model.ToolPayload;

public interface ToolHandler<A extends ToolArguments> {

    ToolDefinition definition();

    Class<A> argumentsType();

    ToolPayload execute(ToolExecutionContext context, A arguments);
}
