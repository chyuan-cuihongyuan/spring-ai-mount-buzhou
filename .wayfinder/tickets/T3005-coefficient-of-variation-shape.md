---
id: T3005
title: 波动系数读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

跨量纲波动可比的无量纲读数怎么算？（spec 1902 / effort #1902 / R103）

## Resolution`

**金融 CV 语义纯计算 `CoefficientOfVariation`（core/metrics）**：
cv（stddev/|mean|，mean=0 fail-fast 不哑算）+ band 三档（<0.15
STABLE/<0.5 MODERATE/其余 VOLATILE 金融惯例带，边界严格小于）。
stddev≥0 fail-fast。落轮 grep 复核无占坑。
