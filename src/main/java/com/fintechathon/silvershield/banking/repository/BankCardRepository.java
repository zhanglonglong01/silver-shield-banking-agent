package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.BankCard;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCardRepository extends JpaRepository<BankCard, UUID> {
    List<BankCard> findAllByUserIdOrderByCardMasked(UUID userId);
}
