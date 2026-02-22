package org.common.task.subscriptionmanagementsystem.subscription.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "subscription_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private SubscriptionEnumType typeName;

    @Column(nullable = false, columnDefinition = "NUMERIC(19,2)")
    private BigDecimal price;
}

