package org.common.task.subscriptionmanagementsystem.cache.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.common.task.subscriptionmanagementsystem.cache.handler.EventHandler;
import org.common.task.subscriptionmanagementsystem.subscription.model.EventType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CacheEventConsumer {

    private final ObjectMapper objectMapper;
    private final Map<EventType, EventHandler> handlerMap;

    public CacheEventConsumer(ObjectMapper objectMapper, List<EventHandler> handlers) {
        this.objectMapper = objectMapper;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(EventHandler::getSupportedType, h -> h));
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void handleEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            EventType type = EventType.valueOf(root.get("type").asText());

            EventHandler handler = handlerMap.get(type);
            if (handler != null) {
                handler.handle(root);
                log.info("Successfully handled event: {}", type);
            } else {
                log.warn("No handler registered for type: {}", type);
            }

        } catch (Exception e) {
            log.error("Critical error in Cache Consumer: {}", e.getMessage());
        }
    }
}


