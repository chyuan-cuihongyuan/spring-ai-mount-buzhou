---
Type: task
Status: closed
---
## Question

Webhook 事件外发的投递可靠性升级（effort #5 fog 毕业）：现 `WebhookEventForwarder`（T89）是进程内 at-least-once——重启丢在途事件、队列有界满即丢弃。生产级事件管道（LangSmith 持久管道 / LangGraph durable 执行语义）要求跨重启不丢。需要决策：持久化 outbox 的存储载体（SessionStateStore 新键空间 vs 新 SPI vs 独立表）、入队时机（emit 即入队 vs 仅 webhook 启用时）、投递器恢复语义（启动重放 + 退避重试 + 毒丸隔离）、去重口径（幂等键已带头，消费端去重文档化 vs 端上 exactly-once 声明降级为 at-least-once + 幂等键契约）、outbox 清理（成功即删 vs 保留窗口）、有界性（outbox 上限与溢出策略）。产出 spec 24 + impl 切片。

## Resolution

AFK 自决（授权同 effort #5，可推翻）：

1. **存储载体：复用 `SessionStateStore` 新键空间 `webhook.outbox.*`**——不新增 SPI、不动表结构（三实现 JDBC/Redis/内存自动获得持久化），键值放 JSON 序列化的待投递记录（eventId/type/payloadJson/attempts/nextAttemptAt/createdAt）。理由：webhook 记录本质是「带外持久状态」，与 state 同生命周期；独立表会让 store 契约测试矩阵翻倍，净收益低。
2. **入队时机：仅 webhook 启用时**（`buzhou.webhook.url` 非空才装配 outbox 环节）——转发器未启用时零开销零写放大；入队与转发解耦：emit → outbox append（同步、失败 WARN 不阻断主流程）→ dispatcher 异步投递。
3. **投递器恢复语义**：启动即扫 outbox 全键重放（at-least-once）；每条记录退避重试沿用 T89 口径（5xx/IO 退避、4xx 不重试）；**毒丸隔离**：单条 `attempts` 达上限（默认 8，可配 `buzhou.webhook.max-attempts`）→ 状态转 dead、发 `webhook.dead-letter` 事件 + WARN 计数、不再重试；成功即删（不留窗口——幂等键头已让消费端可去重，端上保留窗口无净收益）。
4. **去重口径：诚实声明 at-least-once + 幔等键契约**——文档明确「不承诺 exactly-once；消费端以 `Buzhou-Event-Id` 头幂等去重」；不端上做 exactly-once（需消费端 ACK 协议，越权）。
5. **有界性**：outbox 容量上限（默认 10_000 条，可配 `buzhou.webhook.outbox-capacity`）；满时新事件拒入队 + `webhook.outbox.dropped` 计数 + WARN（背压显式化，不静默）。
6. **多实例**：dispatcher 投递前按 eventId 做 store 层 claim（CAS 写 `claim.<eventId>`=实例Id+TTL）避免双发——但**不承诺强不重投**（claim TTL 过期可重入），文档归入 runbook §6 多实例边界。

### 闭合细化（实现期定稿）

- **死信不回注 SessionEvent**：webhook 死信事件再次外发会自触发循环（死信的外发失败又生成死信）；以 `deadLetters()` 查询 API + `buzhou.webhook.dead-letter` 指标 + ERROR 日志替代（spec 24 已记）。
- **多实例不做 claim**：共享 store 双实例可能双投递——at-least-once + 幂等键契约内可接受，消费端去重是契约责任（runbook §6 增补）。
- **重试节奏改为记录级持久退避**：分发循环内不再阻塞睡眠，退避状态（attempts/nextAttemptAt）落 store，重启后自然续跑。
