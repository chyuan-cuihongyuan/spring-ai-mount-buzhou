---
id: T1460
title: 维护窗历史读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1459
created: 2026-09-14
---

## Question

J 会话第 5 轮：维护窗历史读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（MaintenanceGateHistoryTest，AssertJ 同仓风格）：begin→end 产一条闭窗史（reason/beganAt≤endedAt/refusals=0）；窗内 noteRefused×3 → 闭窗条目 refusals==3；窗外 noteRefused 无害无痕；history 新→旧 + 有界（灌 20 窗留 16）；快照不可变；end 未 begin 幂等无痕；begin 无 reason 仍拒（既有语义回归）。定向 `mvn -pl buzhou-core test -Dtest='MaintenanceGateHistoryTest,MaintenanceGateTest,MaintenanceGateHookTest'` 绿。
