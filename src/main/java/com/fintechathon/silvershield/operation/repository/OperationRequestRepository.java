package com.fintechathon.silvershield.operation.repository;
import com.fintechathon.silvershield.operation.domain.OperationRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface OperationRequestRepository extends JpaRepository<OperationRequest,UUID> {
    Optional<OperationRequest> findByIdAndUserId(UUID id,UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from OperationRequest o where o.id=:id and o.userId=:userId")
    Optional<OperationRequest> lockOwned(@Param("id") UUID id,@Param("userId") UUID userId);
}
