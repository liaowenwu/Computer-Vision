package com.autoparts.controller;

import com.autoparts.entity.PriceSnapshot;
import com.autoparts.entity.Product;
import com.autoparts.mapper.PriceSnapshotMapper;
import com.autoparts.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductMapper productMapper;
    private final PriceSnapshotMapper priceSnapshotMapper;

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Product::getSku, keyword).or().like(Product::getProductName, keyword);
        }
        return productMapper.selectList(wrapper);
    }

    @PostMapping
    public int save(@RequestBody Product product) {
        return productMapper.insert(product);
    }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        return productMapper.updateById(product);
    }

    @GetMapping("/latest")
    public List<PriceSnapshot> latest(@RequestParam(required = false) String sku) {
        LambdaQueryWrapper<PriceSnapshot> wrapper = new LambdaQueryWrapper<>();
        if (sku != null && !sku.isBlank()) {
            wrapper.eq(PriceSnapshot::getSku, sku);
        }
        wrapper.inSql(PriceSnapshot::getId,
                "select max(id) from price_snapshot group by sku, supplier_name")
                .orderByDesc(PriceSnapshot::getSnapshotTime);
        return priceSnapshotMapper.selectList(wrapper);
    }

    @GetMapping("/{sku}/price-trend")
    public Map<String, Object> priceTrend(@PathVariable String sku,
                                          @RequestParam(defaultValue = "30") int days) {
        LocalDate start = LocalDate.now().minusDays(days);
        List<PriceSnapshot> snapshots = priceSnapshotMapper.selectList(new LambdaQueryWrapper<PriceSnapshot>()
                .eq(PriceSnapshot::getSku, sku)
                .ge(PriceSnapshot::getSnapshotDate, start)
                .orderByAsc(PriceSnapshot::getSnapshotDate));

        Map<String, List<Map<String, Object>>> series = snapshots.stream()
                .collect(Collectors.groupingBy(
                        PriceSnapshot::getSupplierName,
                        LinkedHashMap::new,
                        Collectors.mapping(item -> Map.of(
                                "date", item.getSnapshotDate(),
                                "price", item.getPrice()
                        ), Collectors.toList())
                ));

        return Map.of(
                "sku", sku,
                "days", days,
                "series", series
        );
    }
}
