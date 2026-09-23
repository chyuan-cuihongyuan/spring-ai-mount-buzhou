---
id: T6011
title: R 会话 R6 周期对账的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

Wave 1 四新类型的快照/档案/台账怎么对齐？（spec 4005 / effort #4005 / R6）

## Resolution

**对账轮收口**：快照补登 1129→1133（CountMin/SpaceSaving/
BoyerMoore/KsTwoSample——metrics×3+eval×1）；api-surface.md 同步
+4 行；CONTEXT 计数 984→1133（历史欠账对齐真值）；全仓 16 模块
离线 verify 三门绿；RSession4000LedgerAuditTest 台账核账 4000–4004
五轮零缺位。
