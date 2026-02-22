package org.common.task.subscriptionmanagementsystem.cache.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.common.task.subscriptionmanagementsystem.cache.service.RedisCacheService;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivationHandlerTest {

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private ActivationHandler activationHandler;

    private final ObjectMapper mapper = new ObjectMapper();
    private ObjectNode rootNode;
    private final String userId = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        rootNode = mapper.createObjectNode();
        rootNode.put("userId", userId);
        rootNode.putObject("subscriptionMap").put("id", "sub123");
    }

    @Test
    @DisplayName("Должен сохранить подписку и инвойс, если они есть в JSON")
    void handle_WithInvoice_Success() {
        // GIVEN
        rootNode.putObject("invoiceMap").put("id", "inv123");

        // WHEN
        activationHandler.handle(rootNode);

        // THEN
        verify(redisCacheService).saveSubscriptionToCache(eq(userId), contains("sub123"));
        verify(redisCacheService).addInvoiceToCache(eq(userId), contains("inv123"));
    }

    @Test
    @DisplayName("Должен сохранить только подписку, если инвойса нет")
    void handle_WithoutInvoice_Success() {
        // WHEN
        activationHandler.handle(rootNode);

        // THEN
        verify(redisCacheService).saveSubscriptionToCache(eq(userId), anyString());
        verify(redisCacheService, never()).addInvoiceToCache(anyString(), anyString());
    }

    @Test
    @DisplayName("Должен перехватить ошибку Redis и залогировать её (Abstract Handler logic)")
    void handle_RedisDown_CaughtInAbstract() {
        // GIVEN
        doThrow(new DataAccessResourceFailureException("Connection refused"))
                .when(redisCacheService).saveSubscriptionToCache(anyString(), anyString());

        // WHEN & THEN
        // Метод не должен бросать исключение (оно ловится в AbstractRedisHandler)
        activationHandler.handle(rootNode);

        verify(redisCacheService).saveSubscriptionToCache(eq(userId), anyString());
    }

    @Test
    void testSupportedType() {
        assertEquals(EventType.SUBSCRIPTION_ACTIVATED, activationHandler.getSupportedType());
    }
}
