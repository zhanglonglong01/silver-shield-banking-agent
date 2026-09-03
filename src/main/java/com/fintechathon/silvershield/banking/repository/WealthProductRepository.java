package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.WealthProduct;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WealthProductRepository extends JpaRepository<WealthProduct, UUID> {
    List<WealthProduct> findAllByOrderByRiskLevelAscNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from WealthProduct p where p.id = :id")
    Optional<WealthProduct> lockById(@Param("id") UUID id);
}
