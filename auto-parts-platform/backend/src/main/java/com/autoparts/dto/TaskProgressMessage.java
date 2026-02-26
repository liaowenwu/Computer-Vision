package com.autoparts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskProgressMessage {
    private String taskNo;
    private String status;
    private int progress;
    private int total;
    private int success;
    private int fail;
    private String message;
}
