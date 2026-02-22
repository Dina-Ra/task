package org.common.task.subscriptionmanagementsystem.subscription.repository;

import org.common.task.subscriptionmanagementsystem.subscription.model.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    List<Outbox> findTop500ByIsPublishedFalseOrderByCreatedAtAsc();
}
