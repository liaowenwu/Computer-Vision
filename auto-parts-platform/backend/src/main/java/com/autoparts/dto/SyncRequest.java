package com.autoparts.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SyncRequest {
    @NotEmpty
    private List<String> skus;
    private String triggerBy;
}
