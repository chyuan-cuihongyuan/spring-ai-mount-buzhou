---
id: X10079
title: X 会话 40 PeakDetector prominence 峰检测 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

PeakDetector（core/metrics，静态纯函数面）：findPeaks(x,minProminence)——严格局部极大+等高线 prominence（两侧爬升至更高峰或边界取谷底最小、prominence=峰高−两侧较大谷底）；严格峰口径（平台无峰明示）；null/负阈 fail-fast。
