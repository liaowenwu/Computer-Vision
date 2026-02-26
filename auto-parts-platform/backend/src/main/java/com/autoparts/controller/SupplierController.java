package com.autoparts.controller;

import com.autoparts.entity.Supplier;
import com.autoparts.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierMapper supplierMapper;

    @GetMapping
    public List<Supplier> list() { return supplierMapper.selectList(null); }

    @PostMapping
    public int save(@RequestBody Supplier supplier) { return supplierMapper.insert(supplier); }

    @PutMapping("/{id}")
    public int update(@PathVariable Long id, @RequestBody Supplier supplier) { supplier.setId(id); return supplierMapper.updateById(supplier); }
}
