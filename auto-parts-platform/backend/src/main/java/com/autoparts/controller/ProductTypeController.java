package com.autoparts.controller;

import com.autoparts.entity.ProductType;
import com.autoparts.mapper.ProductTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-types")
@RequiredArgsConstructor
public class ProductTypeController {
    private final ProductTypeMapper productTypeMapper;

    @GetMapping
    public List<ProductType> list() { return productTypeMapper.selectList(null); }

    @PostMapping
    public int save(@RequestBody ProductType type) { return productTypeMapper.insert(type); }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody ProductType type) { type.setId(id); return productTypeMapper.updateById(type); }

    @DeleteMapping("/{id}")
    public int delete(@PathVariable Long id) { return productTypeMapper.deleteById(id); }
}
