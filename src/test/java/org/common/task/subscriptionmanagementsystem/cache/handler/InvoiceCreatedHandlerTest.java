package org.common.task.subscriptionmanagementsystem.cache.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.common.task.subscriptionmanagementsystem.cache.service.RedisCacheService;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvoiceCreatedHandlerTest {

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private InvoiceCreatedHandler invoiceCreatedHandler;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Должен обновить подписку и добавить инвойс при получении события")
    void handle_InvoiceCreated_Success() {
        // GIVEN
        String userId = "user-789";
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("userId", userId);

        // Данные подписки (с обновленной датой)
        rootNode.putObject("subscriptionMap")
                .put("subscriptionId", "sub-1")
                .put("nextBillingDate", "2026-03-22");

        // Данные нового инвойса
        rootNode.putObject("invoiceMap")
                .put("id", "inv-new-001")
                .put("amount", 200.0);

        // WHEN
        invoiceCreatedHandler.handle(rootNode);

        // THEN
        // 1. Проверяем обновление подписки (новое состояние)
        verify(redisCacheService).saveSubscriptionToCache(eq(userId), contains("2026-03-22"));

        // 2. Проверяем добавление инвойса (новая запись в историю)
        verify(redisCacheService).addInvoiceToCache(eq(userId), contains("inv-new-001"));
    }

    @Test
    void testSupportedType() {
        assertEquals(EventType.INVOICE_CREATED, invoiceCreatedHandler.getSupportedType());
    }

    // Хелпер для проверки содержания строки в аргументах Mockito
    private String contains(String part) {
        return argThat(s -> s != null && s.contains(part));
    }
}
