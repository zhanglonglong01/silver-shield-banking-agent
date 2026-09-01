package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.WealthProduct;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WealthProductRepository extends JpaRepository<WealthProduct, UUID> {
    List<WealthProduct> findAllByOrderByRiskLevelAscNameAsc();
}
