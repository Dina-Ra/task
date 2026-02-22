package org.common.task.subscriptionmanagementsystem.subscription.repository;

import org.common.task.subscriptionmanagementsystem.subscription.model.SubscriptionEnumType;
import org.common.task.subscriptionmanagementsystem.subscription.model.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionTypeRepository extends JpaRepository<SubscriptionType, UUID> {

    Optional<SubscriptionType> findByTypeName(SubscriptionEnumType typeName);
}
