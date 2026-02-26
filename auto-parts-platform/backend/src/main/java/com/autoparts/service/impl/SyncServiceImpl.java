package com.autoparts.service.impl;

import com.autoparts.dto.PythonPriceItem;
import com.autoparts.dto.SyncRequest;
import com.autoparts.dto.TaskProgressMessage;
import com.autoparts.entity.PriceSnapshot;
import com.autoparts.entity.SyncTask;
import com.autoparts.entity.TaskLog;
import com.autoparts.mapper.PriceSnapshotMapper;
import com.autoparts.mapper.SyncTaskMapper;
import com.autoparts.mapper.TaskLogMapper;
import com.autoparts.service.SyncService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {
    private final SyncTaskMapper syncTaskMapper;
    private final PriceSnapshotMapper priceSnapshotMapper;
    private final TaskLogMapper taskLogMapper;
    private final RestClient restClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String syncPrice(SyncRequest request, boolean batch) {
        String taskNo = "SYNC-" + UUID.randomUUID().toString().substring(0, 8);
        SyncTask task = new SyncTask();
        task.setTaskNo(taskNo);
        task.setTaskType(batch ? "BATCH" : "SINGLE");
        task.setTriggerBy(request.getTriggerBy());
        task.setStatus("RUNNING");
        task.setTotalCount(request.getSkus().size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setStartedAt(LocalDateTime.now());
        syncTaskMapper.insert(task);

        pushEvent(taskNo, "RUNNING", 0, request.getSkus().size(), 0, 0,
                "INFO", null, "任务已创建", Map.of("taskId", task.getId()));
        insertLog(task.getId(), taskNo, "INFO", null, "任务已创建");

        executeTask(task.getId(), taskNo, request.getSkus());
        return taskNo;
    }

    @Async("syncTaskExecutor")
    public void executeTask(Long taskId, String taskNo, List<String> skus) {
        int success = 0;
        int fail = 0;

        for (int i = 0; i < skus.size(); i++) {
            String sku = skus.get(i);
            String lockKey = "sync:lock:" + sku;
            try {
                Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, taskNo, 3, TimeUnit.MINUTES);
                if (Boolean.FALSE.equals(locked)) {
                    pushEvent(taskNo, "RUNNING", progress(i + 1, skus.size()), skus.size(), success, fail,
                            "WARN", sku, "SKU 正在被其他任务同步，已跳过", null);
                    insertLog(taskId, taskNo, "WARN", sku, "SKU 正在被其他任务同步，已跳过");
                    continue;
                }

                pushEvent(taskNo, "RUNNING", progress(i, skus.size()), skus.size(), success, fail,
                        "INFO", sku, "开始调用本地爬虫服务", null);
                insertLog(taskId, taskNo, "INFO", sku, "开始调用本地爬虫服务");

                List<PythonPriceItem> items = restClient.post()
                        .uri("/run")
                        .body(Map.of("sku", sku))
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<PythonPriceItem>>() {});

                int saved = 0;
                if (items != null) {
                    for (PythonPriceItem item : items) {
                        if (item.getSku() == null || item.getSupplierName() == null || item.getPrice() == null) {
                            continue;
                        }

                        PriceSnapshot existed = priceSnapshotMapper.selectOne(new LambdaQueryWrapper<PriceSnapshot>()
                                .eq(PriceSnapshot::getSku, item.getSku())
                                .eq(PriceSnapshot::getSupplierName, item.getSupplierName())
                                .eq(PriceSnapshot::getSnapshotDate, LocalDate.now())
                                .last("limit 1"));

                        PriceSnapshot snapshot = new PriceSnapshot();
                        snapshot.setSku(item.getSku());
                        snapshot.setProductName(item.getProductName());
                        snapshot.setBrand(item.getBrand());
                        snapshot.setRegion(item.getRegion());
                        snapshot.setCompanyName(item.getCompanyName());
                        snapshot.setSupplierName(item.getSupplierName());
                        snapshot.setStock(item.getStock());
                        snapshot.setPrice(item.getPrice());
                        snapshot.setSnapshotDate(LocalDate.now());
                        snapshot.setSnapshotTime(LocalDateTime.now());
                        snapshot.setTaskId(taskId);

                        if (existed == null) {
                            priceSnapshotMapper.insert(snapshot);
                        } else {
                            snapshot.setId(existed.getId());
                            priceSnapshotMapper.updateById(snapshot);
                        }
                        saved++;
                    }
                }

                success++;
                pushEvent(taskNo, "RUNNING", progress(i + 1, skus.size()), skus.size(), success, fail,
                        "INFO", sku, "同步完成", Map.of("savedCount", saved, "records", items == null ? List.of() : items));
                insertLog(taskId, taskNo, "INFO", sku, "同步完成，入库记录: " + saved);
            } catch (Exception ex) {
                fail++;
                log.error("sync sku failed {}", sku, ex);
                pushEvent(taskNo, "RUNNING", progress(i + 1, skus.size()), skus.size(), success, fail,
                        "ERROR", sku, "同步失败: " + ex.getMessage(), null);
                insertLog(taskId, taskNo, "ERROR", sku, "同步失败: " + ex.getMessage());
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        SyncTask done = new SyncTask();
        done.setId(taskId);
        done.setStatus(fail > 0 ? "PARTIAL_SUCCESS" : "SUCCESS");
        done.setSuccessCount(success);
        done.setFailCount(fail);
        done.setFinishedAt(LocalDateTime.now());
        syncTaskMapper.updateById(done);

        pushEvent(taskNo, done.getStatus(), 100, skus.size(), success, fail,
                "INFO", null, "任务完成", null);
        insertLog(taskId, taskNo, "INFO", null, "任务完成: " + done.getStatus());
    }

    private int progress(int current, int total) {
        if (total == 0) {
            return 100;
        }
        return (int) ((current * 100.0f) / total);
    }

    private void pushEvent(String taskNo, String status, int progress, int total, int success, int fail,
                           String level, String sku, String message, Object data) {
        messagingTemplate.convertAndSend(
                "/topic/task/" + taskNo,
                new TaskProgressMessage(taskNo, status, progress, total, success, fail, level, sku, message, data, LocalDateTime.now())
        );
    }

    private void insertLog(Long taskId, String taskNo, String level, String sku, String message) {
        TaskLog logLine = new TaskLog();
        logLine.setTaskId(taskId);
        logLine.setTaskNo(taskNo);
        logLine.setLevel(level);
        logLine.setSku(sku);
        logLine.setMessage(message);
        logLine.setCreatedAt(LocalDateTime.now());
        taskLogMapper.insert(logLine);
    }
}
