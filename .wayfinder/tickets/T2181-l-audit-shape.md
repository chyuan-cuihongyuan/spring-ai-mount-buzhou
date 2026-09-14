---
id: T2181
title: L 会话阶段对账轮（R40）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 40 轮（里程碑轮）：40 轮工件链一致性的保障形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察实证：R31 起票号 +2 漂移、R39 effort/spec 错标 1439、impl 1083/1084/1085 错位——人工台账已实际漂移三次。

形状裁决：对账测试 LSessionLedgerAuditTest（starter 测试域）——范围自扩展（spec 文件驱动）+四面互证（票对公式/impl ±1 窗容差（spec 1439 缺位平移入档）/README 行+spec 号连续性）；本批同时完成修复：18 张票 -2 重编号、spec 1439→1438、impl 三片置换。

Out of scope：他会对账；票 frontmatter 深度校验。
