---
id: T6225
title: T 会话 T13 Elias-Fano 单调序列编码的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

单调序列怎么近下界压缩且 O(1) 访问？（spec 6013 /
effort #6013 / T13）

## Resolution

**EliasFano（core/message）**：lowerWidth=⌈log₂⌈U/n⌉⌉ 拆
高低位——低位定宽、高位「值+下标」联合位图；get O(1)
（select+切片）；size/lowerWidth/storedBits 读数；null/空/
非单调 fail-fast。
