---
id: T6211
title: T 会话 T6 周期对账的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

Wave 1 四新类型怎么入快照封账？（spec 6005 /
effort #6005 / T6）

## Resolution

**快照补登**：regenerateSnapshot 全 reactor 再生
（1209→1213：SplayTree/Treap——concurrent +
SparseTable/MonotonicDeque——metrics）+ api-surface.md
同步 +4 行 + CONTEXT 计数 +4 + 全仓 verify 三门 + 台账
核账。
