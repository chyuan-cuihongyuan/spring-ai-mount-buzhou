# Spec 24 — Webhook 持久化 Outbox（投递可靠性）

> effort #6（T103 / impl-78）。延续 spec 20 §事件外发；借鉴 LangSmith 持久事件管道、
> LangGraph durable 执行语义、Transactional Outbox 模式（企业集成模式）。

## Problem Statement

事件外发 webhook（spec 20 / T89）目前是**进程内 at-least-once**：待投递事件只活在内存队列里。
进程重启即丢失在途事件；队列满即静默丢弃。对以 webhook 驱动计费、审计、告警的外部系统而言，
「重启窗口内事件无声消失」不可接受——生产级事件管道必须跨重启不丢。

## Solution

把投递前的暂存从内存队列升级为**持久化 outbox**：事件 emit 时先同步落入 SessionStateStore
（合成会话键空间），后台单线程分发器从 outbox 取「到期」记录投递，成功即删、失败按记录持久化
退避重试、超上限进死信隔离。重启后分发器自动从 store 恢复未决记录继续投递。投递语义仍是
**at-least-once + 幂等键契约**（消费端以 `X-Buzhou-Event-Id` 去重），不承诺 exactly-once。

## User Stories

1. As a 平台运维, I want webhook 事件跨重启不丢, so that 计费/审计下游不缺账。
2. As a webhook 消费方, I want 每请求带稳定幂等键头, so that 重复投递可安全去重。
3. As a 平台运维, I want 反复失败的事件进死信而非无限重试, so that 故障下游不拖垮投递管道。
4. As a 平台运维, I want 死信可查询（eventId/类型/尝试次数）, so that 排障时可定位具体丢失面。
5. As a 应用开发者, I want 不配 webhook url 时零开销零写放大, so that 未用外发的部署不为它买单。
6. As a 平台运维, I want outbox 容量有上限且溢出显式计数, so that 下游长期不可用时不无声吃满存储。
7. As a SRE, I want 投递/丢弃/失败/死信都有指标, so that 告警清单（runbook §7）可直接引用。
8. As a 多实例部署者, I want 多实例语义有明确文档, so that 我知道共享 store 时的双投递边界与消费端责任。

## Implementation Decisions

- **存储载体**：复用 `SessionStateStore`，合成会话 `__buzhou.webhook__`；键 `outbox.<eventId>`
  （未决/退避中）与 `dead.<eventId>`（死信）。value = JSON 记录（eventId、type、body、seq、
  attempts、nextAttemptAt、createdAt）。**不新增 SPI、不动三实现表结构**——JDBC/Redis/内存
  store 自动获得对应持久化等级（内存 store = 与旧行为等价的进程内暂存）。
- **入队时机**：仅 `buzhou.webhook.url` 非空才装配 forwarder（不变）；`onEvent` 构造信封后
  **同步 append** 进 outbox（失败 WARN 不阻断会话主链），成功后 nudge 分发器。序列化失败的
  单事件仍静默丢弃（不可投递，口径同 spec 20）。
- **分发器**：单虚拟线程轮询「到期记录」（`nextAttemptAt <= now`，按 seq 升序，批上限 32）；
  每轮循环对每条记录**只试一次**（不在循环内阻塞睡眠退避——退避状态持久化在记录里，
  由下一轮自然拾起）。4xx 即判死（配置/消费端错误，口径同 spec 20）；IOException/5xx 记
  attempts++ 并按 `min(1s × 2^attempts, 60s)` 排下次投递。
- **死信**：单条 attempts 达 `buzhou.webhook.max-attempts`（默认 8，含首试）→ 键迁移
  `outbox.<id>` → `dead.<id>`（容量随之释放），指标 `buzhou.webhook.dead-letter` + ERROR 日志；
  死信**不再自动重试**，经 `forwarder.deadLetters()` 可查询（返回eventId/type/attempts/
  createdAt，上限 100）。不发 SessionEvent 回注（webhook 死信事件再次外发会自触发循环）。
- **容量**：未决记录数达 `buzhou.webhook.outbox-capacity`（默认 10_000）→ 新事件拒入队 +
  `buzhou.webhook.dropped` 计数 + WARN（沿用丢弃计数口径，背压显式化）。
- **排序公平性**：seq = 进程内 AtomicLong（启动时从存量最大 seq 续起）；多实例共享 store 时
  seq 可能交错，仅影响投递顺序不影响正确性（at-least-once 契约内）。
- **多实例**：不做投递 claim/锁——共享 store 多实例可能双投递，消费端幂等键去重是契约责任
  （runbook §6 增补口径）。
- **优雅关闭**：`close()` 停止接新事件后限时 5s 排空「已到期」记录；未到期退避记录留存 store，
  由下次启动恢复（持久化语义本意）。
- **配置**：`BuzhouWebhookProperties` 增 `outboxCapacity`（默认 10_000）；`maxAttempts` 语义
  微调为「单条记录总尝试上限」（默认 3 → 8）；`queueCapacity` 废弃保留字段（no-op，启动 WARN
  提示迁移）。
- **模块**：仅 `buzhou-core`（webhook 包内新增 `WebhookOutbox` + 记录类型）；auto-config
  forwarder bean 增传 `BuzhouStores.stateStore()`。

## Testing Decisions

- 好测试只测外部行为：**不 mock 内部**，用真实 forwarder + 内存 SessionStateStore + 本地
  `com.sun.net.httpserver` HttpServer（先例：`WebhookEventForwarderTest`）。
- 用例矩阵：①重启恢复（新建 forwarder 实例模拟重启，未决事件补投递）；②退避持久化
  （失败后 attempts/nextAttemptAt 落 store）；③死信隔离（超上限进 dead、不再重试、可查询）；
  ④4xx 立即死；⑤容量上限拒入 + dropped 计数；⑥成功即删（store 中无残留）；⑦幂等键头与
  HMAC 签名回归（spec 20 行为零变化）。
- store 契约测试矩阵不动（未加 SPI）；`examples` 不新增演示（T103 属基础设施纵深）。

## Out of Scope

- exactly-once 投递（需消费端 ACK 协议，越权——契约明确 at-least-once + 幂等键）。
- 多实例投递 claim/分布式锁（runbook §6 文档化边界）。
- 死信自动重放/管理界面（`deadLetters()` 查询 API 即可，重放由运维按需自建）。
- webhook 以外的事件管道（消息队列 sink 等）。

## Further Notes

- 合成会话 `__buzhou.webhook__` 的 state 键不入任何会话生命周期清理；T108（fsck）需将合成
  会话列入白名单（已在票面记依赖）。
- 与 T89 的关系：spec 20 §事件外发的「有界队列满丢弃」语义被本 spec 取代；信封格式、签名、
  HTTP 细节全部沿用。
- **性能注记**：分发器到期扫描为 store 侧 `getAll(合成会话)` 全量读（无 store 端条件查询
  接口）；空/低水位时单次查询返回极小；下游故障积压万条级时每次批扫全量读——属故障窗口内
  的可接受代价（重试节流由记录级退避承担），如未来成为瓶颈，演进方向是 state store 增
  按前缀扫描接口（fog）。
