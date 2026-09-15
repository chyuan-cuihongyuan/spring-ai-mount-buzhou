---
id: T2909
title: 双窗口漂移检测的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

指标漂移的「重要且显著」怎么双闸判定？（spec 1854 / effort #1854 / R55）

## Resolution`

**Netflix/SRE 双窗口异常检测惯例纯判漂 `WindowShiftDetector`
（core/metrics）**：detect(baseline, recent, 绝对闸, 相对闸) → STABLE/
SHIFTED_UP（变差）/SHIFTED_DOWN（变好）；双闸同过才漂（|均值差| ≥ 绝对闸
且 ≥ 相对闸×max(|基线|,1)——零基线退化绝对口径）。单闸二难：只相对被
小基数噪声刷屏、只绝对漏报缓变。纯判漂不归因。

