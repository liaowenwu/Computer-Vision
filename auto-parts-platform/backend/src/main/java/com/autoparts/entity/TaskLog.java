package com.autoparts.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_log")
public class TaskLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String taskNo;
    private String level;
    private String message;
    private String sku;
    private LocalDateTime createdAt;
}
