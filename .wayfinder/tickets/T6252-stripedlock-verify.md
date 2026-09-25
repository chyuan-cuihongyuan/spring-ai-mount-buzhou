---
id: T6252
title: T 会话 T26 Striped Lock 条带锁的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6251]
created: 2026-09-26
---

## Question

T26 合同怎么逐一验绿？（spec 6025 / effort #6025 / T26）

## Resolution

**验证通过**：StripedLockTest 五测全绿——同键恒同锁；4×
5000 同键互斥精确；不同键并行重叠观测；2 的幂取整；
fail-fast。
