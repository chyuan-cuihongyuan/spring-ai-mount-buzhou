---
id: T3116
title: 频率素描的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3115]
created: 2026-09-17
---

## Question

FrequencySketch 合同（单调/饱和/独立/下界/畸形）怎么钉住？（spec 2007 / effort #2007 / R8）

## Resolution

**六用例全绿**（首跑 2 红根因：测试期望写成精确 n——Caffeine min 槽
语义同 key 反复 ≈n/2 序不变，修期望域 [n/2, n] 后 6/6）：7 次访问
frequency ∈ [3,7] / 100 次饱和恰 15 不回绕 / 双 key 独立保序（10:3 →
a>b）/ 500 key×5 轮下界 ≥1 不退化 / 准入次序（newcomer 1 < victim 6）/
畸形四型（容量 0、−1、null key 增/查）fail-fast。
