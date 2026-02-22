package org.common.task.subscriptionmanagementsystem.subscription.shedule;

import org.common.task.subscriptionmanagementsystem.subscription.model.Subscription;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionRepository;
import org.common.task.subscriptionmanagementsystem.subscription.service.BillingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private BillingService billingService;

    @InjectMocks
    private BillingScheduler billingScheduler;

    @Test
    @DisplayName("Шедуллер должен обработать все подписки пачками")
    void processMonthlyBilling_ShouldProcessAllSubscribers() {
        // GIVEN
        LocalDate today = LocalDate.now();
        Subscription sub1 = new Subscription();
        Subscription sub2 = new Subscription();

        // 1. ПЕРВАЯ пачка: ставим hasNext = true (третий параметр)
        // Это заставит цикл while сделать еще один круг
        List<Subscription> firstBatchContent = List.of(sub1, sub2);
        Slice<Subscription> firstBatch = new SliceImpl<>(
                firstBatchContent,
                PageRequest.of(0, 500),
                true
        );

        // 2. ВТОРАЯ пачка: пустая (завершение)
        Slice<Subscription> emptyBatch = new SliceImpl<>(List.of(), PageRequest.of(0, 500), false);

        when(subscriptionRepository.findAllByActiveTrueAndNextBillingDate(eq(today), any()))
                .thenReturn(firstBatch)
                .thenReturn(emptyBatch);

        // WHEN
        billingScheduler.processMonthlyBilling();

        // THEN
        verify(billingService, times(1)).issueInvoice(sub1);
        verify(billingService, times(1)).issueInvoice(sub2);

        // Теперь verify на 2 вызова пройдет успешно!
        verify(subscriptionRepository, times(2)).findAllByActiveTrueAndNextBillingDate(eq(today), any());
    }


    @Test
    @DisplayName("Шедуллер должен прекратить обработку пачки при возникновении ошибки")
    void processMonthlyBilling_ShouldStopBatchOnException() {
        // GIVEN
        LocalDate today = LocalDate.now();
        Subscription sub = new Subscription();
        Slice<Subscription> batch = new SliceImpl<>(List.of(sub));

        when(subscriptionRepository.findAllByActiveTrueAndNextBillingDate(eq(today), any()))
                .thenReturn(batch);

        // Имитируем ошибку при выставлении счета
        doThrow(new RuntimeException("Billing failed")).when(billingService).issueInvoice(any());

        // WHEN
        billingScheduler.processMonthlyBilling();

        // THEN
        // Проверяем, что ошибка была обработана и цикл завершился (hasMore станет false в catch блоке)
        verify(billingService, times(1)).issueInvoice(sub);
    }
}
