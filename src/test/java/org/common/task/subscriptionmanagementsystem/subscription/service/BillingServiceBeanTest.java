package org.common.task.subscriptionmanagementsystem.subscription.service;

import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.common.task.subscriptionmanagementsystem.subscription.repository.InvoiceRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.OutboxRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceBeanTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private EventFactory eventFactory;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private BillingServiceBean billingService;

    @Test
    @DisplayName("Успешное выставление счета и обновление даты подписки")
    void issueInvoice_Success() {
        // GIVEN
        SubscriptionType type = new SubscriptionType();
        type.setPrice(new BigDecimal("100.00"));

        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID());
        sub.setSubscriptionType(type);
        LocalDate initialBillingDate = LocalDate.now();
        sub.setNextBillingDate(initialBillingDate);

        when(eventFactory.buildInvoiceEvent(any())).thenReturn("{\"test\":\"json\"}");

        // WHEN
        Invoice result = billingService.issueInvoice(sub);

        // THEN
        // 1. Проверяем расчет суммы счета
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        verify(invoiceRepository).save(any(Invoice.class));

        // 2. Проверяем сдвиг даты на 1 месяц
        assertEquals(initialBillingDate.plusMonths(1), sub.getNextBillingDate());
        verify(subscriptionRepository).saveAndFlush(sub);

        // 3. Проверяем создание Outbox-события
        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        Outbox savedEvent = outboxCaptor.getValue();
        assertEquals(EventType.INVOICE_CREATED, savedEvent.getEventType());
        assertFalse(savedEvent.isPublished());
        assertNotNull(savedEvent.getData());
    }
}

