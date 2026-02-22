package org.common.task.subscriptionmanagementsystem.cache.controller;

import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.task.subscriptionmanagementsystem.cache.dto.UserResponse;
import org.common.task.subscriptionmanagementsystem.cache.service.RedisCacheService;
import org.common.task.subscriptionmanagementsystem.subscription.model.Invoice;
import org.common.task.subscriptionmanagementsystem.subscription.repository.InvoiceRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserCacheController {

    private final RedisCacheService redisCacheService;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Получает сводную информацию о пользователе: статус подписки и историю счетов.
     * Реализует паттерн Cache-Aside: Redis (основной) -> PostgreSQL (fallback).
     */
    @GetMapping("/{userId}/info")
    public ResponseEntity<UserResponse> getUserInfo(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") @Max(100) int size) {

        UserResponse cachedData = null;

        try {
            cachedData = redisCacheService.getUserFromCache(userId, page, size);
        } catch (Exception e) {
            log.error("Redis is down or error occurred: {}", e.getMessage());
        }

        if (cachedData != null) {
            log.info("Serving data from Redis for user: {}", userId);
            return ResponseEntity.ok(cachedData);
        }

        log.info("Redis miss/down, fetching from PostgreSQL for user: {}", userId);
        UserResponse dbData = getFromDatabase(userId, page, size);

        redisCacheService.populateCacheAsync(userId, dbData);

        return ResponseEntity.ok(dbData);
    }

    /**
     * Прямое чтение из PostgreSQL. Используется как источник истины.
     */
    private UserResponse getFromDatabase(UUID userId, int page, int size) {

        Map<String, Object> activeSubscription = subscriptionRepository.findByUserIdAndActiveTrue(userId)
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("subscriptionId", s.getId());
                    map.put("active", s.isActive());
                    map.put("subscriptionType", s.getSubscriptionType().getTypeName());
                    map.put("nextBillingDate", s.getNextBillingDate());
                    return map;
                })
                .orElse(null);

        Pageable pageable = PageRequest.of(page, size, Sort.by("issuedAt").descending());
        Page<Invoice> invoicePage = invoiceRepository.findAllBySubscriptionUserId(userId, pageable);

        List<Map<String, Object>> invoices = invoicePage.getContent().stream()
                .map(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.getId());
                    map.put("amount", i.getAmount());
                    map.put("issuedAt", i.getIssuedAt());
                    map.put("subscriptionType", i.getSubscription().getSubscriptionType().getTypeName());
                    map.put("subscriptionActivationDate", i.getSubscription().getActivationDate());
                    return map;
                })
                .toList();

        return new UserResponse(activeSubscription, invoices, invoicePage.getTotalElements());
    }
}

