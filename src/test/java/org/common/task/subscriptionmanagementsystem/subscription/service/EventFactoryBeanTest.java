package org.common.task.subscriptionmanagementsystem.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventFactoryBeanTest {

    // Используем Spy для реального объекта, чтобы проверить реальную сериализацию
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private EventFactoryBean eventFactory;

    private Subscription sub;
    private Invoice invoice;
    private User user;
    private SubscriptionType subType;

    @BeforeEach
    void setUp() {
        // Чтобы Jackson умел работать с LocalDate, нужно либо модули добавить,
        // либо в тестах использовать простые строки, но здесь проверим логику сборки Map
        user = new User();
        user.setId(UUID.randomUUID());

        subType = new SubscriptionType();
        subType.setTypeName(SubscriptionEnumType.BASIC);

        sub = new Subscription();
        sub.setId(UUID.randomUUID());
        sub.setUser(user);
        sub.setSubscriptionType(subType);
        sub.setActive(true);
        sub.setActivationDate(LocalDate.now());
        sub.setNextBillingDate(LocalDate.now().plusMonths(1));

        invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setAmount(new BigDecimal("100.00"));
        invoice.setIssuedAt(LocalDate.now());
        invoice.setSubscription(sub);
    }

    @Test
    @DisplayName("Событие активации должно содержать и подписку, и инвойс")
    void buildActivationEvent_WithInvoice() throws JsonProcessingException {
        // WHEN
        String result = eventFactory.buildActivationEvent(sub, invoice);

        // THEN
        assertNotNull(result);
        // Проверяем, что в мапу попали правильные данные перед превращением в JSON
        verify(objectMapper, times(1)).writeValueAsString(anyMap());
        assertTrue(result.contains("SUBSCRIPTION_ACTIVATED"));
        assertTrue(result.contains("subscriptionMap"));
        assertTrue(result.contains("invoiceMap"));
    }

    @Test
    @DisplayName("Событие активации без инвойса")
    void buildActivationEvent_NoInvoice() {
        // WHEN
        String result = eventFactory.buildActivationEvent(sub, null);

        // THEN
        assertNotNull(result);
        assertFalse(result.contains("invoiceMap"));
        assertTrue(result.contains("subscriptionMap"));
    }

    @Test
    @DisplayName("Событие деактивации содержит только базовые поля")
    void buildDeactivationEvent_Success() {
        // WHEN
        String result = eventFactory.buildDeactivationEvent(sub);

        // THEN
        assertNotNull(result);
        assertTrue(result.contains("SUBSCRIPTION_DEACTIVATED"));
        assertTrue(result.contains(user.getId().toString()));
    }

    @Test
    @DisplayName("Событие инвойса содержит все детали")
    void buildInvoiceEvent_Success() {
        // WHEN
        String result = eventFactory.buildInvoiceEvent(invoice);

        // THEN
        assertNotNull(result);
        assertTrue(result.contains("INVOICE_CREATED"));
        assertTrue(result.contains("subscriptionMap"));
        assertTrue(result.contains("invoiceMap"));
        assertTrue(result.contains("100.00"));
    }

    @Test
    @DisplayName("Ошибка сериализации должна пробрасываться как RuntimeException")
    void toJson_ThrowsRuntimeException_OnJsonError() throws JsonProcessingException {
        // GIVEN
        // Заставляем objectMapper выкинуть ошибку
        doThrow(new JsonProcessingException("Error"){}).when(objectMapper).writeValueAsString(any());

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> eventFactory.buildDeactivationEvent(sub));
    }
}

