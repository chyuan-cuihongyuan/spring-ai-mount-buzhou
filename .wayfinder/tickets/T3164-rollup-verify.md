---
id: T3164
title: 必选检查聚合的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3163]
created: 2026-09-17
---

## Question

RequiredChecksRollup 合同（否决优先/挂起/可选显形/畸形）怎么钉住？（spec 2031 / effort #2031 / R32）

## Resolution

**八用例一次全绿**（buzhou-core）：全必选绿 SUCCESS / 未报必选
PENDING+计数 1 / 显式 PENDING 挂起 / FAILURE 一票否决（混合 SUCCESS
+PENDING+FAILURE） / 可选失败不阻断但 optionalFailures=1 / 空集恒
SUCCESS / 快照注册序稳定 / 畸形六型（null/空名、重复注册、报未注册、
null state）fail-fast。
