package com.autoparts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PythonPriceItem {
    @JsonProperty("商品SKU")
    private String sku;
    @JsonProperty("商品")
    private String productName;
    @JsonProperty("品牌")
    private String brand;
    @JsonProperty("地区")
    private String region;
    @JsonProperty("公司名称")
    private String companyName;
    @JsonProperty("供应商")
    private String supplierName;
    @JsonProperty("库存")
    private Integer stock;
    @JsonProperty("价格")
    private BigDecimal price;
}
