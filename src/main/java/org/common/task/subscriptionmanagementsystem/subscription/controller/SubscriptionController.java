package org.common.task.subscriptionmanagementsystem.subscription.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.common.task.subscriptionmanagementsystem.subscription.dto.DeactivationRequest;
import org.common.task.subscriptionmanagementsystem.subscription.dto.SubscriptionRequest;
import org.common.task.subscriptionmanagementsystem.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/activate")
    public ResponseEntity<String> activate(@Valid @RequestBody SubscriptionRequest request) {
        subscriptionService.activate(
                request.userId(),
                request.type(),
                request.activationDate()
        );
        return ResponseEntity.ok("Subscription activated successfully");
    }

    @PostMapping("/deactivate")
    public ResponseEntity<String> deactivate(@Valid @RequestBody DeactivationRequest request) {
        subscriptionService.deactivate(request.user().getId());
        return ResponseEntity.ok("Subscription deactivated successfully");
    }
}

