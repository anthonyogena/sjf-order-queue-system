package com.example.sjf_order_queue_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class ApiError {

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final OffsetDateTime timestamp;


}
