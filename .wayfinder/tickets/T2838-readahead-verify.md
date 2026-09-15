---
id: T2838
title: 顺序读预读顾问的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2837]
created: 2026-09-16
---

## Question

预读判定在链放大/封顶/尾链/哨兵/畸形五面下正确吗？（spec 1818 / effort #1818 / R19）

## Resolution

**ReadAheadAdvisorTest 4 用例全绿**（mvn -pl buzhou-spill test
-Dtest=ReadAheadAdvisorTest）：链 2/4 指数放大、链 8 封顶 8 倍；跳读零预读
+头断尾在取尾链；COLD（单事件/空/null）哨兵；blockSize<1 与零长读
fail-fast。

