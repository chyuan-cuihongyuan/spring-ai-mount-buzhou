---
id: T3043
title: 时钟抖动测量的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

时钟源稳定性的抖动读数怎么测？（spec 1921 / effort #1921 / R122）

## Resolution`

**NTP 时钟 discipline 惯例持态 keeper `ClockJitterMeter`
（core/metrics）**：record（窗口滚动采样偏差，可负）+ jitterMillis
（窗口总体标准差，样本<2 哨兵 -1.0）+ meanOffsetMillis（偏斜分量
读数）。窗口≥2 fail-fast。与 ClockSkewClamp 成对（钳位后果 vs
抖动测量）。落轮 grep 复核无占坑。
