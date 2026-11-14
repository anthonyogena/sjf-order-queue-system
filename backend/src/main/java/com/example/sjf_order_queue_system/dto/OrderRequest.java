package com.example.sjf_order_queue_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(

        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotEmpty(message = "At least one order item is required")
        List<@NotBlank(message = "Order item cannot be blank") String> orderItems,

        @NotNull(message = "Estimated prep time is required")
        @Min(value = 1, message = "Estimated prep time must be at least 1 minute")
        Integer estimatedPrepTime

) {}
