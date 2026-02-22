package org.common.task.subscriptionmanagementsystem.cache.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.common.task.subscriptionmanagementsystem.cache.service.RedisCacheService;
import org.springframework.stereotype.Component;

@Component
public class DeactivationHandler extends AbstractRedisHandler {

    public DeactivationHandler(RedisCacheService redisCacheService) {
        super(redisCacheService);
    }

    @Override
    public EventType getSupportedType() { return EventType.SUBSCRIPTION_DEACTIVATED; }

    @Override
    protected void process(JsonNode root) {
        String userId = root.get(USER_ID).asText();

        redisCacheService.deleteSubscriptionFromCache(userId);
    }
}