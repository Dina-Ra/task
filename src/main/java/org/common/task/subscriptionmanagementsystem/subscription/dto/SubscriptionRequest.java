package org.common.task.subscriptionmanagementsystem.subscription.dto;

import jakarta.validation.constraints.NotNull;
import org.common.task.subscriptionmanagementsystem.subscription.model.SubscriptionEnumType;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionRequest(
        @NotNull UUID userId,
        @NotNull SubscriptionEnumType type,
        @NotNull LocalDate activationDate
) {}
