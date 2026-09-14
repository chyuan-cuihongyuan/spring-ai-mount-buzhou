---
id: T2176
title: 退化条目分桶与长度分位的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2175
created: 2026-09-14
---

## Question

如何证明退化分桶、短输入阈值与分位计算？

## Resolution

**用户常设授权 AFK（可推翻）**

`DatasetQualityAuditTest` 五测全绿（`mvn -pl buzhou-core -am test`）：空集 -1 哨兵；空 input/expected 分桶+退化比 0.75 精确；短输入阈值 6<8 计 1；长度 P50=15/P95=20 秩插值；同条目 input/expected 双空两桶同计退化比 2.0（派生口径极端自证）。
