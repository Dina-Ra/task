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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeactivationHandlerTest {

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private DeactivationHandler deactivationHandler;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Должен вызвать удаление подписки из кэша по userId")
    void handle_Deactivation_Success() {
        // GIVEN
        String userId = "user-123-abc";
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("userId", userId);
        rootNode.put("type", EventType.SUBSCRIPTION_DEACTIVATED.name());

        // WHEN
        deactivationHandler.handle(rootNode);

        // THEN
        verify(redisCacheService).deleteSubscriptionFromCache(userId);
    }

    @Test
    void testSupportedType() {
        assertEquals(EventType.SUBSCRIPTION_DEACTIVATED, deactivationHandler.getSupportedType());
    }
}
