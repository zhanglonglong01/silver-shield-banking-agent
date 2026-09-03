package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.Subscription;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findAllByUserIdOrderByMerchantName(UUID userId);
    Optional<Subscription> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.id = :id and s.userId = :userId")
    Optional<Subscription> lockOwned(@Param("id") UUID id, @Param("userId") UUID userId);
}
