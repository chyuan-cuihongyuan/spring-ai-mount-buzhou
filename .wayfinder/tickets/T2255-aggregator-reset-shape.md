---
id: T2255
title: 计时聚合器双子实例清零面的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 3 轮：HookTimingAggregator / ToolTimingAggregator 的清零生命周期面？

## Resolution

**用户常设授权 AFK（可推翻）**

现状实证：两聚合器 Holder.reset() 只把 current 置 null（关闭聚合），聚合器实例的 timings 映射无任何清零面——enable 后 stats()/windowedMax() 只增不减：测试间基线污染（同 JVM 多测试类共享 Holder 时无法重置断言基线）、长生命周期进程无法重建基线。

形状：两聚合器各补公开 `reset()`（清空 timings 映射，幂等，未装配零副作用——作用于实例不碰 Holder）；Holder 不动（reset 置 null 语义既有）。命名贴 Prometheus counter reset（生产可用的基线重建，非 test-only），Javadoc 注明测试隔离与运维基线重建双用途。思想源：Prometheus counter reset 语义 + 仓库规范「进程级静态读面须配 reset 注入点」的符合性补全；先例 ToolInFlight.reset()。
