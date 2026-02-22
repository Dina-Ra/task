package org.common.task.subscriptionmanagementsystem.subscription.service;

import org.common.task.subscriptionmanagementsystem.subscription.model.Invoice;
import org.common.task.subscriptionmanagementsystem.subscription.model.Subscription;

public interface BillingService {

    Invoice issueInvoice(Subscription sub);
}
