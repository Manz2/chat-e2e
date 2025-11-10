package com.chat.e2e.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageDeliveryRepository extends JpaRepository<MessageDelivery, UUID> {}
