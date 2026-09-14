---
id: T2384
title: R17 工具失败负缓存的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2383
created: 2026-09-15
---

## Question

N 会话第 17 轮：如何验收？

## Resolution

NegativeCachingToolCallbackTest 四断言：失败缓存窗内拦截（三次复读真调 1 次 +
negativeHits=2）；TTL 过期放行（故障恢复经恢复窗口）；异常路径同缓存（二次
不抛返回缓存文本）；不同参数独立 key。文案带结构化标记前缀钉住判定语义。
