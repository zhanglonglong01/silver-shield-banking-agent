package com.fintechathon.silvershield.operation.repository;
import com.fintechathon.silvershield.operation.domain.ConfirmationChallenge;
import jakarta.persistence.LockModeType; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface ConfirmationChallengeRepository extends JpaRepository<ConfirmationChallenge,UUID> {
    Optional<ConfirmationChallenge> findByOperationId(UUID operationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from ConfirmationChallenge c where c.id=:id and c.operationId=:op and c.userId=:user")
    Optional<ConfirmationChallenge> lockBound(@Param("id") UUID id,@Param("op") UUID op,@Param("user") UUID user);
}
