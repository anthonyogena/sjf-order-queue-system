package com.example.sjf_order_queue_system.service.impl;

import com.example.sjf_order_queue_system.dto.OrderRequest;
import com.example.sjf_order_queue_system.dto.OrderResponse;
import com.example.sjf_order_queue_system.entity.Order;
import com.example.sjf_order_queue_system.entity.enums.OrderStatus;
import com.example.sjf_order_queue_system.mapper.OrderMapper;
import com.example.sjf_order_queue_system.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderServiceImpl orderService;

    private final ZoneId zone = ZoneId.systemDefault();
    private final Instant fixedInstant = Instant.parse("2026-11-14T04:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedInstant, zone);

    @BeforeEach
    void setUp() {
        OrderMapper orderMapper = new OrderMapper();
        orderService = new OrderServiceImpl(orderRepository, orderMapper, fixedClock);
        ReflectionTestUtils.setField(orderService, "agingFactor", 0.5);
    }

    @Test
    void getSortedQueue_OrdersByShortestPrepTimeFirst_WhenArrivedTogether() {
        LocalDateTime now = LocalDateTime.now(fixedClock);

        Order longOrder = Order.builder()
                .id(1L).customerName("Alice")
                .orderItems(List.of("1x Ribeye Steak", "1x Mashed Potatoes"))
                .estimatedPrepTime(15)
                .status(OrderStatus.PENDING).createdAt(now).build();

        Order shortOrder = Order.builder()
                .id(2L).customerName("Bob")
                .orderItems(List.of("1x Iced Latte"))
                .estimatedPrepTime(3)
                .status(OrderStatus.PENDING).createdAt(now).build();

        when(orderRepository.findByStatus(OrderStatus.PENDING))
                .thenReturn(List.of(longOrder, shortOrder));

        List<OrderResponse> queue = orderService.getSortedQueue();

        assertEquals(2, queue.size());
        assertEquals("Bob", queue.get(0).customerName());
        assertEquals("Alice", queue.get(1).customerName());
    }

    @Test
    void getSortedQueue_AgingPreventsStarvation_LongOrderOvertakesShortOrder() {
        LocalDateTime now = LocalDateTime.now(fixedClock);

        Order agedSteak = Order.builder()
                .id(1L).customerName("Steak Order")
                .orderItems(List.of("1x Steak Platter"))
                .estimatedPrepTime(10)
                .status(OrderStatus.PENDING).createdAt(now.minusMinutes(16)).build();

        Order freshCoffee = Order.builder()
                .id(2L).customerName("Coffee Order")
                .orderItems(List.of("2x Espresso"))
                .estimatedPrepTime(3)
                .status(OrderStatus.PENDING).createdAt(now).build();

        when(orderRepository.findByStatus(OrderStatus.PENDING))
                .thenReturn(List.of(freshCoffee, agedSteak));

        List<OrderResponse> queue = orderService.getSortedQueue();

        assertEquals("Steak Order", queue.get(0).customerName());
        assertEquals(2.0, queue.get(0).priorityScore());
        assertEquals("Coffee Order", queue.get(1).customerName());
        assertEquals(3.0, queue.get(1).priorityScore());
    }

    @Test
    void startOrder_ThrowsConflict_WhenOrderIsAlreadyInProgress() {
        Order inProgressOrder = Order.builder()
                .id(1L).customerName("Alice")
                .orderItems(List.of("1x Club Sandwich"))
                .estimatedPrepTime(5)
                .status(OrderStatus.IN_PROGRESS).createdAt(LocalDateTime.now(fixedClock)).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(inProgressOrder));

        assertThrows(IllegalStateException.class, () -> orderService.startOrder(1L));
    }

    @Test
    void startOrder_ThrowsNotFound_WhenIdDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.startOrder(99L));
    }

    @Test
    void createOrder_TrimsFieldsAndSetsPending() {
        OrderRequest request = new OrderRequest(
                "  Juan Dela Cruz  ",
                List.of("  2x Iced Latte  ", " 1x Pasta "),
                4
        );
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        OrderResponse response = orderService.createOrder(request);

        assertEquals(10L, response.id());
        assertEquals("Juan Dela Cruz", response.customerName());
        assertEquals(List.of("2x Iced Latte", "1x Pasta"), response.orderItems());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(4.0, response.priorityScore());
    }
}