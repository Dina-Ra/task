package org.common.task.subscriptionmanagementsystem.subscription.repository;

import org.common.task.subscriptionmanagementsystem.subscription.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /**
     * Получает страницу счетов пользователя, сразу подгружая связанные сущности.
     * Это предотвращает N+1 запросов при обращении к getSubscription().getSubscriptionType()
     */
    @EntityGraph(attributePaths = {"subscription", "subscription.subscriptionType"})
    Page<Invoice> findAllBySubscriptionUserId(UUID userId, Pageable pageable);
}
