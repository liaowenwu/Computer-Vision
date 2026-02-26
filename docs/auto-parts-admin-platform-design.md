# 汽车配件爬虫后台管理平台设计方案

## 1. 目标与范围

### 1.1 业务目标
- 统一管理汽车配件商品、商品类型、供应商和价格数据。
- 支持手动单个同步和批量同步价格。
- 通过 WebSocket 实时展示爬虫同步进度与结果。
- 保存每次同步的价格快照，支持商品价格走势分析。

### 1.2 技术栈
- 后端：Spring Boot + MyBatis-Plus
- 数据库：PostgreSQL
- 缓存：Redis
- 实时通信：WebSocket
- 爬虫客户端：Python Flask（本地 HTTP 接口）
- 前端：Vue3 + Element Plus + ECharts

---

## 2. 总体架构

```text
Vue3 管理端
  │
  │ HTTP / WebSocket
  ▼
Spring Boot 管理服务
  ├─ 商品/供应商/类型管理 API
  ├─ 同步任务编排（单个/批量）
  ├─ 调用 Python Flask: POST http://localhost:9001/run
  ├─ WebSocket 推送进度与结果
  ├─ Redis（任务状态、去重锁、热点缓存）
  └─ PostgreSQL（主数据 + 价格历史）

Python Flask 爬虫服务
  └─ 返回字段：['商品SKU','商品','品牌','地区','公司名称','供应商','库存','价格']
```

核心思想：
1. **主数据与历史数据分离**：商品基础信息与价格快照分表。
2. **同步任务异步执行**：前端触发任务后立即返回任务号，WebSocket 监听进度。
3. **幂等入库**：按 `sku + supplier + sync_date` 约束避免重复写入。

---

## 3. 核心模块设计

## 3.1 商品类型模块
功能：
- 商品类型的增删改查（树形/平级都可）。
- 类型编码唯一，用于商品归类与过滤。

建议字段：
- `id`, `type_code`, `type_name`, `parent_id`, `sort_no`, `status`, `created_at`, `updated_at`

## 3.2 商品列表模块
功能：
- 商品主数据管理（SKU、名称、品牌、类型等）。
- 展示商品对应的多个供应商实时价格（最新价）。
- 支持多维筛选：SKU、商品名、品牌、地区、供应商、价格区间、库存区间、更新时间。

## 3.3 供应商列表模块
功能：
- 管理供应商基础信息。
- 查看单个供应商下全部商品及价格。
- 支持供应商维度统计（在售商品数、平均价格、最近同步时间）。

## 3.4 同步任务模块
功能：
- 单个同步：按单 SKU/单商品触发。
- 批量同步：按商品列表、供应商列表、类型或全部触发。
- 任务可追踪：开始时间、结束时间、成功/失败数、失败原因。

---

## 4. 数据库模型（PostgreSQL）

> 以下为建议表结构，可根据现有命名规范调整。

### 4.1 主数据表

1) `product_type` 商品类型表  
2) `product` 商品表  
3) `supplier` 供应商表  
4) `product_supplier` 商品-供应商关系表（可扩展供应商商品编码）

### 4.2 价格历史与任务表

5) `sync_task` 同步任务表
- 记录任务维度（single/batch）、触发人、状态、统计信息。

6) `price_snapshot` 价格快照表（重点）
- 每次同步结果逐条入库。
- 建议唯一键：`(sku, supplier_name, snapshot_date)`。
- 建议索引：`sku`, `supplier_name`, `snapshot_time DESC`。

### 4.3 推荐 DDL（简化版）

```sql
create table product (
  id bigserial primary key,
  sku varchar(64) not null unique,
  product_name varchar(255) not null,
  brand varchar(128),
  type_id bigint,
  status smallint default 1,
  created_at timestamp not null default now(),
  updated_at timestamp not null default now()
);

create table supplier (
  id bigserial primary key,
  supplier_name varchar(255) not null,
  company_name varchar(255),
  region varchar(128),
  status smallint default 1,
  created_at timestamp not null default now(),
  updated_at timestamp not null default now()
);

create table price_snapshot (
  id bigserial primary key,
  sku varchar(64) not null,
  product_name varchar(255) not null,
  brand varchar(128),
  region varchar(128),
  company_name varchar(255),
  supplier_name varchar(255) not null,
  stock integer,
  price numeric(12,2) not null,
  snapshot_time timestamp not null default now(),
  snapshot_date date not null,
  task_id bigint,
  raw_payload jsonb,
  created_at timestamp not null default now(),
  constraint uk_snapshot unique (sku, supplier_name, snapshot_date)
);

create table sync_task (
  id bigserial primary key,
  task_no varchar(64) not null unique,
  task_type varchar(16) not null,
  trigger_by varchar(64),
  status varchar(16) not null,
  total_count integer default 0,
  success_count integer default 0,
  fail_count integer default 0,
  error_message text,
  started_at timestamp,
  finished_at timestamp,
  created_at timestamp not null default now()
);
```

---

## 5. 同步流程设计（关键）

## 5.1 单个/批量同步流程
1. 前端调用 `POST /api/sync/price` 或 `POST /api/sync/price/batch`。
2. 后端创建 `sync_task`，状态置为 `RUNNING`，立即返回 `taskNo`。
3. 后端异步线程池按商品维度调用 Python Flask：
   - `POST http://localhost:9001/run`
   - `Content-Type: application/json`
4. Flask 返回列表数据后，后端执行：
   - 字段校验与类型转换；
   - 供应商与商品主数据补全（不存在可按策略自动创建或记失败）；
   - upsert 写入 `price_snapshot`；
   - 更新商品“最新价缓存”（Redis）。
5. 每处理一批，WebSocket 推送任务进度。
6. 任务完成后更新 `sync_task` 为 `SUCCESS`/`PARTIAL_SUCCESS`/`FAILED`。

## 5.2 幂等与一致性
- 使用 PostgreSQL `ON CONFLICT` 实现日粒度去重。
- 任务级 Redis 锁（如 `sync:lock:{sku}`）防止并发重复抓取。
- 失败重试：网络异常可重试 2~3 次，业务脏数据直接记录失败。

---

## 6. API 设计建议

## 6.1 商品类型
- `GET /api/product-types`
- `POST /api/product-types`
- `PUT /api/product-types/{id}`
- `DELETE /api/product-types/{id}`

## 6.2 商品
- `GET /api/products`（支持多条件分页查询）
- `POST /api/products`
- `PUT /api/products/{id}`
- `GET /api/products/{id}/suppliers`（查看该商品下所有供应商价格）
- `GET /api/products/{id}/price-trend?days=30`

## 6.3 供应商
- `GET /api/suppliers`
- `POST /api/suppliers`
- `PUT /api/suppliers/{id}`
- `GET /api/suppliers/{id}/products`

## 6.4 同步
- `POST /api/sync/price`（单个）
- `POST /api/sync/price/batch`（批量）
- `GET /api/sync/tasks/{taskNo}`（查询任务状态）

---

## 7. WebSocket 设计

## 7.1 连接与订阅
- 连接地址：`/ws/sync`
- 订阅主题：`/topic/task/{taskNo}`

## 7.2 消息结构

```json
{
  "taskNo": "SYNC202601010001",
  "status": "RUNNING",
  "progress": 60,
  "total": 100,
  "success": 58,
  "fail": 2,
  "message": "正在同步供应商A...",
  "time": "2026-01-01T10:30:00"
}
```

---

## 8. 前端页面规划（Vue3）

1. **商品类型管理页**
- 左树右表（可选）
- 支持启停状态

2. **商品列表页（核心）**
- 顶部多维筛选栏
- 表格列：SKU、商品、品牌、最低价、最高价、供应商数、最近同步时间
- 行操作：查看详情、单个同步
- 批量操作：勾选后批量同步

3. **供应商管理页**
- 供应商基础信息
- 供应商商品列表及价格

4. **商品详情页**
- 基础信息
- 供应商价格对比表
- 价格趋势图（按日）

5. **同步任务页**
- 展示任务列表、状态、耗时、失败原因
- 任务详情可查看失败记录

---

## 9. 价格走势图设计

数据来源：`price_snapshot`
- X 轴：日期（`snapshot_date`）
- Y 轴：价格（`price`）
- Series：供应商维度（每个供应商一条线）

接口建议：
- `GET /api/products/{id}/price-trend?startDate=2026-01-01&endDate=2026-01-31`

返回示例：

```json
{
  "sku": "SKU001",
  "productName": "刹车片",
  "series": [
    {
      "supplier": "供应商A",
      "points": [
        {"date":"2026-01-01","price":120.5},
        {"date":"2026-01-02","price":118.0}
      ]
    }
  ]
}
```

---

## 10. 关键实现建议（Spring Boot）

- 使用 `@Async + ThreadPoolTaskExecutor` 执行批量同步。
- 通过 `WebClient` 或 `RestTemplate` 调用 Flask 接口。
- 使用 MyBatis-Plus 的 `saveBatch` + 自定义 mapper 实现 `upsert`。
- 将“最新价”放 Redis：`product:latest-price:{sku}`，提高列表页响应。
- 为同步任务增加审计字段（操作者、触发来源、请求参数快照）。

---

## 11. 分阶段落地计划

### Phase 1（最小可用）
- 完成商品/供应商/类型 CRUD
- 完成单个同步 + 入库 + 任务记录
- 完成商品详情价格历史查询

### Phase 2
- 完成批量同步
- 接入 WebSocket 实时进度
- 完成商品列表多维检索与分页优化

### Phase 3
- 价格趋势图、供应商对比
- 失败重试、告警、报表统计
- 权限与操作日志

---

## 12. 风险与注意事项

- Flask 返回字段可能不稳定：建议做版本化协议与字段映射。
- 价格字段需统一币种与小数精度。
- 同步高峰可能导致数据库写入压力，建议批量写与合理索引。
- 如果跨时区部署，`snapshot_date` 计算应统一时区（建议 Asia/Shanghai）。

