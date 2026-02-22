package org.common.task.subscriptionmanagementsystem.cache.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;

public interface EventHandler {

    EventType getSupportedType();

    void handle(JsonNode root);
}
