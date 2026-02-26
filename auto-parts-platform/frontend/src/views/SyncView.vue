<script setup>
import { onBeforeUnmount, ref } from 'vue'
import http from '../api/http'
import SockJS from 'sockjs-client/dist/sockjs'
import { Client } from '@stomp/stompjs'

const skus = ref('SKU001,SKU002')
const taskNo = ref('')
const progress = ref(0)
const logs = ref([])
const taskList = ref([])
let stompClient

const connectTask = (id) => {
  if (stompClient) {
    stompClient.deactivate()
  }
  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws/sync'),
    reconnectDelay: 3000,
    onConnect: () => {
      stompClient.subscribe(`/topic/task/${id}`, (message) => {
        const payload = JSON.parse(message.body)
        progress.value = payload.progress || 0
        logs.value.push(`${payload.time} [${payload.level}] ${payload.sku || ''} ${payload.message}`)
      })
    }
  })
  stompClient.activate()
}

const startBatch = async () => {
  logs.value = []
  const { data } = await http.post('/sync/price/batch', {
    skus: skus.value.split(',').map(v => v.trim()).filter(Boolean),
    triggerBy: 'admin'
  })
  taskNo.value = data.taskNo
  progress.value = 0
  connectTask(taskNo.value)
}

const loadTasks = async () => {
  const { data } = await http.get('/sync/tasks')
  taskList.value = data
}

const loadTaskLogs = async (id) => {
  taskNo.value = id
  const { data } = await http.get(`/sync/tasks/${id}/logs`)
  logs.value = data.map(i => `${i.createdAt} [${i.level}] ${i.sku || ''} ${i.message}`)
  connectTask(id)
}

onBeforeUnmount(() => {
  if (stompClient) stompClient.deactivate()
})
</script>

<template>
  <el-card>
    <el-input v-model="skus" placeholder="逗号分隔SKU" />
    <div style="margin-top: 12px">
      <el-button type="primary" @click="startBatch">批量同步（调用本地客户端爬虫）</el-button>
      <el-button @click="loadTasks">刷新任务列表</el-button>
    </div>
    <div style="margin-top: 12px">任务号：{{ taskNo }}</div>
    <el-progress :percentage="progress" />

    <el-divider>任务日志</el-divider>
    <el-scrollbar height="220px">
      <div v-for="(line, idx) in logs" :key="idx" style="font-family: monospace; margin-bottom: 6px">{{ line }}</div>
    </el-scrollbar>

    <el-divider>历史任务</el-divider>
    <el-table :data="taskList">
      <el-table-column prop="taskNo" label="任务号" />
      <el-table-column prop="status" label="状态" width="140" />
      <el-table-column prop="successCount" label="成功" width="100" />
      <el-table-column prop="failCount" label="失败" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" @click="loadTaskLogs(scope.row.taskNo)">查看日志</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
