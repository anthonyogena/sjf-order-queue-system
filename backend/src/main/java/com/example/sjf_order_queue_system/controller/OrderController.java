package com.example.sjf_order_queue_system.controller;

import com.example.sjf_order_queue_system.dto.OrderRequest;
import com.example.sjf_order_queue_system.dto.OrderResponse;
import com.example.sjf_order_queue_system.dto.QueueMetricsResponse;
import com.example.sjf_order_queue_system.entity.enums.OrderStatus;
import com.example.sjf_order_queue_system.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Order Queue", description = "SJF with Aging F&B Order Queue Management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new customer order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/queue")
    @Operation(summary = "Get active PENDING queue sorted by dynamic SJF + Aging priority score")
    public ResponseEntity<List<OrderResponse>> getSortedQueue() {
        return ResponseEntity.ok(orderService.getSortedQueue());
    }

    @GetMapping
    @Operation(summary = "Get orders filtered by status (defaults to PENDING)")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @RequestParam(defaultValue = "PENDING") OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @PatchMapping("/{id}/start")
    @Operation(summary = "Move a PENDING order to IN_PROGRESS")
    public ResponseEntity<OrderResponse> startOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.startOrder(id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark an order as COMPLETED")
    public ResponseEntity<OrderResponse> completeOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.completeOrder(id));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get queue counts and average wait time metrics")
    public ResponseEntity<QueueMetricsResponse> getMetrics() {
        return ResponseEntity.ok(orderService.getMetrics());
    }
}