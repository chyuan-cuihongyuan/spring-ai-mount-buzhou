---
id: T952
title: 工具执行 per-tool 耗时聚合读面的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T951
created: 2026-09-13
---

## Question

聚合真累计（count/total/max/failed）？失败路径计 failed？多工具互不串账？未装配零行为回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 1 轮）：① 快/慢工具混合执行——慢工具 stats() count/total/max 非零且 max ≥ 模拟耗时，快工具 max 微小；② 多次调用 count 累计、多工具各自独立键不串账；③ 抛异常工具计 failed ≥1 且 still 累计耗时（错误即反馈路径不改 Turn 语义）；④ 未开启 Holder 时既有 HookedToolCallbackTest 全绿（零变化）；⑤ stats() 快照不可变。`mvn -pl buzhou-core -am test` 全绿。
