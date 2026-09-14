---
id: T2178
title: 悬空轮判定口径与样本封顶的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2177
created: 2026-09-14
---

## Question

如何证明悬空判定口径（TOOL 链/仅 TOOL 轮）与样本纪律？

## Resolution

**用户常设授权 AFK（可推翻）**

`DanglingTurnDetectorTest` 六测全绿（`mvn -pl buzhou-core -am test`）：空哨兵；已答轮不悬空；USER 无回复悬空；**TOOL 链后无 ASSISTANT 收尾同悬空**；仅 TOOL/SYSTEM 轮不算；样本封顶 8 升序（20 悬空轮）。
