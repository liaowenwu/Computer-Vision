package com.autoparts.service.impl;

import com.autoparts.dto.PythonPriceItem;
import com.autoparts.dto.SyncRequest;
import com.autoparts.dto.TaskProgressMessage;
import com.autoparts.entity.PriceSnapshot;
import com.autoparts.entity.SyncTask;
import com.autoparts.mapper.PriceSnapshotMapper;
import com.autoparts.mapper.SyncTaskMapper;
import com.autoparts.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {
    private final SyncTaskMapper syncTaskMapper;
    private final PriceSnapshotMapper priceSnapshotMapper;
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
        syncTaskMapper.insert(task);
        executeTask(task.getId(), taskNo, request.getSkus());
        return taskNo;
    }

    @Async("syncTaskExecutor")
    public void executeTask(Long taskId, String taskNo, List<String> skus) {
        int success = 0;
        int fail = 0;
        for (int i = 0; i < skus.size(); i++) {
            String sku = skus.get(i);
            try {
                String lockKey = "sync:lock:" + sku;
                Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, taskNo);
                if (Boolean.FALSE.equals(locked)) {
                    continue;
                }
                List<PythonPriceItem> items = restClient.post()
                        .uri("/run")
                        .body(Map.of("sku", sku))
                        .retrieve()
                        .body(List.class);
                if (items != null) {
                    for (Object raw : items) {
                        Map<?, ?> itemMap = (Map<?, ?>) raw;
                        PriceSnapshot snapshot = new PriceSnapshot();
                        snapshot.setSku(String.valueOf(itemMap.get("商品SKU")));
                        snapshot.setProductName(String.valueOf(itemMap.get("商品")));
                        snapshot.setBrand(String.valueOf(itemMap.get("品牌")));
                        snapshot.setRegion(String.valueOf(itemMap.get("地区")));
                        snapshot.setCompanyName(String.valueOf(itemMap.get("公司名称")));
                        snapshot.setSupplierName(String.valueOf(itemMap.get("供应商")));
                        snapshot.setStock(itemMap.get("库存") == null ? 0 : Integer.parseInt(String.valueOf(itemMap.get("库存"))));
                        snapshot.setPrice(new java.math.BigDecimal(String.valueOf(itemMap.get("价格"))));
                        snapshot.setSnapshotDate(LocalDate.now());
                        snapshot.setTaskId(taskId);
                        priceSnapshotMapper.insert(snapshot);
                    }
                }
                success++;
            } catch (Exception ex) {
                log.error("sync sku failed {}", sku, ex);
                fail++;
            }
            int progress = (int) (((i + 1) * 100.0) / skus.size());
            messagingTemplate.convertAndSend("/topic/task/" + taskNo,
                    new TaskProgressMessage(taskNo, "RUNNING", progress, skus.size(), success, fail, "同步中"));
        }

        SyncTask done = new SyncTask();
        done.setId(taskId);
        done.setStatus(fail > 0 ? "PARTIAL_SUCCESS" : "SUCCESS");
        done.setSuccessCount(success);
        done.setFailCount(fail);
        syncTaskMapper.updateById(done);
        messagingTemplate.convertAndSend("/topic/task/" + taskNo,
                new TaskProgressMessage(taskNo, done.getStatus(), 100, skus.size(), success, fail, "同步完成"));
    }
}
