---
id: T37
title: memory · sleep-time 后台整理
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

对账/去冗余/重排是热路径开销，如何挪到 turn 后异步而不引入写竞争？事实源：Letta（24,230★：`sleeptime_agent_frequency` 计 turn 触发；与主 agent **共享同一 memory block 实体**（attach_block_async）；整理=memory_replace/insert/rethink；RunStatus.created 后台 Run 绝不阻塞主响应）。

## 待定决策（研究推荐已备）

1. turn 后 hook 投递 `MemoryConsolidationTask` 到专用 executor（**JDK21 虚拟线程 + 每 session 串行化**避免 block 写竞争）——采纳。
2. 整理器动作：`SummaryFactReconciler` 对账、去冗余、P0–P3 重排、archival evidence 归档——**全走双时序台账**（valid_from/until 天然支持）——采纳（与已落地组件严丝合缝）。
3. 触发频率（每 N turn / 空闲判定）、失败退避重试、可开关、全程审计——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §3.4（4–6 天，ROI 高：把对账挪出热路径）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §memory-9**（用户常设授权 2026-08-14 ratify、可推翻）。MemoryConsolidationTask 虚拟线程+每 session 串行；整理全走双时序台账；可开关可退避。
