package org.common.task.subscriptionmanagementsystem.cache.handler;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.common.task.subscriptionmanagementsystem.cache.service.RedisCacheService;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ActivationHandler extends AbstractRedisHandler {

    public ActivationHandler(RedisCacheService redisCacheService) {
        super(redisCacheService);
    }

    @Override
    public EventType getSupportedType() {
        return EventType.SUBSCRIPTION_ACTIVATED;
    }

    @Override
    protected void process(JsonNode root) {
        String userId = root.get(USER_ID).asText();

        redisCacheService.saveSubscriptionToCache(userId, root.get(SUBSCRIPTION_MAP).toString());
        
        if (root.has(INVOICES_MAP)) {
            redisCacheService.addInvoiceToCache(userId, root.get(INVOICES_MAP).toString());
        }
    }
}
