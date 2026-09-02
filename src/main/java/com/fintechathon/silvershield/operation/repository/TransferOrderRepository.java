package com.fintechathon.silvershield.operation.repository;
import com.fintechathon.silvershield.operation.domain.TransferOrder;
import java.time.Instant; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface TransferOrderRepository extends JpaRepository<TransferOrder,UUID> {
    Optional<TransferOrder> findByOperationId(UUID operationId);
    @Query(value="select count(*) from transfer_order where user_id=:user and created_at>=:from and status='SUCCEEDED'",nativeQuery=true)
    long countRecent(@Param("user") UUID userId,@Param("from") Instant from);
    @Query(value="select coalesce(sum(amount),0) from transfer_order where user_id=:user and created_at>=:from and status='SUCCEEDED'",nativeQuery=true)
    java.math.BigDecimal sumRecent(@Param("user") UUID userId,@Param("from") Instant from);
    @Query(value="select coalesce(sum(amount),0) from transfer_order where source_account_id=:account and created_at>=:from and status='SUCCEEDED'",nativeQuery=true)
    java.math.BigDecimal sumSince(@Param("account") UUID accountId,@Param("from") Instant from);
}
