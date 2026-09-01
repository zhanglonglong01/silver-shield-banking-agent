package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.Subscription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findAllByUserIdOrderByMerchantName(UUID userId);
}
