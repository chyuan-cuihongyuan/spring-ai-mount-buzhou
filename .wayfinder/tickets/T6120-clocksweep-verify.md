---
id: T6120
title: S 会话 S10 Clock-Sweep 缓存驱逐的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6119]
created: 2026-09-24
---

## Question

S10 合同怎么逐一验绿？（spec 5009 / effort #5009 / S10）

## Resolution

**验证通过**：ClockSweepCacheTest 五测全绿——基础驱逐序；
命中率差异显证（高频旧页存活/低频新页先出）；使用计数封顶；
空缓存 evict null + 覆盖键不增帧 + 负容量/null fail-fast；
确定性回放。
