package org.common.task.subscriptionmanagementsystem.subscription.dto;

import jakarta.validation.constraints.NotNull;
import org.common.task.subscriptionmanagementsystem.subscription.model.User;

public record DeactivationRequest(
        @NotNull User user
) {}
