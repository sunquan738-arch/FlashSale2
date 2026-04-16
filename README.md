# Flash Sale 秒杀商城（flash-sale-server + flash-sale-web）

## 1. 项目简介和架构图（文字描述）

本项目是一个前后端分离的秒杀商城示例：
- 后端：`flash-sale-server`（Java 17 + Spring Boot 3）
- 前端：`flash-sale-web`（Vue3 + Vite + Element Plus）

核心链路采用 **Redis + Lua + RabbitMQ + MySQL** 组合，目标是在高并发场景下实现：
- 防超卖
- 防重复下单
- 异步削峰
- 最终一致

架构文字图如下：

1. 用户在前端发起秒杀请求  
2. API 网关层（当前为单体 Controller）完成 JWT 鉴权、限流校验  
3. Service 调用 Redis Lua 脚本做原子预扣（库存校验 + 去重）  
4. 预扣成功后发送 RabbitMQ 消息，接口立即返回“已提交”  
5. MQ 消费者异步落库（幂等校验、扣 DB 库存、创建订单）  
6. 前端轮询秒杀结果接口，获取“排队中 / 成功 / 失败”

---

## 2. 技术选型说明

### 后端（flash-sale-server）
- Spring Boot 3：应用基础框架
- MyBatis-Plus：快速 CRUD + 乐观锁插件
- MySQL 8：订单与活动持久化
- Redis：秒杀预扣、限流、结果缓存
- RabbitMQ：削峰填谷、异步下单
- JWT：无状态登录鉴权
- AOP：统一接口日志（耗时/入参/出参）

### 前端（flash-sale-web）
- Vue 3 + Vite：高效开发与构建
- Vue Router 4：页面路由与鉴权守卫
- Pinia：用户态与购物车状态管理
- Axios：接口请求封装（自动携带 JWT，401 自动跳转登录）
- Element Plus：UI 组件库

---

## 3. 快速启动步骤（含 MySQL / Redis / RabbitMQ 初始化）

## 3.1 基础环境
- JDK 17
- Maven 3.9+
- Node.js 18+
- MySQL 8.x
- Redis 6.x+
- RabbitMQ 3.x+

## 3.2 初始化数据库
1. 创建数据库 `flash_sale`
2. 执行：
   - `flash-sale-server/src/main/resources/sql/schema.sql`
   - `flash-sale-server/src/main/resources/sql/alter-product-version.sql`
   - `flash-sale-server/src/main/resources/sql/init-data.sql`

## 3.3 启动后端
```bash
cd flash-sale-server
mvn spring-boot:run
```
默认地址：`http://localhost:8080`

## 3.4 启动前端
```bash
cd flash-sale-web
npm install
npm run dev
```
默认地址：`http://localhost:5173`

---

## 4. 核心设计亮点（秒杀链路、防超卖、防重单）

1. **Redis Lua 原子脚本**  
   在单条脚本中完成“是否重复下单 + 是否有库存 + 扣库存 + 下单标记”，避免并发穿透。

2. **异步削峰（RabbitMQ）**  
   秒杀请求快速返回，真实订单创建走 MQ 消费，降低接口 RT，提升吞吐。

3. **双重幂等保障**  
   - Redis 用户下单标记  
   - MySQL `seckill_order(user_id, product_id, activity_id)` 唯一索引

4. **防超卖保障**  
   - Redis 预扣库存  
   - 数据库活动库存二次扣减  
   - 商品库存乐观锁（`version` 字段）

5. **结果缓存 + 轮询查询**  
   秒杀结果写入 Redis，前端按固定频率轮询，业务反馈更实时。

6. **接口级限流**  
   同一用户同一接口 10 秒最多 5 次，超限直接返回 429。

---

## 5. 接口文档速查表

| 模块 | 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|---|
| 认证 | POST | `/api/auth/login` | 用户登录，返回 token | 否 |
| 商品 | GET | `/api/products` | 商品列表 | 否 |
| 商品 | GET | `/api/products/{id}` | 商品详情 | 否 |
| 秒杀 | GET | `/api/seckill/activities` | 秒杀活动列表 | 否 |
| 秒杀 | GET | `/api/seckill/activities/{id}` | 秒杀活动详情 | 否 |
| 秒杀 | POST | `/api/seckill/do/{activityId}` | 发起秒杀请求 | 是 |
| 秒杀 | GET | `/api/seckill/result/{activityId}` | 查询秒杀结果 | 是 |
| 订单 | GET | `/api/orders/my` | 我的订单列表 | 是 |

鉴权头：
```http
Authorization: Bearer {token}
```

