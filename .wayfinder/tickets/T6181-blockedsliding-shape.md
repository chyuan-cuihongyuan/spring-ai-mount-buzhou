---
id: T6181
title: S 会话 S41 Blocked Sliding Counter 分块滑窗计数器的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

滑窗计数怎么内存有界且误差界显式？（spec 5040 /
effort #5040 / S41；原拟 DG 指数直方图推演不可自洽换分块面）

## Resolution

**BlockedSlidingCounter（core/metrics）**：分块近似面——
窗口切 m 块每块精确计数整块进出，lowerEstimate=Σ−最旧块、
upperBound=Σ（真值恒在两界，界宽 ≤ 块计数 ≤ B），内存
O(m)；position/blockCount/blockSize 读数；参数 fail-fast。
