package org.common.task.subscriptionmanagementsystem.subscription.shedule;

import org.common.task.subscriptionmanagementsystem.subscription.model.Outbox;
import org.common.task.subscriptionmanagementsystem.subscription.service.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @Test
    @DisplayName("Должен отправить все сообщения из пачки в RabbitMQ")
    void processOutbox_Success() {
        // GIVEN
        Outbox event1 = new Outbox();
        event1.setId(UUID.randomUUID());
        Outbox event2 = new Outbox();
        event2.setId(UUID.randomUUID());

        when(outboxService.findTop500ByIsPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event1, event2));

        // WHEN
        outboxScheduler.processOutbox();

        // THEN
        verify(outboxService, times(1)).sendToRabbit(event1);
        verify(outboxService, times(1)).sendToRabbit(event2);
    }

    @Test
    @DisplayName("Должен прекратить отправку (break) при ошибке RabbitMQ")
    void processOutbox_ShouldBreakOnException() {
        // GIVEN
        Outbox event1 = new Outbox();
        event1.setId(UUID.randomUUID());
        Outbox event2 = new Outbox(); // Это сообщение не должно быть обработано
        event2.setId(UUID.randomUUID());

        when(outboxService.findTop500ByIsPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event1, event2));

        // Имитируем ошибку на первом сообщении
        doThrow(new RuntimeException("RabbitMQ is down"))
                .when(outboxService).sendToRabbit(event1);

        // WHEN
        outboxScheduler.processOutbox();

        // THEN
        verify(outboxService, times(1)).sendToRabbit(event1);
        // Проверяем, что до второго сообщения дело не дошло из-за break
        verify(outboxService, never()).sendToRabbit(event2);
    }

    @Test
    @DisplayName("Ничего не делает, если список пуст")
    void processOutbox_EmptyList() {
        // GIVEN
        when(outboxService.findTop500ByIsPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of());

        // WHEN
        outboxScheduler.processOutbox();

        // THEN
        verify(outboxService, never()).sendToRabbit(any());
    }
}
