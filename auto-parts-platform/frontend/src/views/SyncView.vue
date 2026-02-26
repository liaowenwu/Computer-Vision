<script setup>
import { ref } from 'vue'
import http from '../api/http'

const skus = ref('SKU001,SKU002')
const taskNo = ref('')
const progress = ref(0)

const startBatch = async () => {
  const { data } = await http.post('/sync/price/batch', {
    skus: skus.value.split(',').map(v => v.trim()),
    triggerBy: 'admin'
  })
  taskNo.value = data.taskNo
  progress.value = 10
}

const queryTask = async () => {
  if (!taskNo.value) return
  const { data } = await http.get(`/sync/tasks/${taskNo.value}`)
  if (data?.totalCount > 0) {
    progress.value = Math.floor((data.successCount + data.failCount) * 100 / data.totalCount)
  }
}
</script>

<template>
  <el-card>
    <el-input v-model="skus" placeholder="逗号分隔SKU" />
    <div style="margin-top: 12px">
      <el-button type="primary" @click="startBatch">批量同步</el-button>
      <el-button @click="queryTask">查询任务</el-button>
    </div>
    <div style="margin-top: 12px">任务号：{{ taskNo }}</div>
    <el-progress :percentage="progress" />
  </el-card>
</template>
