# 816 — 记忆分层容量读数

> 来源：H 会话第 17 轮 = effort #816 / [T1133](../../.wayfinder/tickets/T1133-memory-hierarchy-capacity.md) / [T1134](../../.wayfinder/tickets/T1134-memory-hierarchy-capacity-verify.md) / impl 569。
> 借鉴：MemGPT/Letta 分层记忆（≈18K star）。

## Problem

Buzhou 记忆三层（九段摘要=core、事实库=archival、消息台账=recall）各自有 store 但无统一容量面：记忆成本增长时「哪层在涨、哪层快满」要逐 store 手查。

## Solution

`MemoryHierarchyCapacity`（memory，纯函数）：

- **三层数**：core-summary（恒 1 条目——单活跃版本）/archival-facts（活跃条数）/recall-window（消息条数），各带 chars。
- **水位**：可选 capChars（≤0 不设限→ratio null 恒 OK）；OK<80%≤WARN<100%≤FULL。
- **快照解耦**：Snapshot 由调用方自 store 采集（零侵入，同 815 模式）。

## 兼容性

纯新增静态工具；无配置键。

## 诚实边界

字符口径非 token；采集时点由调用方定（非实时）；层序固定 core→archival→recall。
