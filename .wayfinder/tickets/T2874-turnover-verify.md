---
id: T2874
title: 库存周转读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2873]
created: 2026-09-16
---

## Question]

周转/视界在三档×2 与畸形五型下正确吗？（spec 1836 / effort #1836 / R37）

## Resolution

**TurnoverReadoutTest 3 用例全绿**（mvn -pl buzhou-spill test
-Dtest=TurnoverReadoutTest）：周转 3 轮/死库存 0/无库存 -1；视界 250/
取整 253/零速率 -1/零库存 0；负库存/负消费/NaN 速率 fail-fast。

