---
id: T6144
title: S 会话 S22 Sparse Index 稀疏索引的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6143]
created: 2026-09-24
---

## Question

S22 合同怎么逐一验绿？（spec 5021 / effort #5021 / S22）

## Resolution

**验证通过**：SparseIndexTest 五测全绿——中块定位；早于首块
-1；块界精确命中；晚于末块落末块；乱序/空块集 fail-fast；
确定性回放。
