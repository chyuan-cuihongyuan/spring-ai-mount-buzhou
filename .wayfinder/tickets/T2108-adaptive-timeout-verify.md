---
id: T2108
title: AdaptiveTimeout EWMA 数学与夹取边界的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2107
created: 2026-09-14
---

## Question

如何证明 EWMA 加权、预热哨兵、夹取与恢复行为正确？

## Resolution

**用户常设授权 AFK（可推翻）**

`AdaptiveTimeoutTest` 七测全绿（`mvn -pl buzhou-resilience -am test`）：预热期 empty→3 样本出推荐（100ms×3）；EWMA 遗忘链精确断言（3700→5590 递升，新样本权重可见）；floor/ceiling 双向夹取（1ms×5→500ms 下限；10s×5→2000ms 上限）；恢复期逐步回落（212.5×2=425——历史权重显形非直落 200）；reset 归零（ewma=-1 哨兵）；参数/样本非法 fail-fast；stats() 无副作用（推荐计数不被快照推进）。评审修正两处：stats() 原内调 recommended() 有计数副作用——改独立内联计算；测试期望两处数学修正。
