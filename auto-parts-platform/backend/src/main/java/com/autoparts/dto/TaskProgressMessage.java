package com.autoparts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TaskProgressMessage {
    private String taskNo;
    private String status;
    private int progress;
    private int total;
    private int success;
    private int fail;
    private String level;
    private String sku;
    private String message;
    private Object data;
    private LocalDateTime time;
}
