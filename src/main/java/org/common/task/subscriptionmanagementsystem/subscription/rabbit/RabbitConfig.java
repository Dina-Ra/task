package org.common.task.subscriptionmanagementsystem.subscription.rabbit;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue}")
    private String queueName;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKeyPattern;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange subscriptionExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue cacheQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding cacheBinding(Queue cacheQueue, TopicExchange subscriptionExchange) {
        return BindingBuilder
                .bind(cacheQueue)
                .to(subscriptionExchange)
                .with(routingKeyPattern);
    }
}
