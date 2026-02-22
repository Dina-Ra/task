package org.common.task.subscriptionmanagementsystem.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.common.task.subscriptionmanagementsystem.subscription.model.Invoice;
import org.common.task.subscriptionmanagementsystem.subscription.model.Subscription;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventFactoryBean implements EventFactory {

    private final ObjectMapper objectMapper;

    @Override
    public String buildActivationEvent(Subscription sub, Invoice firstInvoice) {
        Map<String, Object> data = createBaseEvent(EventType.SUBSCRIPTION_ACTIVATED, sub);

        data.put("subscriptionMap", mapSubscription(sub));

        if (firstInvoice != null) {
            data.put("invoiceMap", mapInvoice(firstInvoice));
        }

        return toJson(data);
    }

    @Override
    public String buildDeactivationEvent(Subscription sub) {
        Map<String, Object> data = createBaseEvent(EventType.SUBSCRIPTION_DEACTIVATED, sub);
        return toJson(data);
    }

    @Override
    public String buildInvoiceEvent(Invoice invoice) {
        Subscription sub = invoice.getSubscription();
        Map<String, Object> data = createBaseEvent(EventType.INVOICE_CREATED, sub);

        data.put("subscriptionMap", mapSubscription(sub));
        data.put("invoiceMap", mapInvoice(invoice));

        return toJson(data);
    }

    private Map<String, Object> createBaseEvent(EventType type, Subscription sub) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type.name());
        data.put("userId", sub.getUser().getId());
        return data;
    }

    private Map<String, Object> mapSubscription(Subscription sub) {
        Map<String, Object> subscriptionMap = new LinkedHashMap<>();
        subscriptionMap.put("subscriptionId", sub.getId());
        subscriptionMap.put("active", sub.isActive());
        subscriptionMap.put("subscriptionType", sub.getSubscriptionType().getTypeName());
        subscriptionMap.put("activationDate", sub.getActivationDate());
        subscriptionMap.put("nextBillingDate", sub.getNextBillingDate());
        return subscriptionMap;
    }

    private Map<String, Object> mapInvoice(Invoice invoice) {
        Map<String, Object> invoiceMap = new LinkedHashMap<>();
        invoiceMap.put("id", invoice.getId());
        invoiceMap.put("amount", invoice.getAmount());
        invoiceMap.put("issuedAt", invoice.getIssuedAt());
        invoiceMap.put("subscriptionType", invoice.getSubscription().getSubscriptionType().getTypeName());
        invoiceMap.put("subscriptionActivationDate", invoice.getSubscription().getActivationDate());

        return invoiceMap;
    }

    private String toJson(Object map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации события в JSON", e);
        }
    }
}
