package org.common.task.subscriptionmanagementsystem.subscription.shedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.task.subscriptionmanagementsystem.subscription.model.Outbox;
import org.common.task.subscriptionmanagementsystem.subscription.service.OutboxService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 60000)
    public void processOutbox() {
        List<Outbox> outboxes = outboxService.findTop500ByIsPublishedFalseOrderByCreatedAtAsc();

        if (outboxes.isEmpty()) {
            return;
        }

        log.debug("Sending batch of {} messages to RabbitMQ", outboxes.size());

        for (Outbox outbox : outboxes) {
            try {
                outboxService.sendToRabbit(outbox);
            } catch (Exception e) {
                log.error("Outbox send failed for ID {}: {}", outbox.getId(), e.getMessage());
                break;
            }
        }
    }
}
