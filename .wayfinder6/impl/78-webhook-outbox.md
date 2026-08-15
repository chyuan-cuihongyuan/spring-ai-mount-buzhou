# impl-78 — Webhook 持久化 Outbox

**What to build:** 配置 webhook 后，事件 emit 即持久入 outbox（SessionStateStore 合成会话
`__buzhou.webhook__`），分发器跨重启补投递未决事件；失败按记录退避（状态落 store），超
max-attempts 进死信可查询；成功即删；容量上限拒入 + 计数。消费端可见行为与 spec 20 兼容
（信封/签名/幂等键头不变），新增「重启不丢」与「死信隔离」两类可验证行为。

**Blocked by:** None — can start immediately（T103 已闭合）

**Status:** done

- [x] `WebhookOutbox`（append/due/delete/update/markDead/容量，seq 续起）+ 记录 JSON 序列化
- [x] `WebhookEventForwarder` 改造：同步入队、到期轮询（批 32）、单试/循环、记录级退避、死信、nudge
- [x] `BuzhouWebhookProperties`：`outboxCapacity`（默认 10_000）、`maxAttempts` 默认 3→8、
      `queueCapacity` 废弃 no-op + 启动 WARN
- [x] auto-config：forwarder bean 增传 `BuzhouStores.stateStore()`
- [x] 测试矩阵（真实 HttpServer + 内存 store）：重启恢复/退避持久化/死信隔离/4xx 即死/容量拒入/
      成功即删/幂等键与签名回归
- [x] runbook §6 多实例双投递口径增补；spec 20 §事件外发标注「队列语义被 spec 24 取代」

## Done

commit：见 git log（impl-78）。验证：`mvn clean test -pl buzhou-core`（webhook 用例全绿）。
