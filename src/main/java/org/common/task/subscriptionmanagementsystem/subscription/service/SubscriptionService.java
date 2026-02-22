package org.common.task.subscriptionmanagementsystem.subscription.service;

import org.common.task.subscriptionmanagementsystem.subscription.model.SubscriptionEnumType;

import java.time.LocalDate;
import java.util.UUID;

public interface SubscriptionService {

    void activate(UUID userId, SubscriptionEnumType typeName, LocalDate activationDate);

    void deactivate(UUID userId);
}
