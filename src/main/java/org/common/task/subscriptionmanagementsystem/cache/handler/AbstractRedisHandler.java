package org.common.task.subscriptionmanagementsystem.cache.handler;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.task.subscriptionmanagementsystem.cache.service.RedisCacheService;
import org.springframework.dao.DataAccessResourceFailureException;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractRedisHandler implements EventHandler {

    protected static final String SUBSCRIPTION_MAP = "subscriptionMap";
    protected static final String INVOICES_MAP = "invoiceMap";
    protected static final String USER_ID = "userId";

    protected final RedisCacheService redisCacheService;

    @Override
    public void handle(JsonNode root) {
        try {
            process(root);

            log.info("Событие {} успешно обработано для пользователя {}",
                    getSupportedType(), root.get(USER_ID).asText());
        } catch (DataAccessResourceFailureException e) {
            log.error("КРИТИЧЕСКАЯ ОШИБКА: Redis недоступен при обработке {}. Данные в кэш не записаны. Причина: {}",
                    getSupportedType(), e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка бизнес-логики в обработчике {}: {}", getSupportedType(), e.getMessage());
        }
    }

    protected abstract void process(JsonNode root);
}

