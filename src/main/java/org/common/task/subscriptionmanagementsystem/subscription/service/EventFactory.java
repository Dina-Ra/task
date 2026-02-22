package org.common.task.subscriptionmanagementsystem.subscription.service;

import org.common.task.subscriptionmanagementsystem.subscription.model.Invoice;
import org.common.task.subscriptionmanagementsystem.subscription.model.Subscription;

public interface EventFactory {

    String buildActivationEvent(Subscription sub, Invoice firstInvoice);

    String buildDeactivationEvent(Subscription sub);

    String buildInvoiceEvent(Invoice invoice);
}
