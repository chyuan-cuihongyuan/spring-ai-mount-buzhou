---
id: T3128
title: 启动豁免窗的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3127]
created: 2026-09-17
---

## Question

StartupGraceTracker 合同（豁免/计账/毕业/重锚/独立/畸形）怎么钉住？（spec 2013 / effort #2013 / R14）

## Resolution

**七用例一次全绿**（buzhou-core）：窗内双失败全豁免（9_999 界内）/
恰 10_000 到期计账 / 毕业幂等且毕业后窗内也计账 / 未锚定一律计账 /
重锚重算窗口（新窗内豁免新窗外计账） / 多实例独立（毕业与豁免互不
串扰+activeGraces 面） / 畸形五型 fail-fast。
