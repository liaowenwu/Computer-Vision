# Auto Parts Platform

## 功能
- 前端页面触发本地爬虫任务（通过后端调用 `http://localhost:9001/run`）。
- WebSocket 实时推送任务日志与结果数据。
- PostgreSQL 记录每次爬取任务（`sync_task`）与日志（`task_log`）及价格快照（`price_snapshot`）。
- 查询最新爬取数据。
- 查询单商品价格历史，并在前端使用折线图展示。

## backend
```bash
cd backend
mvn spring-boot:run
```

## frontend
```bash
cd frontend
npm install
npm run dev
```

依赖服务：
- PostgreSQL: localhost:5432/autoparts
- Redis: localhost:6379
- Python Flask crawler: http://localhost:9001/run
