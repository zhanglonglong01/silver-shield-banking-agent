package com.fintechathon.silvershield.banking.application;

import com.fintechathon.silvershield.banking.domain.Account;
import com.fintechathon.silvershield.banking.domain.AccountTransaction;
import com.fintechathon.silvershield.banking.domain.BankCard;
import com.fintechathon.silvershield.banking.domain.Payee;
import com.fintechathon.silvershield.banking.domain.Subscription;
import com.fintechathon.silvershield.banking.domain.WealthProduct;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BankingReadUseCase {

    List<Account> accounts(UUID userId);

    Account account(UUID userId, UUID accountId);

    List<AccountTransaction> transactions(
            UUID userId, UUID accountId, Instant fromInclusive, Instant toExclusive, int limit);

    List<BankCard> cards(UUID userId);

    List<Payee> payees(UUID userId);

    List<Subscription> subscriptions(UUID userId);

    List<WealthProduct> wealthProducts();
}
