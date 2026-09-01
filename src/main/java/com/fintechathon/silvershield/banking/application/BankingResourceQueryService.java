package com.fintechathon.silvershield.banking.application;

import com.fintechathon.silvershield.banking.domain.Account;
import com.fintechathon.silvershield.banking.domain.AccountTransaction;
import com.fintechathon.silvershield.banking.domain.BankCard;
import com.fintechathon.silvershield.banking.domain.Payee;
import com.fintechathon.silvershield.banking.domain.Subscription;
import com.fintechathon.silvershield.banking.domain.WealthProduct;
import com.fintechathon.silvershield.banking.repository.AccountRepository;
import com.fintechathon.silvershield.banking.repository.AccountTransactionRepository;
import com.fintechathon.silvershield.banking.repository.BankCardRepository;
import com.fintechathon.silvershield.banking.repository.PayeeRepository;
import com.fintechathon.silvershield.banking.repository.SubscriptionRepository;
import com.fintechathon.silvershield.banking.repository.WealthProductRepository;
import com.fintechathon.silvershield.common.api.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BankingResourceQueryService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final BankCardRepository cardRepository;
    private final PayeeRepository payeeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WealthProductRepository wealthProductRepository;

    public BankingResourceQueryService(
            AccountRepository accountRepository,
            AccountTransactionRepository transactionRepository,
            BankCardRepository cardRepository,
            PayeeRepository payeeRepository,
            SubscriptionRepository subscriptionRepository,
            WealthProductRepository wealthProductRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.payeeRepository = payeeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.wealthProductRepository = wealthProductRepository;
    }

    public List<Account> accounts(UUID userId) {
        return accountRepository.findAllByUserIdOrderByAlias(userId);
    }

    public Account account(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId).orElseThrow(ResourceNotFoundException::new);
    }

    public List<AccountTransaction> transactions(
            UUID userId, UUID accountId, Instant fromInclusive, Instant toExclusive, int limit) {
        if (!fromInclusive.isBefore(toExclusive) || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid transaction query range");
        }
        account(userId, accountId);
        return transactionRepository.findByAccountIdAndBookingTimeBetweenOrderByBookingTimeDesc(
                accountId, fromInclusive, toExclusive, PageRequest.of(0, limit));
    }

    public List<BankCard> cards(UUID userId) {
        return cardRepository.findAllByUserIdOrderByCardMasked(userId);
    }

    public List<Payee> payees(UUID userId) {
        return payeeRepository.findAllByUserIdOrderByName(userId);
    }

    public List<Subscription> subscriptions(UUID userId) {
        return subscriptionRepository.findAllByUserIdOrderByMerchantName(userId);
    }

    public List<WealthProduct> wealthProducts() {
        return wealthProductRepository.findAllByOrderByRiskLevelAscNameAsc();
    }
}
