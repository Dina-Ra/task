package org.common.task.subscriptionmanagementsystem.subscription.service;

import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.common.task.subscriptionmanagementsystem.subscription.model.Outbox;
import org.common.task.subscriptionmanagementsystem.subscription.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceBeanTest {

    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxServiceBean outboxService;

    @Test
    @DisplayName("Успешная отправка сообщения в RabbitMQ и обновление статуса")
    void sendToRabbit_Success() {
        // GIVEN
        Outbox outbox = new Outbox();
        outbox.setId(UUID.randomUUID());
        outbox.setEventType(EventType.SUBSCRIPTION_ACTIVATED);
        outbox.setData("{\"user\":\"test\"}");
        outbox.setPublished(false);

        // WHEN
        outboxService.sendToRabbit(outbox);

        // THEN
        // 1. Проверяем правильность routingKey (event.subscription_activated)
        verify(rabbitTemplate).convertAndSend(
                eq("subscription.exchange"),
                eq("event.subscription_activated"),
                eq("{\"user\":\"test\"}")
        );

        // 2. Проверяем, что флаг изменился
        assertTrue(outbox.isPublished());
        verify(outboxRepository).save(outbox);
    }

    @Test
    @DisplayName("Ошибка RabbitMQ должна прерывать выполнение")
    void sendToRabbit_RabbitError_ThrowsException() {
        // GIVEN
        Outbox outbox = new Outbox();
        outbox.setEventType(EventType.INVOICE_CREATED);

        // Имитируем падение RabbitMQ
        doThrow(new RuntimeException("RabbitMQ is down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> outboxService.sendToRabbit(outbox));

        // Проверяем, что статус НЕ обновился и save НЕ вызвался
        assertFalse(outbox.isPublished());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Проверка получения списка Top 500")
    void findTop500_Success() {
        // WHEN
        outboxService.findTop500ByIsPublishedFalseOrderByCreatedAtAsc();

        // THEN
        verify(outboxRepository).findTop500ByIsPublishedFalseOrderByCreatedAtAsc();
    }
}

