package org.common.task.subscriptionmanagementsystem.cache.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.common.task.subscriptionmanagementsystem.cache.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceBeanTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private RedisCacheServiceBean redisCacheService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        // Инициализируем сервис вручную, передавая моки
        redisCacheService = new RedisCacheServiceBean(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("Должен возвращать данные из кэша, если они существуют")
    void getUserFromCache_Success() throws JsonProcessingException {
        // GIVEN
        String subJson = "{\"subscriptionId\":\"abc\"}";
        String invJson = "{\"id\":\"inv1\"}";

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        when(hashOperations.get(anyString(), anyString())).thenReturn(subJson);
        when(zSetOperations.reverseRange(anyString(), anyLong(), anyLong()))
                .thenReturn(Collections.singleton(invJson));
        when(zSetOperations.zCard(anyString())).thenReturn(1L);

        // WHEN
        UserResponse response = redisCacheService.getUserFromCache(userId, 0, 10);

        // THEN
        assertNotNull(response);
        assertNotNull(response.activeSubscription());
        assertEquals(1, response.invoices().size());
        assertEquals(1L, response.totalInvoices());
    }

    @Test
    @DisplayName("Должен вернуть null и залогировать ошибку, если Redis недоступен")
    void getUserFromCache_RedisDown_ReturnsNull() {
        // GIVEN
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("Redis connection failed"));

        // WHEN
        UserResponse response = redisCacheService.getUserFromCache(userId, 0, 10);

        // THEN
        assertNull(response); // Сработает Fallback в контроллере
    }

    @Test
    @DisplayName("Проверка асинхронного наполнения кэша (Self-healing)")
    void populateCacheAsync_Success() throws JsonProcessingException {
        // GIVEN
        Map<String, Object> subMap = Map.of("id", "sub1");
        List<Map<String, Object>> invoices = List.of(Map.of("id", "inv1"));
        UserResponse dbData = new UserResponse(subMap, invoices, 1L);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // WHEN
        redisCacheService.populateCacheAsync(userId, dbData);

        // THEN
        verify(hashOperations).put(anyString(), anyString(), anyString());
        verify(redisTemplate).delete(anyString()); // Проверка очистки старых счетов
        verify(zSetOperations).add(anyString(), anySet());
    }

    @Test
    @DisplayName("Удаление подписки при деактивации")
    void deleteSubscriptionFromCache_Success() {
        // GIVEN
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // WHEN
        redisCacheService.deleteSubscriptionFromCache(userId.toString());

        // THEN
        verify(hashOperations).delete(anyString(), anyString());
    }
}
