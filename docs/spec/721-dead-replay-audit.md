# 721 — 死信重放审计事件

> 来源：G 会话第 22 轮 = effort #721（借鉴审计完整性惯例——敏感动作必留痕）/ [T993](../../.wayfinder/tickets/T993-dead-replay-audit-shape.md) / [T994](../../.wayfinder/tickets/T994-dead-replay-audit-verify.md) / impl 524。

## 背景

`replayDeadLetters()`（spec 37）是运维敏感动作：把死信批量重新打入投递管线（attempts 清零）——此前只有一条 INFO 日志：动作发生过没有、累计重放多少、频率如何，无任何可观测面。

## 目标

- `replayDeadLetters()` 增强：
  - 指标事件 `buzhou.webhook.dead-replayed`（delta = 本次重放条数；requeued=0 不发——无动作无事件）；
  - 累计审计计数：`replayCount()`（动作次数）/ `replayedCount()`（累计信件数）；
  - 审计日志升级：动作 + 条数 + 重放后剩余死信数（结构化参数）。
- **诚实边界**：进程内审计面（指标 + 计数 + 结构化日志）；写进 Merkle 审计链（spec 404）属 guard 域——webhook 域无链（链接线留位）；actor 归属（谁触发）在无宿主身份通道时不可得（fog）。

## 非目标

不做审计链接线；不做重放动作的确认/二次审批面；不改返回类型（int 兼容）。

## 测试

3 条死信重放 → 指标 + replayCount=1 + replayedCount=3 + 返回 3；零死信 → 零事件零计数；既有用例零回归。

## 兼容性

返回类型/语义不变；纯增量观测。
