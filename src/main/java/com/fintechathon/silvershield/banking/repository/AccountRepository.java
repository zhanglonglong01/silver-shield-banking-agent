package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findAllByUserIdOrderByAlias(UUID userId);
    Optional<Account> findByIdAndUserId(UUID id, UUID userId);
}
