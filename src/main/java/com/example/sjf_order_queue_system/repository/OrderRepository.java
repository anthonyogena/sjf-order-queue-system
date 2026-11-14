package com.example.sjf_order_queue_system.repository;

import com.example.sjf_order_queue_system.entity.Order;
import com.example.sjf_order_queue_system.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);
    long countByStatus(OrderStatus status);

}
