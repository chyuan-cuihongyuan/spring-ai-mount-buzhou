---
id: T6216
title: T 会话 T8 Myers O(ND) Diff 的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6215]
created: 2026-09-26
---

## Question

T8 合同怎么逐一验绿？（spec 6007 / effort #6007 / T8）

## Resolution

**验证通过**：MyersDiffTest 五测全绿——脚本应用于 a 恒得
b 且 EQUAL 双侧一致；200 对随机小序列长度 = N+M−2·LCS
（DP oracle）；空侧/全等边界；fail-fast。
