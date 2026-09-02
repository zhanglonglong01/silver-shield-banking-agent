package com.fintechathon.silvershield.config;

import com.fintechathon.silvershield.banking.application.BankingReadUseCase;
import com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools.GetAccountBalanceTool;
import com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools.GetAccountsTool;
import com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools.ListCardsTool;
import com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools.ListPayeesTool;
import com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools.ListSubscriptionsTool;
import com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools.ListTransactionsTool;
import com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools.ListWealthProductsTool;
import com.fintechathon.silvershield.tool.registry.ExplicitToolRegistry;
import java.util.List;
import com.fintechathon.silvershield.operation.application.TransferOperationUseCase;
import com.fintechathon.silvershield.tool.handler.SecureTransferTools.PrepareTransferTool;
import com.fintechathon.silvershield.tool.handler.SecureTransferTools.GetOperationStatusTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReadOnlyToolConfiguration {

    @Bean GetAccountsTool getAccountsTool(BankingReadUseCase banking) { return new GetAccountsTool(banking); }
    @Bean GetAccountBalanceTool getAccountBalanceTool(BankingReadUseCase banking) {
        return new GetAccountBalanceTool(banking);
    }
    @Bean ListTransactionsTool listTransactionsTool(BankingReadUseCase banking) {
        return new ListTransactionsTool(banking);
    }
    @Bean ListCardsTool listCardsTool(BankingReadUseCase banking) { return new ListCardsTool(banking); }
    @Bean ListPayeesTool listPayeesTool(BankingReadUseCase banking) { return new ListPayeesTool(banking); }
    @Bean ListSubscriptionsTool listSubscriptionsTool(BankingReadUseCase banking) {
        return new ListSubscriptionsTool(banking);
    }
    @Bean ListWealthProductsTool listWealthProductsTool(BankingReadUseCase banking) {
        return new ListWealthProductsTool(banking);
    }
    @Bean PrepareTransferTool prepareTransferTool(TransferOperationUseCase operations) { return new PrepareTransferTool(operations); }
    @Bean GetOperationStatusTool getOperationStatusTool(TransferOperationUseCase operations) { return new GetOperationStatusTool(operations); }

    @Bean
    ExplicitToolRegistry explicitToolRegistry(
            GetAccountsTool getAccounts,
            GetAccountBalanceTool getAccountBalance,
            ListTransactionsTool listTransactions,
            ListCardsTool listCards,
            ListPayeesTool listPayees,
            ListSubscriptionsTool listSubscriptions,
            ListWealthProductsTool listWealthProducts,
            PrepareTransferTool prepareTransfer,
            GetOperationStatusTool getOperationStatus) {
        return new ExplicitToolRegistry(List.of(
                getAccounts, getAccountBalance, listTransactions, listCards,
                listPayees, listSubscriptions, listWealthProducts, prepareTransfer, getOperationStatus));
    }
}
