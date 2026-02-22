package org.common.task.subscriptionmanagementsystem.cache.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.common.task.subscriptionmanagementsystem.cache.handler.EventHandler;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheEventConsumerTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EventHandler activationHandler;
    @Mock
    private EventHandler deactivationHandler;

    private CacheEventConsumer consumer;

    @BeforeEach
    void setUp() {
        // Настраиваем моки так, чтобы они возвращали поддерживаемые типы
        when(activationHandler.getSupportedType()).thenReturn(EventType.SUBSCRIPTION_ACTIVATED);
        when(deactivationHandler.getSupportedType()).thenReturn(EventType.SUBSCRIPTION_DEACTIVATED);

        // Создаем потребителя, передавая список моков-хендлеров
        consumer = new CacheEventConsumer(objectMapper, List.of(activationHandler, deactivationHandler));
    }

    @Test
    @DisplayName("Должен вызвать правильный хендлер для события активации")
    void handleEvent_ShouldInvokeCorrectHandler() throws Exception {
        // GIVEN
        String rawMessage = "{\"type\":\"SUBSCRIPTION_ACTIVATED\"}";
        JsonNode rootNode = createJsonNodeWithField("SUBSCRIPTION_ACTIVATED");

        when(objectMapper.readTree(rawMessage)).thenReturn(rootNode);

        // WHEN
        consumer.handleEvent(rawMessage);

        // THEN
        verify(activationHandler, times(1)).handle(rootNode);
        verify(deactivationHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Должен логировать предупреждение, если хендлер для типа не найден")
    void handleEvent_UnknownType_ShouldLogWarning() throws Exception {
        // GIVEN
        String rawMessage = "{\"type\":\"INVOICE_CREATED\"}"; // Для этого типа мы не передали хендлер в setUp
        JsonNode rootNode = createJsonNodeWithField("INVOICE_CREATED");

        when(objectMapper.readTree(rawMessage)).thenReturn(rootNode);

        // WHEN
        consumer.handleEvent(rawMessage);

        // THEN
        verify(activationHandler, never()).handle(any());
        verify(deactivationHandler, never()).handle(any());
        // Проверяем по логам (визуально) или через Mockito, что вызовы не совершались
    }

    @Test
    @DisplayName("Критическая ошибка парсинга не должна прерывать работу приложения")
    void handleEvent_ParseError_ShouldHandleGracefully() throws Exception {
        // GIVEN
        String badMessage = "invalid json";
        when(objectMapper.readTree(badMessage)).thenThrow(new RuntimeException("JSON Parse Error"));

        // WHEN & THEN
        // Метод не должен выбрасывать исключение наружу (оно ловится внутри catch)
        assertDoesNotThrow(() -> consumer.handleEvent(badMessage));
    }

    // Вспомогательный метод для создания JsonNode
    private JsonNode createJsonNodeWithField(String value) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("type", value);
        return node;
    }
}
