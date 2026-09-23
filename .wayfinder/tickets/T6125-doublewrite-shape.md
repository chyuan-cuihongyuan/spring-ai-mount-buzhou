---
id: T6125
title: S 会话 S13 DoubleWrite 双写缓冲的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

页级写崩溃撕裂怎么有恢复源？（spec 5012 / effort #5012 /
S13）

## Resolution

**DoubleWriteBuffer（core/recovery）**：InnoDB doublewrite
思想——stage 先入共享暂存（同页覆盖最新版胜），缓冲满自动
整体落盘守恒；flush 手动清队；recoverable 崩溃恢复视图。
