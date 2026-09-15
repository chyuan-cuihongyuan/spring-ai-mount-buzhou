---
id: T2894
title: 组提交账面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2893]
created: 2026-09-16
---

## Question]

账面在四读数/不划算/哨兵/畸形四面下正确吗？（spec 1846 / effort #1846 / R47）

## Resolution

**GroupCommitAccountingTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=GroupCommitAccountingTest）：10 写 2 刷摊薄 5 倍省 8 刷净省 7000
比率 0.7；2 写 1 刷批刷 5000 → 净省 -3000 诚实；零写哨兵；刷多于写/
有写零刷/负数 fail-fast。

