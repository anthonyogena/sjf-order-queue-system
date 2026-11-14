package com.example.sjf_order_queue_system.mapper;

import com.example.sjf_order_queue_system.dto.OrderRequest;
import com.example.sjf_order_queue_system.dto.OrderResponse;
import com.example.sjf_order_queue_system.entity.Order;
import com.example.sjf_order_queue_system.entity.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderMapper {

    public Order mapToOrder(OrderRequest request, LocalDateTime now) {
        List<String> cleanedItems = request.orderItems().stream()
                .map(String::trim)
                .toList();

        return Order.builder()
                .customerName(request.customerName().trim())
                .orderItems(cleanedItems)
                .estimatedPrepTime(request.estimatedPrepTime())
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .build();
    }

    public OrderResponse mapToOrderResponse(Order order, long waitingSeconds, double priorityScore) {
        double roundedScore = Math.round(priorityScore * 100.0) / 100.0;

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                List.copyOf(order.getOrderItems()),
                order.getEstimatedPrepTime(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getStartedAt(),
                order.getCompletedAt(),
                waitingSeconds,
                roundedScore
        );
    }
}