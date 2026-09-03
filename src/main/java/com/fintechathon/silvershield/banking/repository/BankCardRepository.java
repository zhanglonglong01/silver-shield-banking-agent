package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.BankCard;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCardRepository extends JpaRepository<BankCard, UUID> {
    List<BankCard> findAllByUserIdOrderByCardMasked(UUID userId);

    Optional<BankCard> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BankCard c where c.id = :id and c.userId = :userId")
    Optional<BankCard> lockOwned(@Param("id") UUID id, @Param("userId") UUID userId);
}
