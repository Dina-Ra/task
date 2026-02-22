package org.common.task.subscriptionmanagementsystem.cache.controller;

import org.common.task.subscriptionmanagementsystem.cache.dto.UserResponse;
import org.common.task.subscriptionmanagementsystem.cache.service.RedisCacheService;
import org.common.task.subscriptionmanagementsystem.subscription.repository.InvoiceRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCacheControllerTest {

    @Mock private RedisCacheService redisCacheService;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private InvoiceRepository invoiceRepository;

    @InjectMocks
    private UserCacheController userCacheController;

    @Test
    @DisplayName("Должен вернуть данные из Redis, если они там есть (Cache Hit)")
    void getUserInfo_FromCache() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        UserResponse expectedResponse = new UserResponse(null, List.of(), 0L);
        when(redisCacheService.getUserFromCache(eq(userId), anyInt(), anyInt()))
                .thenReturn(expectedResponse);

        // WHEN
        ResponseEntity<UserResponse> response = userCacheController.getUserInfo(userId, 0, 10);

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        // Проверяем, что в БД НЕ ходили
        verifyNoInteractions(subscriptionRepository, invoiceRepository);
    }

    @Test
    @DisplayName("Должен пойти в БД, если Redis недоступен (Fallback)")
    void getUserInfo_FromDatabaseOnRedisError() {
        // GIVEN
        UUID userId = UUID.randomUUID();

        // Имитируем падение Redis
        when(redisCacheService.getUserFromCache(any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Redis connection error"));

        // Имитируем успешный ответ из БД (пустой результат)
        when(subscriptionRepository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.empty());
        when(invoiceRepository.findAllBySubscriptionUserId(eq(userId), any())).thenReturn(Page.empty());

        // WHEN
        ResponseEntity<UserResponse> response = userCacheController.getUserInfo(userId, 0, 10);

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Проверяем, что обращения к БД БЫЛИ
        verify(subscriptionRepository).findByUserIdAndActiveTrue(userId);
        verify(invoiceRepository).findAllBySubscriptionUserId(eq(userId), any());

        // Проверяем, что запустилось асинхронное лечение кэша
        verify(redisCacheService).populateCacheAsync(eq(userId), any());
    }
}
