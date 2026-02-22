package org.common.task.subscriptionmanagementsystem.subscription.service;

import lombok.RequiredArgsConstructor;
import org.common.task.subscriptionmanagementsystem.subscription.model.Outbox;
import org.common.task.subscriptionmanagementsystem.subscription.repository.OutboxRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxServiceBean implements OutboxService {
    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToRabbit(Outbox outbox) {
        String routingKey = "event." + outbox.getEventType().name().toLowerCase();
        rabbitTemplate.convertAndSend("subscription.exchange", routingKey, outbox.getData());

        outbox.setPublished(true);
        outboxRepository.save(outbox);
    }

    @Override
    public List<Outbox> findTop500ByIsPublishedFalseOrderByCreatedAtAsc() {
        return outboxRepository.findTop500ByIsPublishedFalseOrderByCreatedAtAsc();
    }
}

