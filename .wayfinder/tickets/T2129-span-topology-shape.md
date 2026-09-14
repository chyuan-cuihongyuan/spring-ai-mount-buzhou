---
id: T2129
title: Span 树拓扑读面（SpanTreeTopology）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 15 轮（换题轮）：Span 结构形状分析面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：原题 R44 与 RollingJsonlWriter（spec 712 rotations/压缩代际）覆盖——换入 R39 core 侧通用化（ToolGraphAnalyzer 在 observability 模块且为时延维）。SpanParentIntegrityAudit 占完整性轴（孤儿明细）——本轴取结构形状。

形状裁决：SpanTreeTopology 纯函数（core/observability）——Topology(totalSpans/rootCount/orphanCount 计数/maxDepth 根=1/maxFanout/kindHistogram 数量降序平名典序)+环防护（visited 收敛环成员不计深不炸栈）+无序容忍+空集合哨兵。

Out of scope：时延归因；孤儿明细；跨会话聚合。
