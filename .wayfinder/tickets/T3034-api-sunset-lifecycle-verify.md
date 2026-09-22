---
id: T3034
title: API 弃用日落生命周期的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3019b]
created: 2026-09-23
---

## Question)

三段判定在边界/剩余/畸形下正确吗？（spec 1916 / effort #1916 / R117）

## Resolution`

**ApiSunsetLifecycleTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=ApiSunsetLifecycleTest）：三段各一例；边界含上（恰弃用即
DEPRECATED/恰日落即 SUNSET）；剩余时间正/钳 0；畸形一型 fail-fast。
