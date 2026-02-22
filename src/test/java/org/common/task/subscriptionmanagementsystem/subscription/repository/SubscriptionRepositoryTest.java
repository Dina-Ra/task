package org.common.task.subscriptionmanagementsystem.subscription.repository;

import org.common.task.subscriptionmanagementsystem.BaseTestContainer;
import org.common.task.subscriptionmanagementsystem.subscription.model.Subscription;
import org.common.task.subscriptionmanagementsystem.subscription.model.SubscriptionEnumType;
import org.common.task.subscriptionmanagementsystem.subscription.model.SubscriptionType;
import org.common.task.subscriptionmanagementsystem.subscription.model.User;
import org.common.task.subscriptionmanagementsystem.subscription.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionRepositoryTest extends BaseTestContainer {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionTypeRepository typeRepository;

    private User testUser;
    private SubscriptionType proType;

    @BeforeEach
    void setUp() {
        // Очищаем базу перед каждым тестом (опционально, если контейнер не перезапускается)
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
        typeRepository.deleteAll();

        // 1. Создаем пользователя
        testUser = new User();
        testUser.setName("John Doe");
        testUser = userRepository.save(testUser);

        // 2. Создаем тип подписки
        proType = new SubscriptionType();
        proType.setTypeName(SubscriptionEnumType.PRO);
        proType.setPrice(new BigDecimal("200.00"));
        proType = typeRepository.save(proType);
    }

    @Test
    @DisplayName("Должен находить активную подписку пользователя")
    void shouldFindActiveSubscriptionByUserId() {
        // GIVEN
        Subscription sub = new Subscription();
        sub.setUser(testUser);
        sub.setSubscriptionType(proType);
        sub.setActive(true);
        sub.setActivationDate(LocalDate.now());
        sub.setNextBillingDate(LocalDate.now().plusMonths(1));
        subscriptionRepository.save(sub);

        // WHEN
        Optional<Subscription> result = subscriptionRepository.findByUserIdAndActiveTrue(testUser.getId());

        // THEN
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getUser().getId());
        assertTrue(result.get().isActive());
    }

    @Test
    @DisplayName("Должен находить пачку подписок на конкретную дату (JOIN FETCH)")
    void shouldFindSubscriptionsByBillingDate() {
        // GIVEN
        LocalDate targetDate = LocalDate.now().plusDays(10);

        // Эта подписка должна попасть в выборку
        Subscription subToBill = new Subscription();
        subToBill.setUser(testUser);
        subToBill.setSubscriptionType(proType);
        subToBill.setActive(true);
        subToBill.setActivationDate(LocalDate.now());
        subToBill.setNextBillingDate(targetDate);
        subscriptionRepository.save(subToBill);

        // Эта — нет (другая дата)
        Subscription subOtherDate = new Subscription();
        subOtherDate.setUser(testUser);
        subOtherDate.setSubscriptionType(proType);
        subOtherDate.setActive(true);
        subOtherDate.setActivationDate(LocalDate.now());
        subOtherDate.setNextBillingDate(targetDate.plusDays(1));
        subscriptionRepository.save(subOtherDate);

        // WHEN
        Slice<Subscription> result = subscriptionRepository.findAllByActiveTrueAndNextBillingDate(
                targetDate, PageRequest.of(0, 10)
        );

        // THEN
        assertEquals(1, result.getContent().size());
        Subscription found = result.getContent().get(0);
        assertEquals(targetDate, found.getNextBillingDate());

        // Проверяем работу JOIN FETCH: тип подписки должен быть доступен
        assertNotNull(found.getSubscriptionType());
        assertEquals(SubscriptionEnumType.PRO, found.getSubscriptionType().getTypeName());
    }

    @Test
    @DisplayName("Не должен находить неактивные подписки")
    void shouldNotFindInactiveSubscriptions() {
        // GIVEN
        Subscription inactiveSub = new Subscription();
        inactiveSub.setUser(testUser);
        inactiveSub.setSubscriptionType(proType);
        inactiveSub.setActive(false); // Неактивна
        inactiveSub.setActivationDate(LocalDate.now());
        inactiveSub.setNextBillingDate(LocalDate.now());
        subscriptionRepository.save(inactiveSub);

        // WHEN
        Optional<Subscription> result = subscriptionRepository.findByUserIdAndActiveTrue(testUser.getId());

        // THEN
        assertTrue(result.isEmpty());
    }
}

