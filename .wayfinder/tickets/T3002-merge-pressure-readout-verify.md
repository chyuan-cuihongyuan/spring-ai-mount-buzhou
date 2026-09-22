---
id: T3002
title: 合并压力读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3001]
created: 2026-09-23
---

## Question)

压力三态在边界/硬阀/畸形下正确吗？（spec 1900 / effort #1900 / R101）

## Resolution`

**MergePressureReadoutTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=MergePressureReadoutTest）：三态各一例；边界含上（恰
warnAt/恰 1.0）；硬阀 350<400 不拒、450>400 拒；畸形三型 fail-fast。
