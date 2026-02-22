package org.common.task.subscriptionmanagementsystem.subscription.repository;

import org.common.task.subscriptionmanagementsystem.subscription.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
