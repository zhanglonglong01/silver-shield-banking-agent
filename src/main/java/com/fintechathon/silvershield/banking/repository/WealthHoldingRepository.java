package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.WealthHolding;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WealthHoldingRepository extends JpaRepository<WealthHolding, UUID> {
    List<WealthHolding> findAllByUserIdOrderByOpenedAtDesc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from WealthHolding h where h.userId = :userId and h.productId = :productId")
    Optional<WealthHolding> lockOwnedProduct(@Param("userId") UUID userId, @Param("productId") UUID productId);
}
