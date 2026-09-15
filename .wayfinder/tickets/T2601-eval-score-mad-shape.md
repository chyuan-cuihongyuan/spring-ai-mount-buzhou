---
id: T2601
title: 评测分数 MAD 鲁棒离散度的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

评测分数离散度读面的形状怎么裁决？（spec 1700 / effort #1700 / R1）

## Resolution

**静态纯函数 `EvalScoreMad`（core/eval）**，EvalPassRateTrend 同族房规：
`analyze(scores[, maxZ])` → 嵌套 record `MadReport(count/median/mad/scores/outliers/dispersion)`。
三档闭集 `Dispersion`：INSUFFICIENT（n<3，mad=−1 哨兵）/ TIGHT（MAD=0——
偏离中位点直接判离群）/ SPREAD（修正 z=0.6745·|x−med|/MAD > 阈值判离群，
默认 3.5 Iglewicz–Hoaglin、常数与阈值公共可复用）。借鉴 Prometheus/Thanos
MAD 异常检测。纯读面零状态、不可变报告、默认零行为变化。
