package com.autoparts.controller;

import com.autoparts.entity.Product;
import com.autoparts.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductMapper productMapper;

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
}
