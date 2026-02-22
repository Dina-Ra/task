package org.common.task.subscriptionmanagementsystem.subscription.shedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.task.subscriptionmanagementsystem.subscription.model.Subscription;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionRepository;
import org.common.task.subscriptionmanagementsystem.subscription.service.BillingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingService billingService;
    private static final int BATCH_SIZE = 500;

    @Scheduled(fixedDelay = 60000) // cron = "0 0 2 * * *"
    public void processMonthlyBilling() {
        LocalDate today = LocalDate.now();
        log.info("Starting batch billing for date: {}", today);

        boolean hasMore = true;
        int totalProcessed = 0;

        while (hasMore) {
            Slice<Subscription> batch = subscriptionRepository.findAllByActiveTrueAndNextBillingDate(
                    today, PageRequest.of(0, BATCH_SIZE)
            );

            if (batch.isEmpty()) {
                hasMore = false;
                continue;
            }

            for (Subscription sub : batch) {
                try {
                    billingService.issueInvoice(sub);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Failed to bill sub {}: {}", sub.getId(), e.getMessage());

                    hasMore = false;
                }
            }

            if (!batch.hasNext()) {
                hasMore = false;
            }
        }
        log.info("Billing finished. Total processed: {}", totalProcessed);
    }
}


