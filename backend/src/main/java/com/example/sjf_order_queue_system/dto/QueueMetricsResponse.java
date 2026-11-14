package com.example.sjf_order_queue_system.dto;

public record QueueMetricsResponse(
        long pendingCount,
        long inProgressCount,
        long completedCount,
        double averageWaitMinutes
) {}