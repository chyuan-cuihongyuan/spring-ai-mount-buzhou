---
id: T6215
title: T 会话 T8 Myers O(ND) Diff 的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

序列差异怎么产出最短可执行脚本？（spec 6007 /
effort #6007 / T8）

## Resolution

**MyersDiff（core/metrics）**：贪心 O(ND)——V 数组对角线
推进 + 轨迹快照回溯，脚本长度 = N+M−2·LCS 最优；Edit 三态
（EQUAL/INSERT/DELETE）脚本，tie-break 定构同输入同脚本；
null 序列 fail-fast。
