package com.example.sjf_order_queue_system.service;

import com.example.sjf_order_queue_system.dto.OrderRequest;
import com.example.sjf_order_queue_system.dto.OrderResponse;
import com.example.sjf_order_queue_system.dto.QueueMetricsResponse;
import com.example.sjf_order_queue_system.entity.enums.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    List<OrderResponse> getSortedQueue();

    List<OrderResponse> getOrdersByStatus(OrderStatus status);

    OrderResponse startOrder(Long id);

    OrderResponse completeOrder(Long id);

    QueueMetricsResponse getMetrics();
}