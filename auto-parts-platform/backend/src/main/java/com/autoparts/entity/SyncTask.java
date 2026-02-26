package com.autoparts.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sync_task")
public class SyncTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private String taskType;
    private String triggerBy;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String errorMessage;
}
