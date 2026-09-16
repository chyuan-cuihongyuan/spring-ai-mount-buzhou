---
id: T3111
title: P 会话 R6 对账轮的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

Wave 1 四新类型 + O 系四未补登类型的快照门怎么收口？（spec 2005 / effort #2005 / R6）

## Resolution

**R6k 对账轮四件套**：regenerateSnapshot（reactor -am 形态）快照 998→1006
（+8：P 系 HLL/指数直方图/记忆强度/φ 检测器 + O 系代补 BurstCredit/
ScheduleFloat/RegexRiskAudit 等）+ api-surface.md 八行 spec 注释同步 +
CONTEXT 897→905 + 全仓 mvn verify 三门全绿 + P 对账门四面核账 + push
重试。O 系代补遵循「对账轮扫荡并行残留」先例。
