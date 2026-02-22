package org.common.task.subscriptionmanagementsystem.subscription.repository;

import org.common.task.subscriptionmanagementsystem.subscription.model.Subscription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserIdAndActiveTrue(UUID userId);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.subscriptionType WHERE s.active = true AND s.nextBillingDate = :date")
    Slice<Subscription> findAllByActiveTrueAndNextBillingDate(@Param("date") LocalDate nextBillingDate, Pageable pageable);
}
