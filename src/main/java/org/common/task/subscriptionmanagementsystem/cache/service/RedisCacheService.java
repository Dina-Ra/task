package org.common.task.subscriptionmanagementsystem.cache.service;

import org.common.task.subscriptionmanagementsystem.cache.dto.UserResponse;

import java.util.UUID;

public interface RedisCacheService {
    UserResponse getUserFromCache(UUID userId, int page, int size);

    void populateCacheAsync(UUID userId, UserResponse dbData);

    void saveSubscriptionToCache(String userId, String subJson);

    void addInvoiceToCache(String userId, String invJson);

    void deleteSubscriptionFromCache(String userId);
}
