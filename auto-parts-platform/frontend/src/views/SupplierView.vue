<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const suppliers = ref([])
const fetchSuppliers = async () => {
  const { data } = await http.get('/suppliers')
  suppliers.value = data
}
onMounted(fetchSuppliers)
</script>

<template>
  <el-card>
    <el-table :data="suppliers">
      <el-table-column prop="supplierName" label="供应商" />
      <el-table-column prop="companyName" label="公司" />
      <el-table-column prop="region" label="地区" />
    </el-table>
  </el-card>
</template>
