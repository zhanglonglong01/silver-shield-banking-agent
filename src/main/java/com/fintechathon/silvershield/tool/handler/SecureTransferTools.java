package com.fintechathon.silvershield.tool.handler;

import com.fintechathon.silvershield.operation.application.TransferOperationUseCase;
import com.fintechathon.silvershield.operation.application.TransferOperationUseCase.*;
import com.fintechathon.silvershield.tool.gateway.ToolDeniedException;
import com.fintechathon.silvershield.tool.model.*;
import com.fintechathon.silvershield.tool.registry.ToolHandler;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public final class SecureTransferTools {
    private SecureTransferTools() {}

    public record PrepareTransferArguments(
            @NotNull UUID sourceAccountId,
            @NotNull UUID payeeId,
            @NotNull @DecimalMin(value="0.01") @Digits(integer=15,fraction=2) BigDecimal amount,
            @NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
            @Size(max=120) String purpose) implements ToolArguments {}
    public record OperationArguments(@NotNull UUID operationId) implements ToolArguments {}

    public static final class PrepareTransferTool implements ToolHandler<PrepareTransferArguments> {
        private final TransferOperationUseCase operations;
        public PrepareTransferTool(TransferOperationUseCase operations){this.operations=operations;}
        @Override public ToolDefinition definition(){return new ToolDefinition(new ToolId("prepare_transfer","1.0"),
                "Validate a transfer and create a confirmation-bound operation without moving funds",
                ToolCapability.TRANSFER_PREPARE,ToolAccessMode.PREPARE_WRITE,"m4-v1",
                Set.of("sourceAccountId","payeeId","amount","currency","purpose"),true);}
        @Override public Class<PrepareTransferArguments> argumentsType(){return PrepareTransferArguments.class;}
        @Override public ToolPayload execute(ToolExecutionContext context,PrepareTransferArguments args){
            PreparationResult result=operations.prepare(new PreparationContext(context.requestId(),context.conversationId(),
                    context.userId(),context.idempotencyKey()),new TransferCommand(args.sourceAccountId(),args.payeeId(),args.amount(),args.currency(),args.purpose()));
            if(!result.accepted()) throw new ToolDeniedException(result.reasonCode(),result.explanation());
            return new ToolPayload(result.operation(),1);
        }
    }

    public static final class GetOperationStatusTool implements ToolHandler<OperationArguments> {
        private final TransferOperationUseCase operations;
        public GetOperationStatusTool(TransferOperationUseCase operations){this.operations=operations;}
        @Override public ToolDefinition definition(){return new ToolDefinition(new ToolId("get_operation_status","1.0"),
                "Read one authenticated customer's transfer operation status and receipt reference",
                ToolCapability.OPERATION_READ,ToolAccessMode.READ_ONLY,"m4-v1",Set.of("operationId"),false);}
        @Override public Class<OperationArguments> argumentsType(){return OperationArguments.class;}
        @Override public ToolPayload execute(ToolExecutionContext context,OperationArguments args){return new ToolPayload(operations.get(context.userId(),args.operationId()),1);}
    }
}
