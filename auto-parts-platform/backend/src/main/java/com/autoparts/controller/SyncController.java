package com.autoparts.controller;

import com.autoparts.dto.SyncRequest;
import com.autoparts.entity.SyncTask;
import com.autoparts.mapper.SyncTaskMapper;
import com.autoparts.service.SyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {
    private final SyncService syncService;
    private final SyncTaskMapper syncTaskMapper;

    @PostMapping("/price")
    public Map<String, String> syncSingle(@Valid @RequestBody SyncRequest request) {
        return Map.of("taskNo", syncService.syncPrice(request, false));
    }

    @PostMapping("/price/batch")
    public Map<String, String> syncBatch(@Valid @RequestBody SyncRequest request) {
        return Map.of("taskNo", syncService.syncPrice(request, true));
    }

    @GetMapping("/tasks/{taskNo}")
    public SyncTask task(@PathVariable String taskNo) {
        return syncTaskMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SyncTask>().lambda()
                .eq(SyncTask::getTaskNo, taskNo));
    }
}
