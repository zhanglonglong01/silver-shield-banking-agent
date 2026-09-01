package com.fintechathon.silvershield.banking.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fintechathon.silvershield.banking.repository.AccountRepository;
import com.fintechathon.silvershield.banking.repository.AccountTransactionRepository;
import com.fintechathon.silvershield.banking.repository.BankCardRepository;
import com.fintechathon.silvershield.banking.repository.PayeeRepository;
import com.fintechathon.silvershield.banking.repository.SubscriptionRepository;
import com.fintechathon.silvershield.banking.repository.WealthProductRepository;
import com.fintechathon.silvershield.common.api.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BankingResourceQueryServiceTest {

    @Test
    void accountLookupMustAlwaysIncludeAuthenticatedOwnerScope() {
        AccountRepository accounts = mock(AccountRepository.class);
        AccountTransactionRepository transactions = mock(AccountTransactionRepository.class);
        UUID alice = UUID.randomUUID();
        UUID bobAccount = UUID.randomUUID();
        when(accounts.findByIdAndUserId(bobAccount, alice)).thenReturn(Optional.empty());
        BankingResourceQueryService service = service(accounts, transactions);

        assertThatThrownBy(() -> service.account(alice, bobAccount)).isInstanceOf(ResourceNotFoundException.class);

        verify(accounts).findByIdAndUserId(bobAccount, alice);
        verifyNoInteractions(transactions);
    }

    private static BankingResourceQueryService service(
            AccountRepository accounts, AccountTransactionRepository transactions) {
        return new BankingResourceQueryService(
                accounts,
                transactions,
                mock(BankCardRepository.class),
                mock(PayeeRepository.class),
                mock(SubscriptionRepository.class),
                mock(WealthProductRepository.class));
    }
}
