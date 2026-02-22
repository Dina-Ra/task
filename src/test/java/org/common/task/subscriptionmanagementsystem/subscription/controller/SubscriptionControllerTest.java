package org.common.task.subscriptionmanagementsystem.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.common.task.subscriptionmanagementsystem.subscription.dto.DeactivationRequest;
import org.common.task.subscriptionmanagementsystem.subscription.dto.SubscriptionRequest;
import org.common.task.subscriptionmanagementsystem.subscription.model.SubscriptionEnumType;
import org.common.task.subscriptionmanagementsystem.subscription.model.User;
import org.common.task.subscriptionmanagementsystem.subscription.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("Успешная активация через POST /activate")
    void activate_Success() throws Exception {
        // GIVEN
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        SubscriptionRequest request = new SubscriptionRequest(userId, SubscriptionEnumType.PRO, date);

        // Имитируем успешную работу сервиса (ничего не возвращает)
        doNothing().when(subscriptionService).activate(userId, SubscriptionEnumType.PRO, date);

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/subscriptions/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Subscription activated successfully"));

        verify(subscriptionService).activate(userId, SubscriptionEnumType.PRO, date);
    }

    @Test
    @DisplayName("Успешная деактивация через POST /deactivate")
    void deactivate_Success() throws Exception {
        // GIVEN
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        DeactivationRequest request = new DeactivationRequest(user);

        doNothing().when(subscriptionService).deactivate(userId);

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/subscriptions/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Subscription deactivated successfully"));

        verify(subscriptionService).deactivate(userId);
    }

    @Test
    @DisplayName("Должен возвращать 400 при неверном формате JSON")
    void activate_InvalidJson_BadRequest() throws Exception {
        // WHEN & THEN
        mockMvc.perform(post("/api/v1/subscriptions/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"invalid\": \"json\" }"))
                .andExpect(status().isBadRequest());
    }
}

