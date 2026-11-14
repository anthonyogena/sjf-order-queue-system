package com.example.sjf_order_queue_system.service.impl;

import com.example.sjf_order_queue_system.dto.OrderRequest;
import com.example.sjf_order_queue_system.dto.OrderResponse;
import com.example.sjf_order_queue_system.dto.QueueMetricsResponse;
import com.example.sjf_order_queue_system.entity.Order;
import com.example.sjf_order_queue_system.entity.enums.OrderStatus;
import com.example.sjf_order_queue_system.mapper.OrderMapper;
import com.example.sjf_order_queue_system.repository.OrderRepository;
import com.example.sjf_order_queue_system.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final Clock clock;

    @Value("${app.scheduling.aging-factor:0.5}")
    private double agingFactor;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = orderMapper.mapToOrder(request, now);
        Order savedOrder = orderRepository.save(order);
        return buildOrderResponse(savedOrder, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getSortedQueue() {
        LocalDateTime now = LocalDateTime.now(clock);
        return orderRepository.findByStatus(OrderStatus.PENDING).stream()
                .map(order -> buildOrderResponse(order, now))
                .sorted(Comparator.comparingDouble(OrderResponse::priorityScore)
                        .thenComparing(OrderResponse::createdAt))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        LocalDateTime now = LocalDateTime.now(clock);
        return orderRepository.findByStatus(status).stream()
                .map(order -> buildOrderResponse(order, now))
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse startOrder(Long id) {
        Order order = findOrderOrThrow(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be started.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setStartedAt(now);
        return buildOrderResponse(orderRepository.save(order), now);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(Long id) {
        Order order = findOrderOrThrow(id);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Order is already completed.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (order.getStartedAt() == null) {
            order.setStartedAt(now);
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(now);
        return buildOrderResponse(orderRepository.save(order), now);
    }

    @Override
    @Transactional(readOnly = true)
    public QueueMetricsResponse getMetrics() {
        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long inProgress = orderRepository.countByStatus(OrderStatus.IN_PROGRESS);
        List<Order> completedOrders = orderRepository.findByStatus(OrderStatus.COMPLETED);

        double avgWaitMinutes = completedOrders.stream()
                .filter(o -> o.getStartedAt() != null)
                .mapToDouble(o -> ChronoUnit.SECONDS.between(o.getCreatedAt(), o.getStartedAt()) / 60.0)
                .average()
                .orElse(0.0);

        return new QueueMetricsResponse(
                pending,
                inProgress,
                completedOrders.size(),
                Math.round(avgWaitMinutes * 100.0) / 100.0
        );
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
    }

    private OrderResponse buildOrderResponse(Order order, LocalDateTime now) {
        LocalDateTime endPoint = (order.getStartedAt() != null) ? order.getStartedAt() : now;
        long waitingSeconds = Math.max(0, ChronoUnit.SECONDS.between(order.getCreatedAt(), endPoint));
        double waitingMinutes = waitingSeconds / 60.0;
        double priorityScore = order.getEstimatedPrepTime() - (waitingMinutes * agingFactor);
        return orderMapper.mapToOrderResponse(order, waitingSeconds, priorityScore);
    }
}