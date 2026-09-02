package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.Payee;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PayeeRepository extends JpaRepository<Payee, UUID> {
    List<Payee> findAllByUserIdOrderByName(UUID userId);
    Optional<Payee> findByIdAndUserId(UUID id, UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payee p where p.id=:id and p.userId=:userId")
    Optional<Payee> lockOwned(@Param("id") UUID id, @Param("userId") UUID userId);
}
