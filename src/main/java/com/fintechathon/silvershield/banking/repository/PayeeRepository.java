package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.Payee;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayeeRepository extends JpaRepository<Payee, UUID> {
    List<Payee> findAllByUserIdOrderByName(UUID userId);
}
