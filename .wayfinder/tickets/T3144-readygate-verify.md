---
id: T3144
title: 就绪等待门的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3143]
created: 2026-09-17
---

## Question

WaitForReadyGate 合同（四态/预算/排空/重计/畸形）怎么钉住？（spec 2021 / effort #2021 / R22）

## Resolution

**八用例一次全绿**（buzhou-core）：双姿态两态 / 就绪直通姿态无关 /
预算 2 满 third QUEUE_FULL 深度不超 / 零预算禁排 / 三入队一批次排空
drained=3 / 再失就绪重计排空账不重复 / 同值翻转 no-op / 负预算
fail-fast。
