---
id: T988
title: webhook 投递限速的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T987
created: 2026-09-13
---

## Question

限速真节流（N 秒内投递 ≤ rate×t）？defer 不碰重试状态机？默认关零回归？整批 defer 提前结束不热旋？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 17+2 轮）：① 令牌桶 rate=2 burst=2，5 条 due 记录一批 → 恰 2 条投递、3 条 defer、deferredCount=3、记录仍在 outbox 且 attempts 不变；② 指针时钟推进回填 → 后续批次放行；③ 无 limiter 既有 forwarder 用例零回归；④ 指标事件仅在 defer 时出现。`mvn -pl buzhou-core -am test` 全绿。
