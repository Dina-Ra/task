package org.common.task.subscriptionmanagementsystem.subscription.service;

import lombok.RequiredArgsConstructor;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.common.task.subscriptionmanagementsystem.subscription.model.Invoice;
import org.common.task.subscriptionmanagementsystem.subscription.model.Outbox;
import org.common.task.subscriptionmanagementsystem.subscription.model.Subscription;
import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.common.task.subscriptionmanagementsystem.subscription.repository.InvoiceRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.OutboxRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BillingServiceBean implements BillingService {
    private final InvoiceRepository invoiceRepository;
    private final OutboxRepository outboxRepository;
    private final EventFactory eventFactory;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public Invoice issueInvoice(Subscription sub) {

        Invoice invoice = new Invoice();
        invoice.setSubscription(sub);
        invoice.setIssuedAt(LocalDate.now());
        invoice.setAmount(sub.getSubscriptionType().getPrice());
        invoiceRepository.save(invoice);

        sub.setNextBillingDate(sub.getNextBillingDate().plusMonths(1));

        subscriptionRepository.saveAndFlush(sub);

        String data = eventFactory.buildInvoiceEvent(invoice);
        Outbox event = Outbox.builder()
                .eventType(EventType.INVOICE_CREATED)
                .data(data)
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .build();
        outboxRepository.save(event);

        return invoice;
    }
}
