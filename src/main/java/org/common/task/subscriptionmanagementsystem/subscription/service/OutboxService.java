package org.common.task.subscriptionmanagementsystem.subscription.service;

import org.common.task.subscriptionmanagementsystem.subscription.model.Outbox;

import java.util.List;

public interface OutboxService {

    void sendToRabbit(Outbox outbox);

    List<Outbox> findTop500ByIsPublishedFalseOrderByCreatedAtAsc();
}
