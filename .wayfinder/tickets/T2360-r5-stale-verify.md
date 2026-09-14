---
id: T2360
title: R5 响应缓存 stale-if-error 的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2359
created: 2026-09-15
---

## Question

N 会话第 5 轮：如何验收？

## Resolution

ResponseCacheStaleIfErrorTest 四断言：宽限内条目保留 + get miss + getStale 命中计数 +
超窗弃；默认关过期即弃零变化；FailingChain 下 advisor 救场返回旧答案不抛；无救场
条目异常照抛（消息原样）。resilience 模块全量绿（367 用例）后单轮 commit。
