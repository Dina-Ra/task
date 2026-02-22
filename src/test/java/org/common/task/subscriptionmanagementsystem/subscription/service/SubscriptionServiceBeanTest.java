package org.common.task.subscriptionmanagementsystem.subscription.service;

import jakarta.persistence.EntityNotFoundException;
import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.common.task.subscriptionmanagementsystem.subscription.repository.OutboxRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.SubscriptionTypeRepository;
import org.common.task.subscriptionmanagementsystem.subscription.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceBeanTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionTypeRepository typeRepository;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private BillingService billingService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventFactory eventFactory;

    @InjectMocks
    private SubscriptionServiceBean subscriptionService;

    private UUID userId;
    private User user;
    private SubscriptionType subType;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setName("Test User");

        subType = new SubscriptionType();
        subType.setTypeName(SubscriptionEnumType.PRO);
    }

    @Test
    @DisplayName("Успешная активация подписки сегодня (со счетом)")
    void activate_Success_Today() {
        // GIVEN
        LocalDate today = LocalDate.now();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.empty());
        when(typeRepository.findByTypeName(SubscriptionEnumType.PRO)).thenReturn(Optional.of(subType));
        when(eventFactory.buildActivationEvent(any(), any())).thenReturn("{}");

        // WHEN
        subscriptionService.activate(userId, SubscriptionEnumType.PRO, today);

        // THEN
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(billingService).issueInvoice(any(Subscription.class)); // Проверка, что счет выставлен
        verify(outboxRepository).save(any(Outbox.class));
    }

    @Test
    @DisplayName("Успешная активация в будущем (без немедленного счета)")
    void activate_Success_Future() {
        // GIVEN
        LocalDate futureDate = LocalDate.now().plusDays(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.empty());
        when(typeRepository.findByTypeName(SubscriptionEnumType.PRO)).thenReturn(Optional.of(subType));

        // WHEN
        subscriptionService.activate(userId, SubscriptionEnumType.PRO, futureDate);

        // THEN
        verify(billingService, never()).issueInvoice(any()); // Счет НЕ должен выставляться
    }

    @Test
    @DisplayName("Ошибка: пользователь не найден")
    void activate_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                subscriptionService.activate(userId, SubscriptionEnumType.PRO, LocalDate.now())
        );
    }

    @Test
    @DisplayName("Ошибка: дата в прошлом")
    void activate_PastDate_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        LocalDate pastDate = LocalDate.now().minusDays(1);

        assertThrows(IllegalArgumentException.class, () ->
                subscriptionService.activate(userId, SubscriptionEnumType.PRO, pastDate)
        );
    }

    @Test
    @DisplayName("Ошибка: уже есть активная подписка")
    void activate_AlreadyActive_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.of(new Subscription()));

        assertThrows(IllegalStateException.class, () ->
                subscriptionService.activate(userId, SubscriptionEnumType.PRO, LocalDate.now())
        );
    }

    @Test
    @DisplayName("Успешная деактивация")
    void deactivate_Success() {
        // GIVEN
        Subscription sub = new Subscription();
        sub.setActive(true);
        when(subscriptionRepository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.of(sub));
        when(eventFactory.buildDeactivationEvent(sub)).thenReturn("{}");

        // WHEN
        subscriptionService.deactivate(userId);

        // THEN
        assertFalse(sub.isActive());
        verify(subscriptionRepository).save(sub);
        verify(outboxRepository).save(any(Outbox.class));
    }

    @Test
    @DisplayName("Ошибка деактивации: подписка не найдена")
    void deactivate_NotFound_ThrowsException() {
        when(subscriptionRepository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> subscriptionService.deactivate(userId));
    }

    @Test
    @DisplayName("Проверка данных в Outbox (ArgumentCaptor)")
    void saveToOutbox_CorrectData() {
        // GIVEN
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.empty());
        when(typeRepository.findByTypeName(SubscriptionEnumType.PRO)).thenReturn(Optional.of(subType));
        String expectedJson = "{\"test\":\"json\"}";
        when(eventFactory.buildActivationEvent(any(), any())).thenReturn(expectedJson);

        // WHEN
        subscriptionService.activate(userId, SubscriptionEnumType.PRO, LocalDate.now());

        // THEN (захватываем объект Outbox, который ушел в save)
        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        Outbox savedOutbox = outboxCaptor.getValue();
        assertEquals(expectedJson, savedOutbox.getData());
        assertEquals(EventType.SUBSCRIPTION_ACTIVATED, savedOutbox.getEventType());
        assertFalse(savedOutbox.isPublished());
    }
}
