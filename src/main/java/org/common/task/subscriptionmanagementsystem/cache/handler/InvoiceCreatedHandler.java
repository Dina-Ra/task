package org.common.task.subscriptionmanagementsystem.cache.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.common.task.subscriptionmanagementsystem.cache.service.RedisCacheService;
import org.springframework.stereotype.Component;

@Component
public class InvoiceCreatedHandler extends AbstractRedisHandler {

    public InvoiceCreatedHandler(RedisCacheService redisCacheService) {
        super(redisCacheService);
    }

    @Override
    public EventType getSupportedType() { return EventType.INVOICE_CREATED; }

    @Override
    protected void process(JsonNode root) {
        String userId = root.get(USER_ID).asText();

        redisCacheService.saveSubscriptionToCache(userId, root.get(SUBSCRIPTION_MAP).toString());

        if (root.has(INVOICES_MAP)) {
            redisCacheService.addInvoiceToCache(userId, root.get(INVOICES_MAP).toString());
        }
    }
}
