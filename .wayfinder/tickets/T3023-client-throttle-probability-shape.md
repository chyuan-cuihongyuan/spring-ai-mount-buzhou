---
id: T3023
title: 客户端自适应节流的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

按历史接受比的客户端拒发概率怎么算？（spec 1911 / effort #1911 / R112）

## Resolution`

**Google SRE 自适应节流公式纯计算 `ClientThrottleProbability`
（core/ratelimit）**：rejectProbability（max(0,(req−K×acc)/(req+1))
——健康期恒 0 过载期爬升）+ shouldDrop（dice 掷骰确定性回放）。
计数非负/K≥1 fail-fast。落轮 grep 复核无占坑。
