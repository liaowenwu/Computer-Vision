package com.autoparts.controller;

import com.autoparts.dto.SyncRequest;
import com.autoparts.entity.SyncTask;
import com.autoparts.entity.TaskLog;
import com.autoparts.mapper.SyncTaskMapper;
import com.autoparts.mapper.TaskLogMapper;
import com.autoparts.service.SyncService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {
    private final SyncService syncService;
    private final SyncTaskMapper syncTaskMapper;
    private final TaskLogMapper taskLogMapper;

    @PostMapping("/price")
    public Map<String, String> syncSingle(@Valid @RequestBody SyncRequest request) {
        return Map.of("taskNo", syncService.syncPrice(request, false));
    }

    @PostMapping("/price/batch")
    public Map<String, String> syncBatch(@Valid @RequestBody SyncRequest request) {
        return Map.of("taskNo", syncService.syncPrice(request, true));
    }

    @GetMapping("/tasks")
    public List<SyncTask> tasks() {
        return syncTaskMapper.selectList(new LambdaQueryWrapper<SyncTask>().orderByDesc(SyncTask::getId));
    }

    @GetMapping("/tasks/{taskNo}")
    public SyncTask task(@PathVariable String taskNo) {
        return syncTaskMapper.selectOne(new LambdaQueryWrapper<SyncTask>()
                .eq(SyncTask::getTaskNo, taskNo)
                .last("limit 1"));
    }

    @GetMapping("/tasks/{taskNo}/logs")
    public List<TaskLog> taskLogs(@PathVariable String taskNo) {
        return taskLogMapper.selectList(new LambdaQueryWrapper<TaskLog>()
                .eq(TaskLog::getTaskNo, taskNo)
                .orderByAsc(TaskLog::getId));
    }
}
