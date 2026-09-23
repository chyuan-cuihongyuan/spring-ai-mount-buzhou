---
id: T6051
title: R 会话 R26 重启强度的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

子进程重启风暴怎么累积判定并升级？（spec 4025 / effort #4025 / R26）

## Resolution

**SupervisorRestartIntensity（core/runaway）**：OTP max_intensity——
滑窗内 >maxRestarts 即闩锁升级（终止全部子进程停监督者语义），
reset 开新纪元；时钟可注入 + 滑窗读数可审计。与 TurnStallWatchdog
（单轮失速）互补：本件管重启频率累积。
