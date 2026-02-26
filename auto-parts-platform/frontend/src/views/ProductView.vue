<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const products = ref([])
const keyword = ref('')

const fetchProducts = async () => {
  const { data } = await http.get('/products', { params: { keyword: keyword.value } })
  products.value = data
}

onMounted(fetchProducts)
</script>

<template>
  <el-card>
    <el-input v-model="keyword" placeholder="按 SKU/商品名称搜索" style="width: 280px; margin-right: 12px" />
    <el-button type="primary" @click="fetchProducts">查询</el-button>
    <el-table :data="products" style="margin-top: 12px">
      <el-table-column prop="sku" label="SKU" />
      <el-table-column prop="productName" label="商品" />
      <el-table-column prop="brand" label="品牌" />
      <el-table-column prop="status" label="状态" />
    </el-table>
  </el-card>
</template>
