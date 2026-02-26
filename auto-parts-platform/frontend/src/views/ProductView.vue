<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import http from '../api/http'

const products = ref([])
const latestRows = ref([])
const keyword = ref('')
const trendSku = ref('')
const chartRef = ref()
let chart

const fetchProducts = async () => {
  const { data } = await http.get('/products', { params: { keyword: keyword.value } })
  products.value = data
}

const fetchLatest = async () => {
  const { data } = await http.get('/products/latest')
  latestRows.value = data
}

const fetchTrend = async () => {
  if (!trendSku.value) return
  const { data } = await http.get(`/products/${trendSku.value}/price-trend`, { params: { days: 30 } })
  const legends = Object.keys(data.series || {})
  const xAxis = [...new Set(legends.flatMap(name => (data.series[name] || []).map(p => p.date)))]
  const series = legends.map(name => ({
    name,
    type: 'line',
    data: xAxis.map(date => {
      const point = (data.series[name] || []).find(p => p.date === date)
      return point ? point.price : null
    })
  }))
  await nextTick()
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: legends },
    xAxis: { type: 'category', data: xAxis },
    yAxis: { type: 'value' },
    series
  })
}

onMounted(async () => {
  await fetchProducts()
  await fetchLatest()
})
</script>

<template>
  <el-card>
    <el-input v-model="keyword" placeholder="按 SKU/商品名称搜索" style="width: 280px; margin-right: 12px" />
    <el-button type="primary" @click="fetchProducts">查询商品</el-button>
    <el-button @click="fetchLatest">查询最新爬取数据</el-button>

    <el-table :data="products" style="margin-top: 12px">
      <el-table-column prop="sku" label="SKU" />
      <el-table-column prop="productName" label="商品" />
      <el-table-column prop="brand" label="品牌" />
      <el-table-column prop="status" label="状态" />
    </el-table>

    <el-divider>最新爬取数据</el-divider>
    <el-table :data="latestRows">
      <el-table-column prop="sku" label="SKU" />
      <el-table-column prop="supplierName" label="供应商" />
      <el-table-column prop="price" label="价格" />
      <el-table-column prop="stock" label="库存" />
      <el-table-column prop="snapshotDate" label="日期" />
    </el-table>

    <el-divider>单商品价格历史折线图</el-divider>
    <el-input v-model="trendSku" placeholder="输入 SKU 查看30天价格走势" style="width: 280px; margin-right: 12px" />
    <el-button type="success" @click="fetchTrend">查询趋势</el-button>
    <div ref="chartRef" style="height: 360px; margin-top: 12px"></div>
  </el-card>
</template>
