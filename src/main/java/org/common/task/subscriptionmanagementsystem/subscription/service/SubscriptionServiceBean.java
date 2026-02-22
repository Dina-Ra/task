package org.common.task.subscriptionmanagementsystem.subscription.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.common.task.subscriptionmanagementsystem.subscription.repository.OutboxRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionTypeRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceBean implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTypeRepository typeRepository;
    private final OutboxRepository outboxRepository;
    private final BillingService billingService;
    private final UserRepository userRepository;
    private final EventFactory eventFactory;

    @Transactional
    public void activate(UUID userId, SubscriptionEnumType typeName, LocalDate activationDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with ID " + userId + " not found"));

        if (activationDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Activation date cannot be in the past");
        }

        subscriptionRepository.findByUserIdAndActiveTrue(userId)
                .ifPresent(s -> {
                    throw new IllegalStateException("User already has active subscription");
                });

        SubscriptionType type = typeRepository.findByTypeName(typeName)
                .orElseThrow(() -> new RuntimeException("Subscription type not found"));

        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setSubscriptionType(type);
        sub.setActivationDate(activationDate);
        sub.setNextBillingDate(activationDate);
        sub.setActive(true);
        subscriptionRepository.save(sub);

        Invoice invoice = null;
        if (activationDate.equals(LocalDate.now())) {
            invoice = billingService.issueInvoice(sub);
        }

        String jsonData = eventFactory.buildActivationEvent(sub, invoice);

        saveToOutbox(EventType.SUBSCRIPTION_ACTIVATED, jsonData);
    }

    @Transactional
    public void deactivate(UUID userId) {
        Subscription sub = subscriptionRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        sub.setActive(false);
        subscriptionRepository.save(sub);

        String jsonData = eventFactory.buildDeactivationEvent(sub);

        saveToOutbox(EventType.SUBSCRIPTION_DEACTIVATED, jsonData);
    }

    private void saveToOutbox(EventType type, String jsonData) {
        Outbox outbox = Outbox.builder()
                .eventType(type)
                .data(jsonData)
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(outbox);
    }
}
