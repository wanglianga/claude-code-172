# 城市社区配送机器人进楼门禁避障签收服务

外卖、药品、商超订单交给社区配送机器人后，服务统一编排**门禁授权 → 园区避障 → 进楼 → 电梯协同选层 → 到达通知 → 签收/退回 → 异常客服介入**全流程。

机器人在社区里不是孤立设备：本服务把它与**门禁系统、电梯系统、物业规则、用户取件、人工接力**协同起来，并把每一次设备动作与业务结果写入**同一条配送事件流**（同表、同订单、连续序号），同时投递到 Kafka `delivery-events` 主题，供物业、运营、客服三端使用。

技术栈：Java 17 · Spring Boot 3.2 · Spring Security · Spring Data JPA · PostgreSQL 16 · Kafka 3.7（KRaft）· springdoc-openapi（Swagger UI）· JUnit/MockMvc。

---

## 1. 能力与需求覆盖

订单要素：订单类型（外卖/药品/商超）、收货楼栋/单元/楼层/房号、门禁授权、是否冷藏、预计送达、机器人电量（接单快照）、载物仓编号、用户手机号/小程序标识。

| # | 题目要求的异常/规则 | 处理方式 | 事件类型 / 责任方 |
|---|---|---|---|
| 1 | 物业门禁拒绝 | 物业审批拒绝 → 订单退回 | `GATE_REJECTED` / 物业 |
| 2 | 老人儿童挡路 | 原地避障停留（不催行、礼让居民），有备用路线则绕行，行人通过后恢复 | `OBSTACLE_DETECTED`→`OBSTACLE_WAIT`→`ROUTE_ADJUSTED` / 外部因素 |
| 3 | 电梯满载 | 礼让居民等下一班，记录满载次数与等待时长，再乘梯 | `ELEVATOR_FULL` / 电梯系统 |
| 4 | 楼道施工 | 读取物业施工通告，有绕行则改道，无绕行转人工步行接力 | `CONSTRUCTION_ENCOUNTERED`→`ROUTE_ADJUSTED`/`STAFF_HANDOVER` / 物业 |
| 5 | 电量不足 | 低于阈值就近安全停靠、呼叫人工接力 | `LOW_BATTERY` / 机器人设备 |
| 6 | 用户超时不取 | 半程提醒一次；超 15 分钟未取 → 短信告知并退回 | `PICKUP_REMINDER`→`PICKUP_TIMEOUT`→`RETURN_STARTED` / 用户 |
| 7 | 载物仓打不开 | 报修、转人工现场开箱交付 | `BIN_OPEN_FAILED`→`STAFF_HANDOVER` / 机器人设备 |
| 8 | 冷藏订单超时 | 冷链时限（默认 10 分钟，短于取件超时）优先升级人工，防止变质 | `COLD_CHAIN_TIMEOUT` / 机器人设备 |
| 9 | 投诉送错楼栋 | 客服判责、修正正确楼栋/楼层，在途单二次配送 | `COMPLAINT_FILED`→`CUSTOMER_SERVICE_INTERVENED`→`ROUTE_ADJUSTED` / 机器人设备 |
| 10 | 物业限制夜间运行 | 小区级夜间禁行窗口，禁行时段门禁拒绝并记录，窗口外自动恢复；物业可随时改规则 | `NIGHT_RESTRICTION`+`GATE_REJECTED` / 物业 |

**统一配送事件流**：门禁开通、路线调整、电梯请求、避障停留、签收、退回、客服介入等全部动作，都以 `(orderId, seq)` 单调递增追加到 `delivery_event` 表（数据库唯一约束兜底），每条事件同时携带：动作类型、触发方、位置（门口/园区/楼栋/电梯/户门）、业务结果说明、**等待/绕行耗时**、**是否占用居民通道**、当时电量、订单状态快照、责任方。设备动作与业务结果在同一事务落库，事务提交后再发布 Kafka。

三端视角：

- **物业端**：审批门禁；维护准入/夜间规则；发布撤销施工通告；通行影响看板（占道事件数、总占用时长、按楼栋分布、门禁拒绝/夜间限制/避障/电梯满载/施工分类计数）。
- **运营端**：按**小区 / 楼栋 / 订单类型**聚合单量、履约率（签收率）、退回量、异常率、冷藏超时、准时率，评估机器人投放效率。
- **客服端**：受理投诉工单，依据该订单完整事件时间线追踪并判定异常责任，登记处理结论；送错楼栋可一键修正地址触发二次配送。

---

## 2. 一键启动（Docker Compose，推荐）

需要本机 Docker。该命令启动 PostgreSQL、Kafka（KRaft 模式，无需 ZooKeeper）和应用，并自动建表、初始化演示数据：

```bash
docker compose up --build
```

- 应用 / Swagger UI： http://localhost:8080/swagger-ui.html
- OpenAPI JSON： http://localhost:8080/v3/api-docs
- PostgreSQL：localhost:5432（库/用户/密码 = `delivery` / `delivery` / `delivery123`）
- Kafka：localhost:9092，topic `delivery-events`（3 分区，按订单号分区保证同一事件流有序）

停止并清理：

```bash
docker compose down -v
```

---

## 3. 本地开发启动（无需 Docker / PostgreSQL / Kafka）

不依赖外部中间件，使用 H2 内存库 + 进程内事件总线（下游分析/通知 Sink 行为与 Kafka 模式一致）：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
# 或
mvn package -DskipTests
java -jar target/community-delivery.jar --spring.profiles.active=local
```

连接本机真实 PostgreSQL/Kafka（默认配置即指向 localhost）：

```bash
mvn spring-boot:run
```

运行测试（H2 + 本地事件总线，自动执行）：

```bash
mvn test
```

---

## 4. 测试账号（HTTP Basic，在 Swagger 右上角 Authorize 填入）

| 角色 | 用户名 | 密码 | 可访问端 |
|---|---|---|---|
| 物业 | `property` | `property123` | `/api/property/**` |
| 运营 | `operator` | `operator123` | `/api/operator/**` |
| 客服 | `support` | `support123` | `/api/support/**` |
| 机器人/业务系统 | `robot` | `robot123` | `/api/robot/**` |

订单与事件流 `/api/orders/**`、基础数据 `/api/meta/**` 对任意登录角色开放（三端都需核查）。Swagger 与 OpenAPI 文档免鉴权。

演示数据：小区「阳光花园」(id=1，门禁需人工审批)、「河畔家园」(id=2，门禁免审批、已设 21:30–06:30 夜间禁行)；机器人 R01（96% 电、2 冷藏仓）、R02（58%、1 冷藏仓）、R03（15%、充电中）；6 号楼一条施工通告。

---

## 5. API 导览（curl）

一次完整配送（机器人端动作 + 物业审批）：

```bash
B=http://localhost:8080

# ① 下冷藏药品单
NO=$(curl -s -u robot:robot123 -H 'Content-Type: application/json' -d '{
  "orderType":"MEDICINE","communityId":1,"targetBuilding":"6号楼","unit":"2单元",
  "targetFloor":5,"room":"502","coldChain":true,
  "userName":"赵大爷","userPhone":"13700000002"}' $B/api/robot/orders | python3 -c 'import sys,json;print(json.load(sys.stdin)["orderNo"])')

# ② 分配机器人（自动选电量最充足的空闲机器人，分配冷藏仓）
curl -s -u robot:robot123 -X POST $B/api/robot/orders/$NO/assign

# ③ 到小区门口申请门禁
RN=$(curl -s -u robot:robot123 -X POST -H 'Content-Type: application/json' \
  -d '{"gatePoint":"南门人行门"}' $B/api/robot/orders/$NO/gate-request \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["requestNo"])')

# ④ 物业审批（拒绝传 {"decision":"REJECTED","reason":"..."} 即触发门禁拒绝->退回）
curl -s -u property:property123 -X POST -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVED","reason":"药品优先放行"}' \
  $B/api/property/gate-requests/$RN/decision

# ⑤ 进门、遇老人儿童挡路避让绕行、进楼
curl -s -u robot:robot123 -X POST $B/api/robot/orders/$NO/pass-gate
curl -s -u robot:robot123 -X POST -H 'Content-Type: application/json' \
  -d '{"obstacleType":"ELDERLY_CHILD","location":"COMMUNITY","waitSeconds":30,"detourAvailable":true,"detail":"老人推婴儿车"}' \
  $B/api/robot/orders/$NO/obstacle
curl -s -u robot:robot123 -X POST $B/api/robot/orders/$NO/enter-building

# ⑥ 电梯协同（满载礼让）、乘梯、到达楼层 -> 自动短信/小程序通知取件
curl -s -u robot:robot123 -X POST $B/api/robot/orders/$NO/elevator-request
curl -s -u robot:robot123 -X POST -H 'Content-Type: application/json' -d '{"waitSeconds":40}' \
  $B/api/robot/orders/$NO/elevator-full
curl -s -u robot:robot123 -X POST $B/api/robot/orders/$NO/elevator-board
curl -s -u robot:robot123 -X POST $B/api/robot/orders/$NO/arrive-floor

# ⑦ 用户签收
curl -s -u robot:robot123 -X POST -H 'Content-Type: application/json' -d '{"signerName":"赵大爷"}' \
  $B/api/robot/orders/$NO/sign
```

其余异常动作（均为 `POST /api/robot/orders/{orderNo}/...`）：

- 楼道施工 `.../construction`，body `{"detourAvailable":false,"note":"一层封路"}`（false→人工接力）
- 低电量 `.../battery`，body `{"batteryPercent":12}`（自动人工接力）
- 仓门故障 `.../bin-failure`；人工接力 `.../staff-takeover`；人工代签收 `.../staff-sign`
- 开始/完成退回 `.../return`、`.../returned`；手动触发超时扫描 `POST /api/robot/timeouts/scan`
  （生产环境由定时任务每 60 秒自动扫描；测试时可拨时间或直接等待）

客服投诉送错楼栋并二次配送：

```bash
TN=$(curl -s -u support:support123 -H 'Content-Type: application/json' -d "{
  \"orderNo\":\"$NO\",\"type\":\"WRONG_BUILDING\",
  \"description\":\"实际住5号楼却送到6号楼\",\"complainantPhone\":\"13700000002\"}" \
  $B/api/support/tickets | python3 -c 'import sys,json;print(json.load(sys.stdin)["ticketNo"])')

curl -s -u support:support123 -X POST -H 'Content-Type: application/json' -d '{
  "responsibility":"ROBOT","resolution":"楼栋识别错误，二次配送",
  "correctBuilding":"5号楼","correctFloor":6}' $B/api/support/tickets/$TN/resolve
```

物业与运营：

```bash
# 限制夜间运行（22:00-次日06:00）
curl -s -u property:property123 -X PUT -H 'Content-Type: application/json' -d '{
  "robotAllowed":true,"nightRestricted":true,"nightStart":"22:00","nightEnd":"06:00",
  "gateManualApproval":true,"remark":"业主大会决议"}' $B/api/property/communities/1/rule

# 通行影响看板 / 施工通告
curl -s -u property:property123 $B/api/property/communities/1/passage-impact
curl -s -u property:property123 "$B/api/property/constructions?communityId=1"

# 运营效率（小区 / 楼栋 / 订单类型）
curl -s -u operator:operator123 $B/api/operator/efficiency/by-community
curl -s -u operator:operator123 "$B/api/operator/efficiency/by-building?communityId=1"
curl -s -u operator:operator123 $B/api/operator/efficiency/by-order-type

# 任一角色查看同一订单完整事件时间线与通知记录
curl -s -u support:support123 $B/api/orders/$NO/events
curl -s -u support:support123 $B/api/orders/$NO/notifications
```

完整接口清单与字段见 Swagger UI（39 个路径，按「机器人/配送端、物业端、运营端、客服端、订单与事件流、基础数据」分组）。

---

## 6. 数据模型

- `delivery_order` 配送订单（聚合根，含全部订单要素、状态机、异常/冷链标记、责任方）
- `delivery_event` **统一配送事件流**（append-only，`(order_id,seq)` 唯一）
- `gate_authorization` 门禁申请与审批（支持夜间退回后次日重新申请）
- `elevator_request` 电梯协同请求（目标楼层、满载重试次数）
- `property_rule` 小区物业规则（准入、夜间窗口、是否人工审批）
- `construction_notice` 楼道施工通告（时间窗、可撤销）
- `robot` 机器人台账（状态、电量、冷藏仓能力）、`community` 小区
- `notification_record` 短信/小程序通知留痕、`support_ticket` 客服工单与责任结论

订单状态机：`CREATED → GATE_PENDING → IN_COMMUNITY → IN_BUILDING → ELEVATOR_WAITING → ARRIVED_WAITING_PICKUP → PICKED_UP`；异常可转 `EXCEPTION_HANDOVER`（人工接力后仍可签收）或 `RETURNING → RETURNED`。

Kafka 链路：业务事务提交后发布 `delivery-events`（key=订单号）→ 消费者广播给 `DeliveryEventSink`（事件指标投影、短信/小程序通知网关）。发送失败标记 `published=false`，由补偿任务每 30 秒重发，保证最终送达。

---

## 7. 验证方式与**实际验证结论**

以下均为在本工作区实际执行过的验证（日期 2026-10-06）：

1. **编译与单元/集成测试**：`mvn test` → `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0 / BUILD SUCCESS`。
   - `DeliveryOrchestratorIntegrationTest`（16 项，H2 + 本地事件总线）覆盖：正常全流程、9 类异常、夜间限制次日恢复、物业改规则即时生效、三端统计聚合、通行影响看板、非法状态转移拒绝、出发前更正楼栋；并断言事件流序号连续、设备动作与业务结果同事件、等待时长/占道标记/责任方正确、短信留痕、事件被消息总线 Sink 消费。
   - `WebApiIntegrationTest`（5 项，MockMvc）覆盖：Swagger/OpenAPI 公开可访问、匿名 401、角色越权 403、错误密码 401、HTTP 端到端下单→物业审批→电梯→签收→客服查事件流、参数校验 400。
2. **打包启动冒烟**：`mvn package` 生成 `target/community-delivery.jar`；以 `local` profile 实际启动成功（`Started DeliveryApplication`，Tomcat 8080）。
   - `GET /swagger-ui/index.html` → 200；`GET /v3/api-docs` → OpenAPI 3.0.1，**39 个 API 路径**。
   - 匿名访问受保护接口 → 401。
3. **真实 HTTP 全流程手测**（curl，结果已核对）：
   - 冷藏药品单分配到 `R01-COLD-1` 冷藏仓，电量快照 96%；物业审批门禁 PENDING→GRANTED；老人儿童挡路产生 `OBSTACLE_WAIT`（30000ms、占道、责任=外部因素）并绕行；电梯满载产生 `ELEVATOR_FULL`（40000ms、占道、责任=电梯系统）；最终 PICKED_UP，事件流 16 条序号连续，到达短信已留痕并经通知网关 Sink 投递。
   - 运营三视角聚合返回正确（按小区/6 号楼/药品类型，履约率 100%）；物业通行看板显示占道事件 3 起、总占用 70000ms 并按楼栋归集。
   - 河畔家园免人工审批白天 GRANTED；物业临时设置覆盖当前时段的夜间窗口后门禁立即 REJECTED，事件流含 `NIGHT_RESTRICTION`+`GATE_REJECTED`。
   - 客服「送错楼栋」工单判责 ROBOT、修正为 5 号楼 6 层后，订单回到 IN_COMMUNITY 二次配送，事件流含投诉、客服介入、路线调整三条记录。

> 说明：当前环境无 Docker 守护进程（`docker` 二进制缺失），故 `Dockerfile`/`docker-compose.yml` 未在本机实际构建运行；其配置为标准 Spring Boot 分层构建 + 官方 `postgres:16-alpine`、`apache/kafka:3.7.0` 镜像并带健康检查，应用已在等价的 local profile（H2）下验证可正常启动与运行。

---

## 8. 关键配置（application.yml）

| 配置 | 默认 | 说明 |
|---|---|---|
| `app.rules.pickup-timeout-seconds` | 900 | 用户超时不取阈值 |
| `app.rules.cold-chain-timeout-seconds` | 600 | 冷藏等待超时（短于取件超时，优先升级人工） |
| `app.rules.low-battery-threshold` | 20 | 低电量阈值（%） |
| `app.rules.night-start/end` | 22:00 / 06:00 | 全局夜间窗口（可被小区规则覆盖） |
| `app.kafka.enabled` | true | 置 false 时使用进程内本地事件总线（local/test profile） |
| `app.kafka.topic` | delivery-events | 统一事件主题 |
