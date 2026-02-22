package org.common.task.subscriptionmanagementsystem.cache.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.common.task.subscriptionmanagementsystem.cache.dto.UserResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class RedisCacheServiceBean implements RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    protected static final String SUBSCRIPTION_KEY = "user:%s:subscription";
    protected static final String INVOICES_KEY = "user:%s:invoices";
    protected static final String SUB_FIELD = "subscription";

    public RedisCacheServiceBean(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Получение данных из кэша (Cache-Aside чтение)
     */
    public UserResponse getUserFromCache(UUID userId, int page, int size) {
        String subscriptionKey = String.format(SUBSCRIPTION_KEY, userId);
        String invKey = String.format(INVOICES_KEY, userId);

        try {
            String subJson = (String) redisTemplate.opsForHash().get(subscriptionKey, SUB_FIELD);
            Map<String, Object> activeSub = null;
            if (subJson != null) {
                activeSub = objectMapper.readValue(subJson, new TypeReference<>() {});
            }

            int start = page * size;
            int end = start + size - 1;

            Set<Object> range = redisTemplate.opsForZSet().reverseRange(invKey, start, end);
            Long total = redisTemplate.opsForZSet().zCard(invKey);

            List<Map<String, Object>> invoices = new ArrayList<>();
            if (range != null) {
                for (Object obj : range) {
                    invoices.add(objectMapper.readValue((String) obj, new TypeReference<>() {}));
                }
            }

            if (subJson == null && invoices.isEmpty()) {
                return null;
            }

            return new UserResponse(activeSub, invoices, total != null ? total : 0);

        } catch (Exception e) {
            log.error("Redis read error for user {}: {}. Fallback triggered.", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Фоновое заполнение кэша данными из БД.
     * Выполняется асинхронно, чтобы не блокировать ответ пользователю.
     */
    @Async
    public void populateCacheAsync(UUID userId, UserResponse dbData) {
        try {
            String infoKey = String.format(SUBSCRIPTION_KEY, userId);
            String invKey = String.format(INVOICES_KEY, userId);

            if (dbData.activeSubscription() != null) {
                redisTemplate.opsForHash().put(infoKey, SUB_FIELD,
                        objectMapper.writeValueAsString(dbData.activeSubscription()));
            }

            if (!dbData.invoices().isEmpty()) {
                redisTemplate.delete(invKey);

                Set<ZSetOperations.TypedTuple<Object>> tuples = new HashSet<>();
                for (Map<String, Object> invoice : dbData.invoices()) {
                    String json = objectMapper.writeValueAsString(invoice);

                    double score = System.currentTimeMillis();
                    tuples.add(ZSetOperations.TypedTuple.of((Object) json, score));
                }

                redisTemplate.opsForZSet().add(invKey, tuples);
            }
            log.info("Cache healed for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to heal cache for user {}: Redis is still down.", userId);
        }
    }

    /**
     * Сохранение JSON подписки при получении события из RabbitMQ
     */
    public void saveSubscriptionToCache(String userId, String subJson) {
        try {
            String key = String.format(SUBSCRIPTION_KEY, userId);
            redisTemplate.opsForHash().put(key, SUB_FIELD, subJson);
        } catch (Exception e) {
            log.error("Redis error during saveSubscription: {}", e.getMessage());
        }
    }

    /**
     * Добавление нового счета в историю (Sorted Set)
     */
    public void addInvoiceToCache(String userId, String invJson) {
        try {
            String key = String.format(INVOICES_KEY, userId);
            redisTemplate.opsForZSet().add(key, invJson, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("Redis error during addInvoice: {}", e.getMessage());
        }
    }

    /**
     * Удаление активной подписки (используется при деактивации)
     */
    public void deleteSubscriptionFromCache(String userId) {
        try {
            String key = String.format(SUBSCRIPTION_KEY, userId);
            redisTemplate.opsForHash().delete(key, SUB_FIELD);
        } catch (Exception e) {
            log.error("Redis error during deleteSubscription: {}", e.getMessage());
        }
    }
}
