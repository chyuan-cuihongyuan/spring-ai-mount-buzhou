---
id: T6167
title: S 会话 S34 Content-Defined Chunking 内容定义分块的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

字节流切块怎么边界只由内容决定（插入不移位全量）？
（spec 5033 / effort #5033 / S34）

## Resolution

**ContentDefinedChunking（core/fs）**：restic/rclone FastCDC
思想——Gear 滚动哈希低位掩码命中即切，min 内不切/max 硬
上限/归一化双掩码（S→L 切点偏大）；SplitMix64 种子化 Gear
表（跨进程同表确定性）；块覆盖连续；畸形 fail-fast。
