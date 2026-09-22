---
id: T2999
title: 乱序接纳窗的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

迟到数据的接纳/拒收边界怎么定？（spec 1899 / effort #1899 / R100）

## Resolution`

**QuestDB out-of-order 窗口纯计算 `OutOfOrderWindow`
（core/metrics）**：classify 三态（FRESH 顺序/LATE_ACCEPTED 窗内
迟到就地接纳/TOO_OLD 窗前拒收，窗沿含下）+ advance 水位取大单调。
window≥0/时点≥0 fail-fast。落轮 grep 复核无占坑。
