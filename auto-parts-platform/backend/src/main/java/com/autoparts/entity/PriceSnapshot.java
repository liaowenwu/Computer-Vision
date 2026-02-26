package com.autoparts.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("price_snapshot")
public class PriceSnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sku;
    private String productName;
    private String brand;
    private String region;
    private String companyName;
    private String supplierName;
    private Integer stock;
    private BigDecimal price;
    private LocalDate snapshotDate;
    private Long taskId;
}
