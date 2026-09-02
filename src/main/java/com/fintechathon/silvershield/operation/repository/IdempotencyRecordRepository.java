package com.fintechathon.silvershield.operation.repository;
import com.fintechathon.silvershield.operation.domain.IdempotencyRecord;
import java.util.Optional; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord,UUID> {
    Optional<IdempotencyRecord> findByUserIdAndScopeAndIdempotencyKeyHash(UUID userId,String scope,String hash);
    Optional<IdempotencyRecord> findByOperationId(UUID operationId);
}
