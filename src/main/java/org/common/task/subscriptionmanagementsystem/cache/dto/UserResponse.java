package org.common.task.subscriptionmanagementsystem.cache.dto;

import java.util.List;
import java.util.Map;

public record UserResponse(
        Map<String, Object> activeSubscription, // Теперь объект, а не список
        List<Map<String, Object>> invoices,
        long totalInvoices
) {}
