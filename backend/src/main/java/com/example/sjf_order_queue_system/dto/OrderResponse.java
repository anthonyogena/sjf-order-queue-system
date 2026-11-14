package com.example.sjf_order_queue_system.dto;

import com.example.sjf_order_queue_system.entity.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse (
        Long id,
        String customerName,
        List<String> orderItems,
        Integer estimatedPrepTime,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        long waitingSeconds,
        double priorityScore
) {}
